package com.jcode.authidentityservice.controller;

import com.jcode.authidentityservice.dto.AuthResponse;
import com.jcode.authidentityservice.dto.CreateTenantUserRequest;
import com.jcode.authidentityservice.dto.LoginRequest;
import com.jcode.authidentityservice.dto.LogoutRequest;
import com.jcode.authidentityservice.dto.RefreshTokenRequest;
import com.jcode.authidentityservice.dto.RegisterUserRequest;
import com.jcode.authidentityservice.dto.UserResponse;
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

    /**
     * Registro inicial de usuario: crea usuario y, si es el primero,
     * también crea el tenant.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterUserRequest request) {
        UserResponse response = authService.registerUser(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Login: devuelve accessToken + refreshToken.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh token: genera nuevo accessToken (y opcionalmente nuevo refreshToken).
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout: revoca el refreshToken actual (y opcionalmente todos los del usuario).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Crear usuario dentro del tenant del usuario autenticado.
     * Permitido para:
     *  - ADMINPLATAFORMA
     *  - TENANT_OWNER
     *  - TENANT_ADMINCONSULTORIO
     *
     * Ojo: en SecurityConfig/JwtAuthenticationFilter debes estar
     * construyendo authorities como "ROLE_" + roleName.
     */
    @PostMapping("/tenant/users")
    @PreAuthorize(
            "hasRole('ADMINPLATAFORMA') " +
                    "or hasRole('TENANT_OWNER') " +
                    "or hasRole('TENANT_ADMINCONSULTORIO')"
    )
    public ResponseEntity<UserResponse> createUserInTenant(@RequestBody CreateTenantUserRequest request) {
        UserResponse response = authService.createUserInCurrentTenant(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint solo para ADMINPLATAFORMA.
     */
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMINPLATAFORMA')")
    public String adminOnly() {
        return "Acceso sólo para ADMINPLATAFORMA";
    }

    /**
     * Endpoint solo para DOCTOR.
     */
    @GetMapping("/doctor-only")
    @PreAuthorize("hasRole('DOCTOR')")
    public String doctorOnly() {
        return "Acceso sólo para DOCTOR";
    }

    /**
     * Endpoint accesible para cualquier usuario autenticado (cualquier rol).
     */
    @GetMapping("/universal")
    @PreAuthorize("isAuthenticated()")
    public String universal() {
        return "Acceso para cualquier usuario autenticado";
    }

    /**
     * Endpoint público de prueba.
     */
    @GetMapping("/hola")
    public String holaMundo() {
        return "¡Hola, Mundo!";
    }
}
