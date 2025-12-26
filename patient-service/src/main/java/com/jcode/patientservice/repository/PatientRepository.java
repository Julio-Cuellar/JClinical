package com.jcode.patientservice.repository;

import com.jcode.patientservice.domain.Patient;
import com.jcode.patientservice.domain.enums.PatientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    // Buscar por ID y Tenant (seguridad multitenancy)
    Optional<Patient> findByIdAndTenantId(UUID id, UUID tenantId);

    // Buscar todos los pacientes de un tenant
    List<Patient> findByTenantId(UUID tenantId);

    // Buscar pacientes activos de un tenant
    List<Patient> findByTenantIdAndStatus(UUID tenantId, PatientStatus status);

    // Buscar por CURP y tenant
    Optional<Patient> findByCurpAndTenantId(String curp, UUID tenantId);

    // Verificar si existe CURP en tenant
    boolean existsByCurpAndTenantId(String curp, UUID tenantId);

    // Buscar pacientes por nombre (búsqueda parcial)
    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId " +
            "AND p.status = :status " +
            "AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Patient> searchByNameInTenant(@Param("tenantId") UUID tenantId,
                                       @Param("status") PatientStatus status,
                                       @Param("searchTerm") String searchTerm);

    // Buscar pacientes por IDs y tenant (para filtrado por doctor)
    @Query("SELECT p FROM Patient p WHERE p.id IN :patientIds AND p.tenantId = :tenantId")
    List<Patient> findByIdsAndTenantId(@Param("patientIds") List<UUID> patientIds,
                                       @Param("tenantId") UUID tenantId);

    // Contar pacientes activos por tenant
    long countByTenantIdAndStatus(UUID tenantId, PatientStatus status);
}
