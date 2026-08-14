package com.ecom.auth.dto;


import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;


    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;



//    @Pattern(
//            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
//            message = "Password must contain uppercase, lowercase, and a number"
//    )
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;


    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Please provide a valid phone number"
    )
    private String phoneNumber;
}
