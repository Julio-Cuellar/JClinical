package com.jcode.authidentityservice.service.impl;

import com.jcode.authidentityservice.domain.*;
import com.jcode.authidentityservice.domain.enums.*;
import com.jcode.authidentityservice.dto.*;
import com.jcode.authidentityservice.repository.*;
import com.jcode.authidentityservice.security.JwtProperties;
import com.jcode.authidentityservice.security.JwtTokenService;
import com.jcode.authidentityservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserTokenVersionRepository userTokenVersionRepository;

    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    // ---- Registro público (crea o usa tenant según request) ----

    @Override
    public UserResponse registerUser(RegisterUserRequest request) {
        validateNewUser(request);

        Instant now = Instant.now();

        Tenant tenant = tenantRepository.findByCode(request.getTenantCode())
                .orElseGet(() -> {
                    Tenant t = Tenant.builder()
                            .name(request.getTenantName())
                            .code(request.getTenantCode())
                            .status(TenantStatus.ACTIVE)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return tenantRepository.save(t);
                });

        UserType userType = UserType.valueOf(request.getUserType());
        UserStatus status = UserStatus.ACTIVE;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .userType(userType)
                .status(status)
                .cedulaProfesional(request.getCedulaProfesional())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Role defaultRole = getDefaultRoleForUserType(userType);
        user.getRoles().add(defaultRole);

        user = userRepository.save(user);

        boolean isFirstUserInTenant = tenantMembershipRepository.findByTenant(tenant).isEmpty();
        TenantRole tenantRole = isFirstUserInTenant ? TenantRole.OWNER : TenantRole.DOCTOR_ASOCIADO;

        TenantMembership membership = TenantMembership.builder()
                .tenantRole(tenantRole)
                .isDefault(isFirstUserInTenant)
                .createdAt(now)
                .updatedAt(now)
                .user(user)
                .tenant(tenant)
                .build();

        tenantMembershipRepository.save(membership);

        return mapToUserResponse(user);
    }

    private void validateNewUser(RegisterUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username ya está en uso");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email ya está en uso");
        }
        if (!tenantRepository.existsByCode(request.getTenantCode())
                && (request.getTenantName() == null || request.getTenantName().isBlank())) {
            throw new IllegalArgumentException("Se requiere tenantName para crear un nuevo tenant");
        }
    }

    private Role getDefaultRoleForUserType(UserType userType) {
        String roleName;
        switch (userType) {
            case MEDICO -> roleName = "DOCTOR";
            case PERSONAL_SALUD -> roleName = "ASISTENTE";
            case PACIENTE -> roleName = "PACIENTE";
            case ADMIN -> roleName = "ADMINPLATAFORMA";
            default -> throw new IllegalStateException("Tipo de usuario no soportado: " + userType);
        }
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Rol por defecto no configurado: " + roleName));
    }

    // ---- Creación de usuario dentro del tenant actual (admin/owner) ----

    @Override
    public UserResponse createUserInCurrentTenant(CreateTenantUserRequest request) {
        // Validaciones básicas de username/email
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username ya está en uso");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email ya está en uso");
        }

        // Validar permisos del usuario actual (además de @PreAuthorize)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No hay usuario autenticado en el contexto de seguridad");
        }

        String currentUsername = auth.getName();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

        boolean hasAdminRole = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a ->
                        a.equals("ROLE_ADMINPLATAFORMA") ||   // admin global
                                a.equals("ROLE_DOCTOR") ||            // owner actual (primer médico)
                                a.equals("TENANT_OWNER")              // como viene hoy del token
                );

        if (!hasAdminRole) {
            throw new IllegalStateException(
                    "Usuario " + currentUsername +
                            " no tiene permisos para crear usuarios en su tenant. Authorities actuales: " +
                            authorities
            );
        }

        String tenantCode = getCurrentTenantCode();
        Tenant tenant = tenantRepository.findByCode(tenantCode)
                .orElseThrow(() -> new IllegalStateException("Tenant no encontrado para el usuario actual: " + tenantCode));

        Instant now = Instant.now();

        UserType userType = UserType.valueOf(request.getUserType());
        UserStatus status = UserStatus.ACTIVE;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .userType(userType)
                .status(status)
                .cedulaProfesional(request.getCedulaProfesional())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Role defaultRole = getDefaultRoleForUserType(userType);
        user.getRoles().add(defaultRole);

        user = userRepository.save(user);

        // Determinar rol dentro del tenant
        TenantRole tenantRole;
        if (request.getTenantRole() != null && !request.getTenantRole().isBlank()) {
            tenantRole = TenantRole.valueOf(request.getTenantRole());
        } else {
            tenantRole = switch (userType) {
                case MEDICO -> TenantRole.DOCTOR_ASOCIADO;
                case PERSONAL_SALUD -> TenantRole.ASISTENTE;
                case PACIENTE -> TenantRole.DOCTOR_ASOCIADO;
                case ADMIN -> TenantRole.ADMIN_CONSULTORIO;
            };
        }

        TenantMembership membership = TenantMembership.builder()
                .tenantRole(tenantRole)
                .isDefault(false)
                .createdAt(now)
                .updatedAt(now)
                .user(user)
                .tenant(tenant)
                .build();

        tenantMembershipRepository.save(membership);

        return mapToUserResponse(user);
    }

    private String getCurrentTenantCode() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("No hay usuario autenticado");
        }
        Object details = auth.getDetails();
        if (details == null) {
            throw new IllegalStateException("No se encontró tenant en el contexto");
        }
        return details.toString();
    }

    // ---- Login ----

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Usuario no está activo");
        }

        Tenant tenant = tenantRepository.findByCode(request.getTenantCode())
                .orElseThrow(() -> new IllegalArgumentException("Tenant no encontrado"));

        TenantMembership membership = tenantMembershipRepository.findByUserAndTenant(user, tenant)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no pertenece a este tenant"));

        Set<String> roleNames = buildRoleNamesForToken(user, membership);

        String accessToken = jwtTokenService.generateAccessToken(user, tenant.getCode(), roleNames);

        String rawRefreshToken = generateRawRefreshToken();
        String refreshTokenHash = hashToken(rawRefreshToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getRefreshTokenExpiration());

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(refreshTokenHash)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .revoked(false)
                .user(user)
                .tenant(tenant)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration())
                .build();
    }

    private Set<String> buildRoleNamesForToken(User user, TenantMembership membership) {
        Set<String> roleNames = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        roleNames.add("TENANT_" + membership.getTenantRole().name());
        return roleNames;
    }

    // ---- Refresh token ----

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawRefreshToken = request.getRefreshToken();
        String hash = hashToken(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expirado o revocado");
        }

        User user = stored.getUser();
        Tenant tenant = stored.getTenant();

        TenantMembership membership = tenantMembershipRepository.findByUserAndTenant(user, tenant)
                .orElseThrow(() -> new IllegalStateException("Usuario ya no pertenece al tenant"));

        Set<String> roleNames = buildRoleNamesForToken(user, membership);

        String newAccessToken = jwtTokenService.generateAccessToken(user, tenant.getCode(), roleNames);

        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        String newRawRefreshToken = generateRawRefreshToken();
        String newHash = hashToken(newRawRefreshToken);

        Instant now = Instant.now();
        Instant newExpiresAt = now.plusSeconds(jwtProperties.getRefreshTokenExpiration());

        RefreshToken newToken = RefreshToken.builder()
                .tokenHash(newHash)
                .issuedAt(now)
                .expiresAt(newExpiresAt)
                .revoked(false)
                .user(user)
                .tenant(tenant)
                .build();

        refreshTokenRepository.save(newToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration())
                .build();
    }

    // ---- Logout ----

    @Override
    public void logout(LogoutRequest request) {
        String hash = hashToken(request.getRefreshToken());

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido"));

        User user = token.getUser();

        if (request.isLogoutAllDevices()) {
            List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
            Instant now = Instant.now();
            for (RefreshToken t : activeTokens) {
                t.setRevoked(true);
                t.setRevokedAt(now);
            }
            refreshTokenRepository.saveAll(activeTokens);
            incrementUserTokenVersion(user);
        } else {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        }
    }

    @Override
    public void logoutAllForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        Instant now = Instant.now();
        for (RefreshToken t : activeTokens) {
            t.setRevoked(true);
            t.setRevokedAt(now);
        }
        refreshTokenRepository.saveAll(activeTokens);
        incrementUserTokenVersion(user);
    }

    private void incrementUserTokenVersion(User user) {
        UserTokenVersion version = userTokenVersionRepository.findById(user.getId())
                .orElseGet(() -> UserTokenVersion.builder()
                        .userId(user.getId())
                        .currentVersion(0)
                        .user(user)
                        .build());

        version.setCurrentVersion(version.getCurrentVersion() + 1);
        userTokenVersionRepository.save(version);
    }

    // ---- Helpers ----

    private String generateRawRefreshToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo inicializar SHA-256", e);
        }
    }

    private UserResponse mapToUserResponse(User user) {
        Set<String> roleNames = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType() != null ? user.getUserType().name() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .roles(roleNames)
                .build();
    }
}
