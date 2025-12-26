package com.jcode.authidentityservice.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String tenantCode; // o tenantId, según cómo lo manejes al login
}
