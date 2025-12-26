package com.jcode.authidentityservice.repository;

import com.jcode.authidentityservice.domain.AuthEvent;
import com.jcode.authidentityservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuthEventRepository extends JpaRepository<AuthEvent, UUID> {

    List<AuthEvent> findByUser(User user);
}
