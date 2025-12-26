package com.jcode.authidentityservice.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
    private boolean logoutAllDevices;
}
