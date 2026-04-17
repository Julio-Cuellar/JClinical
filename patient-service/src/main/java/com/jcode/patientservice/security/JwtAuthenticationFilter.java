package com.jcode.patientservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("No JWT token found in request");
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = authHeader.substring(7);

            if (!jwtUtil.validateToken(jwt)) {
                log.warn("Invalid JWT token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return;
            }

            UUID userId = jwtUtil.extractUserId(jwt);
            String tenantCode = jwtUtil.extractTenantCode(jwt);
            List<String> roles = jwtUtil.extractRoles(jwt);

            log.debug("JWT validated - userId: {}, tenantCode: {}, roles: {}",
                    userId, tenantCode, roles);

            // Atributos internos para el controlador/servicio
            request.setAttribute("X-User-Id", userId);
            request.setAttribute("X-Tenant-Code", tenantCode);
            request.setAttribute("X-Roles", String.join(",", roles));

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> {
                        if (!role.startsWith("ROLE_") && !role.startsWith("TENANT_")) {
                            return "ROLE_" + role;
                        }
                        return role;
                    })
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("Error en filtro JWT: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Authentication error: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
