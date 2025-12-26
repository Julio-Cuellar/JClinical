package com.jcode.authidentityservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtTokenService.validateAccessToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        var userId = jwtTokenService.getUserIdFromToken(token);
        String tenantCode = jwtTokenService.getTenantCodeFromToken(token);
        Set<String> roles = jwtTokenService.getRolesFromToken(token);

        var authorities = roles.stream()
                .map(roleName -> {
                    // para roles clásicos de Spring Security se recomienda prefijo ROLE_
                    if (!roleName.startsWith("ROLE_") && !roleName.startsWith("TENANT_")) {
                        return "ROLE_" + roleName;
                    }
                    return roleName;
                })
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId.toString(), // principal
                        null,
                        authorities
                );

        authentication.setDetails(tenantCode);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
