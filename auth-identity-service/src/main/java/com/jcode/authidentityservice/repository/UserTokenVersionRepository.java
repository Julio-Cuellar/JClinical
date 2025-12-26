package com.jcode.authidentityservice.repository;

import com.jcode.authidentityservice.domain.UserTokenVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserTokenVersionRepository extends JpaRepository<UserTokenVersion, UUID> {
}
