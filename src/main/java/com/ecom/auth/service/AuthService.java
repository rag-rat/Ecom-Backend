package com.ecom.auth.service;


import com.ecom.auth.dto.*;
import com.ecom.auth.entity.RefreshToken;
import com.ecom.auth.repository.RefreshTokenRepository;
import com.ecom.common.enums.RoleType;
import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.redis.TokenBlacklistService;
import com.ecom.security.CustomUserDetails;
import com.ecom.security.JwtService;
import com.ecom.user.entity.User;
import com.ecom.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;


    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpirationMs;



    // register()

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());


        // Check Email Uniqueness

        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BusinessException(
                    "An account with this email already exists. Please login instead.",
                    HttpStatus.CONFLICT
            );
        }



        // Build and save the user document

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(RoleType.CUSTOMER) // Always CUSTOMER — never trust client for role
                .isActive(true)
                .build();


        User savedUser = userRepository.save(user);
        log.info("New user registered: id={}, email={}", savedUser.getId(), savedUser.getEmail());


        // Generate tokens

        return generateAndSaveTokens(savedUser, null);
    }



    // Login()

    @Transactional
    public AuthResponse login(LoginRequest request) {


        // Load user by Email first

        User user = userRepository
                .findByEmailAndIsActiveTrue(request.getEmail().toLowerCase().trim()).orElseThrow(() -> {
                    log.warn("Login failed - email not found or account inactive: {}", request.getEmail());

                    return new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
                });



        // Authenticate via Spring Security's AuthenticationManager

//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        user.getId(),
//                        request.getPassword()
//                )
//        );


        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getId(),
                        request.getPassword()
                )
        );

        log.info("Login successful for user : {}", user.getId());

        // Generate tokens and return response

        return generateAndSaveTokens(user, request.getDeviceInfo());

    }



    // refreshToken()

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request){
        log.info("Token refresh requested");


        // Find the refresh token in MongoDB

        RefreshToken storedToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException(
                        "Invalid or expired refresh token. Please login again.",
                        HttpStatus.UNAUTHORIZED
                ));



        // Verify it's not expired

        if(storedToken.getExpiresAt().isBefore(LocalDateTime.now())){
            refreshTokenRepository.deleteByToken(request.getRefreshToken());

            throw new BusinessException(
                    "Refresh token has expired. Please login again.",
                    HttpStatus.UNAUTHORIZED
            );
        }



        // Load the user

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found for refresh token" ));



        // Check user is still active

        if (!user.isActive()){
            throw new BusinessException(
                    "Your account has been disabled.",
                    HttpStatus.UNAUTHORIZED
            );
        }



        // Delete old refresh token (rotation)

        refreshTokenRepository.deleteByToken(request.getRefreshToken());
        log.info("Old refresh token deleted for user: {}", user.getId());


        // Generate new tokens

        AuthResponse response = generateAndSaveTokens(user, storedToken.getDeviceInfo());
        log.info("Token refreshed successfully for user: {}", user.getId());


        return response;
    }



    // Logout()

    @Transactional
    public void logout(String accessToken, LogoutRequest request){
        log.info("Logout requested");


        // Step 1: Blacklist the access token JTI in Redis

        if(StringUtils.hasText(accessToken)) {
            try{
                String jti = jwtService.extractJti(accessToken);
                long remainingMs = jwtService.getRemainingValidityMs(accessToken);

                if (remainingMs > 0) {
                    tokenBlacklistService.blacklistToken(jti, remainingMs);
                    log.info("Access token blacklisted. JTI: {}, TTL: {}ms", jti, remainingMs);
                }

            } catch(Exception e) {
                log.warn ("Could not blacklist access token: {}", e.getMessage());
            }
        }


        // Step 2: Delete refresh token from MongoDB

        if (StringUtils.hasText(request.getRefreshToken())) {
            refreshTokenRepository.deleteByToken(request.getRefreshToken());
            log.info("Refresh token deleted on logout");
        }

        log.info("Logout completed successfully");

    }



    // logoutFromAllDevices()

    @Transactional
    public void logoutFromAllDevices(String userId, String accessToken){
        log.info("Loggin out user {} from all devices", userId);

        refreshTokenRepository.deleteAllByUserId(userId);

        if(StringUtils.hasText(accessToken)) {
            try {
                String jti = jwtService.extractJti(accessToken);
                long remainingMs = jwtService.getRemainingValidityMs(accessToken);
                if (remainingMs > 0) {
                    tokenBlacklistService.blacklistToken(jti, remainingMs);
                }
            } catch (Exception e) {
                log.warn("Could not blacklist access token during all-device logout: {}",
                        e.getMessage());
            }
        }

        log.info("All sessions terminated for user: {}", userId);
    }



    // PRIVATE HELPERS

    private AuthResponse generateAndSaveTokens(User user, String deviceInfo) {

        CustomUserDetails userDetails = new CustomUserDetails(user);


        // Generate access token

        String accessToken = jwtService.generateAccessToken(
                userDetails,
                user.getId(),
                user.getRole().name()
        );


        // Generate refresh token

        String refreshTokenStr = jwtService.generateRefreshToken(user.getId());


        // Save refresh token to MongoDB.

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(user.getId())
                .deviceInfo(deviceInfo)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(
                        refreshTokenExpirationMs / 1000  // convert ms to seconds
                ))
                .build();


        refreshTokenRepository.save(refreshTokenEntity);
        log.debug("Refresh token saved for user: {}", user.getId());


        // Build and return the response DTO

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .accessTokenExpiresIn(accessTokenExpirationMs / 1000) // seconds for frontend
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

    }

}
