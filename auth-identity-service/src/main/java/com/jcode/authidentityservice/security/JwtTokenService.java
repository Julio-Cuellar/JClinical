package com.jcode.authidentityservice.security;

import com.jcode.authidentityservice.domain.User;

import java.util.Set;
import java.util.UUID;

public interface JwtTokenService {

    String generateAccessToken(User user, String tenantCode, Set<String> roleNames);

    boolean validateAccessToken(String token);

    UUID getUserIdFromToken(String token);

    String getTenantCodeFromToken(String token);

    Set<String> getRolesFromToken(String token);
}
