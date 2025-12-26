package com.jcode.authidentityservice.controller;

import com.jcode.authidentityservice.dto.*;
import com.jcode.authidentityservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterUserRequest request) {
        UserResponse response = authService.registerUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Crear usuario dentro del tenant del usuario autenticado.
     * Sólo permitido para ADMIN_PLATAFORMA, TENANT_OWNER o TENANT_ADMIN_CONSULTORIO.
     */
    @PostMapping("/tenant/users")
    @PreAuthorize(
            "hasRole('ADMIN_PLATAFORMA') " +
                    "or hasAuthority('TENANT_OWNER') " +
                    "or hasAuthority('TENANT_ADMIN_CONSULTORIO')"
    )
    public ResponseEntity<UserResponse> createUserInTenant(@RequestBody CreateTenantUserRequest request) {
        UserResponse response = authService.createUserInCurrentTenant(request);
        return ResponseEntity.ok(response);
    }

    // Endpoint solo para ADMIN_PLATAFORMA
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    public String adminOnly() {
        return "Acceso sólo para ADMIN_PLATAFORMA";
    }

    // Endpoint solo para DOCTOR
    @GetMapping("/doctor-only")
    @PreAuthorize("hasRole('DOCTOR')")
    public String doctorOnly() {
        return "Acceso sólo para DOCTOR";
    }

    // Endpoint accesible para cualquier usuario autenticado (cualquier rol)
    @GetMapping("/universal")
    @PreAuthorize("isAuthenticated()")
    public String universal() {
        return "Acceso para cualquier usuario autenticado";
    }

    // Endpoint público de prueba
    @GetMapping("/hola")
    public String holaMundo() {
        return "¡Hola, Mundo!";
    }
}
