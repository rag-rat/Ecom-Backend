package com.ecom.user.controller;


import com.ecom.common.dto.ApiResponse;
import com.ecom.security.CustomUserDetails;
import com.ecom.user.dto.ChangePasswordRequest;
import com.ecom.user.dto.UpdateProfileRequest;
import com.ecom.user.dto.UserResponse;
import com.ecom.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Customer profile management")
public class UserController {

    private final UserService userService;


    // Get Profile

    @GetMapping("/me")
    @Operation(summary = "Get current logged-in user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getOwnProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UserResponse response = userService.getOwnProfile(userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Profiile fetched", response));
    }



    // Update Profile

    @PutMapping("/me")
    @Operation(summary = "Update current user's profile (partial update)")
    public ResponseEntity<ApiResponse<UserResponse>> updateOwnProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {


        UserResponse response = userService.updateOwnProfile(
                userDetails.getUsername(), request);

        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }


    // Change Password

    @PutMapping("/me/password")
    @Operation(summary = "Change current user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(userDetails.getUsername(), request);

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

}
