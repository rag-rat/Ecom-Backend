package com.ecom.admin.controller;


import com.ecom.common.dto.ApiResponse;
import com.ecom.common.dto.PageResponse;
import com.ecom.user.dto.UpdateUserRoleRequest;
import com.ecom.user.dto.UserResponse;
import com.ecom.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - User Management", description = "Admin operations on customer accounts")
public class AdminUserController {

    private final UserService userService;


    // Get All Users

    @GetMapping
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<UserResponse> response = userService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success("Users fetched", response));
    }


    // Search users by name or email

    @GetMapping("/search")
    @Operation(summary = "Search users by name or email")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<UserResponse> response = userService.searchUsers(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Search results", response));
    }



    // Search User by user_id

    @GetMapping("/{userId}")
    @Operation(summary = "Get a specific user's details")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable String userId) {

        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User fetched", response));
    }


    // Activate a User Account

    @PutMapping("/{userId}/activate")
    @Operation(summary = "Activate a user account")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @PathVariable String userId) {

        UserResponse response = userService.toggleUserActive(userId, true);
        return ResponseEntity.ok(ApiResponse.success("User activated", response));
    }


    // Deactivate a User Account

    @PutMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
            @PathVariable String userId) {

        UserResponse response = userService.toggleUserActive(userId, false);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", response));
    }


    // Change a User Role

    @PutMapping("/{userId}/role")
    @Operation(summary = "Change a user's role (CUSTOMER ↔ ADMIN)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        UserResponse response = userService.updateUserRole(userId, request.getRole());
        return ResponseEntity.ok(ApiResponse.success("User role updated", response));
    }

}
