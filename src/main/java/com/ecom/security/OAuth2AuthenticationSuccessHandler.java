package com.ecom.security;

import com.ecom.auth.entity.RefreshToken;
import com.ecom.auth.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;


/*
 * ═══════════════════════════════════════════════════════════════
 * After CustomOAuth2UserService successfully returns a CustomUserDetails
 * object (the "who is this user?" question is answered), Spring calls
 * THIS handler as the "what do we do NOW?" step.
 *
 * THE CHALLENGE: OAuth2 is a browser-redirect flow, but our app is a
 * STATELESS REST API with JWT tokens — there's no session, no cookies
 * for auth. We need to get the JWT back to the React frontend somehow.
 *
 * SOLUTION: After generating the JWT, we redirect the browser to the
 * React app's callback URL WITH the tokens as query parameters:
 *   http://localhost:3000/oauth2/redirect?token=eyJ...&refreshToken=eyJ...
 *
 * The React app reads these from the URL on the /oauth2/redirect route,
 * stores them in Redux (same as after a normal login), and strips them
 * from the URL (replaceState) so they don't linger visibly.
 *
 * WHY QUERY PARAMS AND NOT COOKIES OR RESPONSE BODY?
 * - Response body: impossible in a redirect flow (we're redirecting the
 *   browser, not returning JSON to an Axios call)
 * - HttpOnly cookies: more secure but complex to set cross-origin;
 *   left as a future hardening step
 * - Query params: simple, works cross-origin, matches how most React
 *   OAuth2 tutorials handle this pattern; tokens are short-lived (15 min)
 *   so the window of exposure is small
 * ═══════════════════════════════════════════════════════════════
 */


@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.oauth2.authorized-redirect-uris}")
    private String authorizedRedirectUri;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();


        // Generate our standard JWT tokens — EXACTLY the same tokens
        // as AuthService.generateAndSaveTokens() in Part 6, ensuring
        // OAuth2 users are indistinguishable from email/password users
        // from the frontend's perspective once they have a token
        String accessToken = jwtService.generateAccessToken(
                userDetails,
                userDetails.getUser().getId(),
                userDetails.getUser().getRole().name()
        );


        String refreshTokenStr = jwtService.generateRefreshToken(
                userDetails.getUser().getId());



        // Persist refresh token in MongoDB (same as Part 6)
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(userDetails.getUser().getId())
                .deviceInfo("OAuth2-" + request.getParameter("registrationId"))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);


        // Build the redirect URL with tokens as query parameters
        String targetUrl = UriComponentsBuilder
                .fromUriString(authorizedRedirectUri)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshTokenStr)
                .build().toUriString();

        log.info("OAuth2 authentication successful for user {}. Redirecting to frontend.",
                userDetails.getUser().getEmail());

        // This redirect sends the browser to the React app
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}