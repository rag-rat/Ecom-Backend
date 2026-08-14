package com.ecom.security;


import com.ecom.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class CustomUserDetails implements UserDetails, OAuth2User {



    @Getter
    private final User user;

    public CustomUserDetails(User user){
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }


    @Override
    public String getPassword(){
        return user.getPasswordHash();
    }


    @Override
    public String getUsername(){
        return user.getId();
    }


    @Override
    public boolean isAccountNonExpired(){
        return true;
    }


    @Override
    public boolean isAccountNonLocked(){
        return true;
    }


    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }


    @Override
    public boolean isEnabled(){
        return user.isActive();
    }


    @Override
    public Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }


    @Override
    public String getName() {
        return user.getId();
    }
}
