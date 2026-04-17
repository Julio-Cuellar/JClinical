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

    // Buscar por ID y tenant
    Optional<Patient> findByIdAndTenantCode(UUID id, String tenantCode);

    // Todos los pacientes de un tenant
    List<Patient> findByTenantCode(String tenantCode);

    // Pacientes activos de un tenant
    List<Patient> findByTenantCodeAndStatus(String tenantCode, PatientStatus status);

    // Buscar por CURP y tenant
    Optional<Patient> findByCurpAndTenantCode(String curp, String tenantCode);

    // Verificar si existe CURP en tenant
    boolean existsByCurpAndTenantCode(String curp, String tenantCode);

    // Buscar pacientes por nombre (búsqueda parcial)
    @Query("SELECT p FROM Patient p " +
            "WHERE p.tenantCode = :tenantCode " +
            "AND p.status = :status " +
            "AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Patient> searchByNameInTenant(@Param("tenantCode") String tenantCode,
                                       @Param("status") PatientStatus status,
                                       @Param("searchTerm") String searchTerm);

    // Buscar pacientes por IDs y tenant (para filtrado por doctor)
    @Query("SELECT p FROM Patient p " +
            "WHERE p.id IN :patientIds AND p.tenantCode = :tenantCode")
    List<Patient> findByIdsAndTenantCode(@Param("patientIds") List<UUID> patientIds,
                                         @Param("tenantCode") String tenantCode);

    // Contar pacientes activos por tenant
    long countByTenantCodeAndStatus(String tenantCode, PatientStatus status);
}
