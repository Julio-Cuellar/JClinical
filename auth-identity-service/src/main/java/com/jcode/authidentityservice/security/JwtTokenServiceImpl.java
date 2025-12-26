package com.jcode.authidentityservice.security;

import com.jcode.authidentityservice.domain.User;
import com.jcode.authidentityservice.domain.enums.UserType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_USER_TYPE = "user_type";
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        // Si la secret está en Base64, la decodificamos; si no, usamos los bytes tal cual.
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ex) {
            return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        }
    }

    @Override
    public String generateAccessToken(User user, String tenantCode, Set<String> roleNames) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getAccessTokenExpiration());

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TENANT_ID, tenantCode);
        claims.put(CLAIM_USER_TYPE, Optional.ofNullable(user.getUserType()).map(UserType::name).orElse(null));
        claims.put(CLAIM_ROLES, roleNames);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId().toString())
                .setIssuer(jwtProperties.getIssuer())
                .setAudience(jwtProperties.getAudience())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean validateAccessToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // firma inválida, token expirado, mal formado, etc.
            return false;
        }
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        String sub = claims.getSubject();
        return UUID.fromString(sub);
    }

    @Override
    public String getTenantCodeFromToken(String token) {
        Claims claims = parseClaims(token);
        Object value = claims.get(CLAIM_TENANT_ID);
        return value != null ? value.toString() : null;
    }

    @Override
    public Set<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);
        Object raw = claims.get(CLAIM_ROLES);
        if (raw instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
