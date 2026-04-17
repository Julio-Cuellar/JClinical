package com.jcode.jclinical.infrastructure.persistence.repository;

import com.jcode.jclinical.infrastructure.persistence.entity.ExpedienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataExpedienteRepository extends JpaRepository<ExpedienteEntity, UUID> {
}