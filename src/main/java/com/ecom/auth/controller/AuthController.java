package com.ecom.auth.controller;

import com.ecom.auth.dto.*;
import com.ecom.auth.service.AuthService;
import com.ecom.common.dto.ApiResponse;
import com.ecom.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Register, loging , logout and token refresh")
public class AuthController {

    private final AuthService authService;


    // Register

    @PostMapping("/register")
    @Operation(
            summary = "Register a new customer account",
            description = "Creates a new CUSTOMER account. ADMIN accounts cannot be create via this endpoint."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request ) {

        log.info("POST /api/auth/register - email: {}", request.getEmail());

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Welcome!", response));
    }



    // Login

    @PostMapping("/login")
    @Operation(
            summary = "Login with email and password",
            description = "Returns access token (15 min) and refresh token (7 days). " + "Store access token in memory, not localStorage."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("POST /api/auth/login - email: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response)
        );
    }



    // Refresh Token

    @PostMapping("/refresh")
    @Operation(
            summary = "Get a new access token using a refresh token",
            description = "The old refresh token is deleted and a new one is issued (rotation). " + "If refresh token is expired or invalid, user must login again."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Post /api/auth/refresh");

        AuthResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", response)
        );
    }



    // Logout

    @PostMapping("/logout")
    @Operation(
            summary = "Logout - invalidate access and refresh tokens",
            description = "Blacklists the access token in Redis and deletes the refresh token from MongoDB. " + "Requires Authorization header with valid access token"
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) LogoutRequest request){

        log.info("POST /api/auth/logout - user: {}", userDetails.getUsername());

        String accessToken = null;


        // Extract just the token part (remove "Bearer " prefix)

        if(StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer")) {
            accessToken = authHeader.substring(7);
        }


        // Default to empty request if body was not sent

        if (request == null) {
            request = new LogoutRequest();
        }

        authService.logout(accessToken, request);


        return ResponseEntity.ok(
                ApiResponse.success("Logged out successfully")
        );
    }



    // Logout from All Devices

    @PostMapping("/logout-all")
    @Operation(
            summary = "Logout from all devices",
            description = "Deletes all refresh tokens for this user. " + "Other sessions access token remain valid for up to 15 minutes. "
    )
    public ResponseEntity<ApiResponse<Void>> logoutFromAllDevices(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        log.info("POST /api/auth/logout-all = user: {}", userDetails.getUsername());

        String accessToken = null;

        if(StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        authService.logoutFromAllDevices(userDetails.getUsername(), accessToken);

        return ResponseEntity.ok(ApiResponse.success("Logged Out from all devices successfully "));
    }



    // Verify Token

    @GetMapping("/verify")
    @Operation(
            summary = "Verify if access token is still valid",
            description = "Returns 200 with user info if valid, 401 if expired or blacklisted "
    )
    public ResponseEntity<ApiResponse<AuthResponse>> verifyToken(
            @AuthenticationPrincipal CustomUserDetails userDetails
//            HttpServletRequest request
    ){


//        System.out.println("Dummy string to test verify sout");
//
//        System.out.println(request.getHeader("Authorization"));
//
//        System.out.println(userDetails);
//
//        if (userDetails == null) {
//            return ResponseEntity.status(401).build();
//        }


        return ResponseEntity.ok(
                ApiResponse.success("Token is valid",
                        AuthResponse.builder()
                                .userId(userDetails.getUser().getId())
                                .firstName(userDetails.getUser().getFirstName())
                                .lastName(userDetails.getUser().getLastName())
                                .email(userDetails.getUser().getEmail())
                                .role(userDetails.getUser().getRole().name())
                                .tokenType("Bearer")
                                .build()
                )
        );
    }

}
