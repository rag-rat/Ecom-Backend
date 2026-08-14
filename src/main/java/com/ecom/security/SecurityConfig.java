package com.ecom.security;


import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;


    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))


                /// //// Access Rules

                .authorizeHttpRequests(auth -> auth


                                /// //// FULLY PUBLIC ENDPOINTS  (No token required. Anyone can access these)

                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                                .requestMatchers("/api/payments/webhook").permitAll()
                                .requestMatchers("/ws/**").permitAll()
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                ).permitAll()
                                .requestMatchers("/actuator/health").permitAll()
                                .requestMatchers("/oauth2/**").permitAll()
                                .requestMatchers("/login/oauth2/**").permitAll()


                                /// ///////  ADMIN-ONLY ENDPOINTS

                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
//                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasRole("ADMIN")
                                .requestMatchers("/api/inventory/**").hasRole("ADMIN")


                                /// ////   CUSTOMER-ONLY ENDPOINTS

                                .requestMatchers("/api/cart/**").hasAnyRole("CUSTOMER", "ADMIN")
                                .requestMatchers("/api/wishlist/**").hasAnyRole("CUSTOMER", "ADMIN")
                                .requestMatchers("/api/addresses/**").hasAnyRole("CUSTOMER", "ADMIN")
                                .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                                .requestMatchers("/api/payments/**").hasAnyRole("CUSTOMER", "ADMIN")


                                /// //// Review write operations — authenticated users only

                                .requestMatchers(HttpMethod.POST, "/api/reviews/**")
                                .hasAnyRole("CUSTOMER", "ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/reviews/**")
                                .hasAnyRole("CUSTOMER", "ADMIN")


                                /// /////  ALL OTHER REQUESTS

                                .anyRequest().authenticated()

                )


                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    """
                                            {
                                                "success": false,
                                                "message": "Authentication required. Please login.",
                                                "timestamp": "%s"
                                            }
                                            """.formatted(java.time.LocalDateTime.now())
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    """
                                            {
                                                "success": false,
                                                "message": "Access denied. You do not have permission.",
                                                "timestamp": "%s"
                                            }
                                            """.formatted(java.time.LocalDateTime.now())
                            );
                        })

                )


                /// ///// Authentication Provider

                .authenticationProvider(authenticationProvider())


                /// /// Register Our JWT Filter

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )


                /*
                 * OAuth2 Login configuration
                 * ─────────────────────────────
                 * authorizationEndpoint: where Spring starts the OAuth2 flow.
                 *   Customers navigate to:
                 *     /oauth2/authorize/google  → triggers Google OAuth2 flow
                 *     /oauth2/authorize/github  → triggers GitHub OAuth2 flow
                 *   These URLs are generated automatically by Spring Security based
                 *   on the registration names in application.yml. The frontend just
                 *   needs a link: <a href="/oauth2/authorize/google">Login with Google</a>
                 *
                 * redirectionEndpoint: where Google/GitHub redirects BACK after auth.
                 *   Must match EXACTLY what you configured in Google Console and GitHub
                 *   OAuth app settings: http://localhost:8080/oauth2/callback/google
                 *
                 * userInfoEndpoint: which service processes the user's profile.
                 *   Our CustomOAuth2UserService handles find-or-create logic.
                 *
                 * successHandler/failureHandler: what happens after processing.
                 *   Success → generate JWT, redirect to React with tokens in URL.
                 *   Failure → redirect to React with error message.
                 */


                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint ->
                                endpoint.baseUri("/oauth2/authorize"))
                        .redirectionEndpoint(endpoint ->
                                endpoint.baseUri("/oauth2/callback/*"))
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                );


        return http.build();


    }


    /// /////// BEAN 2: PasswordEncoder — BCrypt Password Hashing

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }


    /// //////////  BEAN 3: AuthenticationProvider — Wiring UserDetails + PasswordEncoder

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }


    /// ////// BEAN 4: AuthenticationManager

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    /// /////////  BEAN 5: CorsConfigurationSource — Cross-Origin Resource Sharing

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
