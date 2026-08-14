package com.ecom.user.dto;


import com.ecom.common.enums.RoleType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class UpdateUserRoleRequest {
    
    @NotNull(message = "Role is required")
    private RoleType role;
}
