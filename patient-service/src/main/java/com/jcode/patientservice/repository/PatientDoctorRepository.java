package com.jcode.patientservice.repository;

import com.jcode.patientservice.domain.PatientDoctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientDoctorRepository extends JpaRepository<PatientDoctor, UUID> {

    // Buscar todas las relaciones de un paciente en un tenant
    List<PatientDoctor> findByPatientIdAndTenantId(UUID patientId, UUID tenantId);

    // Buscar todos los pacientes de un doctor en un tenant
    List<PatientDoctor> findByDoctorUserIdAndTenantId(UUID doctorUserId, UUID tenantId);

    // Verificar si existe relación entre doctor y paciente
    boolean existsByPatientIdAndDoctorUserIdAndTenantId(UUID patientId, UUID doctorUserId, UUID tenantId);

    // Buscar relación específica
    Optional<PatientDoctor> findByPatientIdAndDoctorUserIdAndTenantId(
            UUID patientId, UUID doctorUserId, UUID tenantId);

    // Buscar el médico principal de un paciente
    @Query("SELECT pd FROM PatientDoctor pd WHERE pd.patient.id = :patientId " +
            "AND pd.tenantId = :tenantId AND pd.isPrimary = true")
    Optional<PatientDoctor> findPrimaryDoctorForPatient(@Param("patientId") UUID patientId,
                                                        @Param("tenantId") UUID tenantId);

    // Obtener IDs de pacientes de un doctor (para filtrado)
    @Query("SELECT pd.patient.id FROM PatientDoctor pd WHERE pd.doctorUserId = :doctorUserId " +
            "AND pd.tenantId = :tenantId")
    List<UUID> findPatientIdsByDoctorAndTenant(@Param("doctorUserId") UUID doctorUserId,
                                               @Param("tenantId") UUID tenantId);

    // Eliminar todas las relaciones de un paciente (para cleanup)
    void deleteByPatientIdAndTenantId(UUID patientId, UUID tenantId);

    // Contar cuántos médicos tiene asignados un paciente
    long countByPatientIdAndTenantId(UUID patientId, UUID tenantId);

    // Contar cuántos pacientes tiene un doctor
    long countByDoctorUserIdAndTenantId(UUID doctorUserId, UUID tenantId);
}
