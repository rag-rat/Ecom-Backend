package com.ecom.security;


import com.ecom.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String userId)throws UsernameNotFoundException{

        log.debug("Loading user by ID: {}, userId");

        return userRepository.findById(userId)
                .map(user -> {
                    log.debug("User found: {}, role: {}", user.getEmail(), user.getRole());
                    return new CustomUserDetails(user);
                })
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UsernameNotFoundException(
                            "User not found with ID: " + userId );
                });
    }
}
