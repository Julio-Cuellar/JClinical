package com.jcode.authidentityservice.repository;

import com.jcode.authidentityservice.domain.TenantMembership;
import com.jcode.authidentityservice.domain.User;
import com.jcode.authidentityservice.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    List<TenantMembership> findByUser(User user);

    List<TenantMembership> findByTenant(Tenant tenant);

    Optional<TenantMembership> findByUserAndTenant(User user, Tenant tenant);
}
