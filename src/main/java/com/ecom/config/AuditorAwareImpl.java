package com.ecom.config;


import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.swing.text.html.Option;
import java.util.Optional;



@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor(){

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken){
            return Optional.of("SYSTEM");
        }

        Object principal = authentication.getPrincipal();

        if(principal instanceof UserDetails userDetails){
            return Optional.of(userDetails.getUsername());
        }

        if (principal instanceof String principalStr) {
            return Optional.of(principalStr);
        }

        return Optional.of("SYSTEM");
    }
}
