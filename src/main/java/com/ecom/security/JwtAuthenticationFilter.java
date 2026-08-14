package com.ecom.security;

import com.ecom.redis.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;



@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;



    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {


        final String token = extractTokenFromRequest(request);


        /// /////////  if no jwt token

        if (token == null) {
            log.debug("No JWT token found in request to: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }


        /////////////  Extract the User ID from the token...

        final String userId;

        try {
            userId = jwtService.extractUserId(token);
        } catch (Exception e) {
            log.warn("Could not extract user ID from token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }


        ///////  if the SecurityContext already has authentication? (request forwarding)

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            String jti = jwtService.extractJti(token);
            if (tokenBlacklistService.isBlacklisted(jti)) {
                log.warn("Rejected blaclisted token. JTI: {}, User: {}", jti, userId);
                filterChain.doFilter(request, response);
                return;
            }
        }


        /// ////////  Load the user from MongoDB

        UserDetails userDetails = userDetailsService.loadUserByUsername(userId);


        //////////// Validate the complete token

        if (jwtService.validateToken(token, userDetails)) {

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );


            /// ////// set the Security Context

            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("Successfully authenticated user: {} for URI: {}", userId, request.getRequestURI());

        } else {
            log.warn("Invalid JWT token for user: {}", userId);
        }


        filterChain.doFilter(request, response);

    }



    /// /////// Extracts the JWT string from the Authorization header

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken)
                && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }



    ///////////  Optimization: Skip this filter entirely for public endpoints.

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

//        return path.startsWith("/api/auth/")
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health")
                || path.equals("/ws")
                || path.startsWith("/ws/");
    }
}
