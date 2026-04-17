package com.jcode.authidentityservice.service;

import com.jcode.authidentityservice.dto.*;

public interface AuthService {

    UserResponse registerUser(RegisterUserRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    // Opcional: logout global por admin o cambio de contraseña
    void logoutAllForUser(String username);

    UserResponse createUserInCurrentTenant(CreateTenantUserRequest request);

}
