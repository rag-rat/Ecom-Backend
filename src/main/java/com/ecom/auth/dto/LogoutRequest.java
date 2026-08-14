package com.ecom.auth.dto;

import lombok.Data;


@Data
public class LogoutRequest {

    // Optional — we'll try to delete if present
    private String refreshToken;
}
