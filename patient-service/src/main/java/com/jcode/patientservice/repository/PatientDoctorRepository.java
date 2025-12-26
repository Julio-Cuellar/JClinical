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

    // Relaciones de un paciente en un tenant
    List<PatientDoctor> findByPatientIdAndTenantCode(UUID patientId, String tenantCode);

    // Pacientes de un doctor en un tenant
    List<PatientDoctor> findByDoctorUserIdAndTenantCode(UUID doctorUserId, String tenantCode);

    // Verificar relación doctor-paciente
    boolean existsByPatientIdAndDoctorUserIdAndTenantCode(UUID patientId,
                                                          UUID doctorUserId,
                                                          String tenantCode);

    // Relación específica
    Optional<PatientDoctor> findByPatientIdAndDoctorUserIdAndTenantCode(UUID patientId,
                                                                        UUID doctorUserId,
                                                                        String tenantCode);

    // Médico principal de un paciente
    @Query("SELECT pd FROM PatientDoctor pd " +
            "WHERE pd.patient.id = :patientId " +
            "AND pd.tenantCode = :tenantCode " +
            "AND pd.isPrimary = true")
    Optional<PatientDoctor> findPrimaryDoctorForPatient(@Param("patientId") UUID patientId,
                                                        @Param("tenantCode") String tenantCode);

    // IDs de pacientes de un doctor (para filtrado)
    @Query("SELECT pd.patient.id FROM PatientDoctor pd " +
            "WHERE pd.doctorUserId = :doctorUserId " +
            "AND pd.tenantCode = :tenantCode")
    List<UUID> findPatientIdsByDoctorAndTenantCode(@Param("doctorUserId") UUID doctorUserId,
                                                   @Param("tenantCode") String tenantCode);

    // Eliminar relaciones de un paciente
    void deleteByPatientIdAndTenantCode(UUID patientId, String tenantCode);

    // Contar médicos asignados a un paciente
    long countByPatientIdAndTenantCode(UUID patientId, String tenantCode);

    // Contar pacientes de un doctor
    long countByDoctorUserIdAndTenantCode(UUID doctorUserId, String tenantCode);
}
