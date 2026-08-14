package com.ecom.user.dto;

import lombok.Builder;
import lombok.Data;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;


@Data
@Builder
public class UserResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String role;
    private boolean active;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
}
