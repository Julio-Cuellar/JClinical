package com.jcode.authidentityservice.service;

import com.jcode.authidentityservice.dto.TenantSummary;
import com.jcode.authidentityservice.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getById(UUID id);

    UserResponse getByUsername(String username);

    List<TenantSummary> getTenantsForUser(UUID userId);

    // asignación de roles globales (admin plataforma)
    UserResponse assignRoleToUser(UUID userId, String roleName);

    UserResponse removeRoleFromUser(UUID userId, String roleName);
}
