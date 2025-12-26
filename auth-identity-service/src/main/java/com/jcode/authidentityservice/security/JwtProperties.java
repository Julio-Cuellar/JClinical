package com.jcode.authidentityservice.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Clave secreta para firmar el JWT (Base64 o texto plano suficientemente largo).
     */
    private String secret;

    /**
     * Issuer que se pondrá en el claim "iss".
     */
    private String issuer;

    /**
     * Audience que se pondrá en el claim "aud".
     */
    private String audience;

    /**
     * Tiempo de vida del access token en segundos.
     */
    private long accessTokenExpiration;

    /**
     * Tiempo de vida sugerido para refresh tokens (por si lo necesitas).
     */
    private long refreshTokenExpiration;
}
