package com.ecom.security;

import com.ecom.common.enums.RoleType;
import com.ecom.user.entity.User;
import com.ecom.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;



@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // Let DefaultOAuth2UserService do the actual HTTP call to the
        // provider's userinfo endpoint — we just process the result
        OAuth2User oAuth2User = super.loadUser(userRequest);


        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        Map<String, Object> attributes = oAuth2User.getAttributes();


        // Extract provider-specific fields — Google and GitHub use
        // different attribute names for the same concepts
        OAuth2UserInfo userInfo = extractUserInfo(registrationId, attributes);


        User user = findOrCreateUser(userInfo, registrationId);

        log.info("OAuth2 login successful: provider={}, email={}", registrationId, userInfo.email());

        // Wrap in CustomUserDetails so the rest of the security chain
        // (JwtAuthenticationFilter, @AuthenticationPrincipal, etc.)
        // works exactly the same as with regular email/password login
        return new CustomUserDetails(user);
    }



    /*
     * extractUserInfo()
     * ───────────────────
     * Normalizes the provider-specific attribute maps into a uniform
     * structure our application can work with, hiding the differences
     * between Google's and GitHub's response schemas behind one interface.
     *
     * Google attributes look like:
     *   { "sub": "108...", "email": "priya@gmail.com",
     *     "given_name": "Priya", "family_name": "Sharma" }
     *
     * GitHub attributes look like:
     *   { "id": 123456, "email": "priya@example.com",
     *     "name": "Priya Sharma", "login": "priya" }
     */
    private OAuth2UserInfo extractUserInfo(String provider, Map<String, Object> attributes) {

        return switch (provider) {

            case "google" -> new OAuth2UserInfo(
                    String.valueOf(attributes.get("sub")),
                    (String) attributes.get("email"),
                    (String) attributes.getOrDefault("given_name", ""),
                    (String) attributes.getOrDefault("family_name", "")
            );

            case "github" -> {
                String fullName = (String) attributes.getOrDefault("name", "");
                String[] parts = fullName.split(" ", 2);
                yield new OAuth2UserInfo(
                        String.valueOf(attributes.get("id")),
                        (String) attributes.getOrDefault("email", ""),
                        parts.length > 0 ? parts[0] : fullName,
                        parts.length > 1 ? parts[1] : ""
                );
            }

            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);

        };
    }



    /*
     * findOrCreateUser()
     * ────────────────────
     * returning user, first-time OAuth2 user, or account linking.
     */
    private User findOrCreateUser(OAuth2UserInfo info, String provider) {

        // CASE B: returning user — find by providerId (most common path)
        // We check by providerId first because it's more reliable than email
        // (email can theoretically change; providerId never does)
        return userRepository.findByProviderAndProviderId(provider, info.providerId())
                .orElseGet(() -> {
                    // Check if email is already in our system (CASE C — link accounts)
                    return userRepository.findByEmail(info.email())
                            .map(existingUser -> {
                                // CASE C: link the OAuth2 provider to existing account
                                log.info("Linking {} account to existing user: {}",
                                        provider, info.email());
                                existingUser.setProvider(provider);
                                existingUser.setProviderId(info.providerId());
                                return userRepository.save(existingUser);
                            })

                            .orElseGet(() -> {
                                // CASE A: brand new user — create an account for them
                                log.info("Creating new user from {} OAuth2: {}",
                                        provider, info.email());
                                User newUser = User.builder()
                                        .email(info.email())
                                        // No passwordHash — OAuth2 users don't have a password.
                                        // If they later try email/password login, they'd need to
                                        // go through a "set password" flow (outside this build's scope)
                                        .passwordHash("")
                                        .firstName(info.firstName())
                                        .lastName(info.lastName())
                                        .role(RoleType.CUSTOMER)
                                        .provider(provider)
                                        .providerId(info.providerId())
                                        .isActive(true)
                                        .build();
                                return userRepository.save(newUser);
                            });
                });
    }



    /*
     * OAuth2UserInfo — a simple record to carry normalized user data
     * between extractUserInfo() and findOrCreateUser(). Records are
     * Java 16+ and give us an immutable data carrier with zero boilerplate.
     */
    private record OAuth2UserInfo(
            String providerId,
            String email,
            String firstName,
            String lastName
    ) {}
}