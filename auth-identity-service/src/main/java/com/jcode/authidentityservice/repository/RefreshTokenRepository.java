package com.jcode.authidentityservice.repository;

import com.jcode.authidentityservice.domain.RefreshToken;
import com.jcode.authidentityservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser(User user);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    long deleteByUser(User user);

    long deleteByExpiresAtBefore(Instant instant);
}
