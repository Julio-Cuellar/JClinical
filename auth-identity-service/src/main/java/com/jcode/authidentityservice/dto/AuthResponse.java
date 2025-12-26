package com.jcode.authidentityservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;      // normalmente "Bearer"
    private long expiresIn;        // segundos de vida del access token
}
