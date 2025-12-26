package com.jcode.patientservice.service;

import com.jcode.patientservice.domain.Patient;
import com.jcode.patientservice.domain.PatientDoctor;
import com.jcode.patientservice.domain.enums.PatientStatus;
import com.jcode.patientservice.dto.PatientRequestDTO;
import com.jcode.patientservice.dto.PatientResponseDTO;
import com.jcode.patientservice.dto.PatientSummaryDTO;
import com.jcode.patientservice.dto.PatientUpdateDTO;
import com.jcode.patientservice.exception.DuplicateCurpException;
import com.jcode.patientservice.exception.PatientNotFoundException;
import com.jcode.patientservice.exception.UnauthorizedAccessException;
import com.jcode.patientservice.mapper.PatientMapper;
import com.jcode.patientservice.repository.PatientDoctorRepository;
import com.jcode.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientDoctorRepository patientDoctorRepository;
    private final PatientMapper patientMapper;

    /**
     * Crear un nuevo paciente
     */
    @Transactional
    public PatientResponseDTO createPatient(PatientRequestDTO requestDTO,
                                            UUID tenantId,
                                            UUID createdByUserId) {
        log.debug("Creando paciente en tenant: {} por usuario: {}", tenantId, createdByUserId);

        // Validar CURP único dentro del tenant
        if (requestDTO.getCurp() != null &&
                patientRepository.existsByCurpAndTenantId(requestDTO.getCurp(), tenantId)) {
            throw new DuplicateCurpException(requestDTO.getCurp());
        }

        // Crear entidad Patient
        Patient patient = patientMapper.toEntity(requestDTO);
        patient.setTenantId(tenantId);
        patient.setCreatedByUserId(createdByUserId);
        patient.setStatus(PatientStatus.ACTIVE);

        // Guardar paciente
        Patient savedPatient = patientRepository.save(patient);
        log.info("Paciente creado con ID: {} en tenant: {}", savedPatient.getId(), tenantId);

        // Crear relación con el médico que lo registró
        PatientDoctor patientDoctor = PatientDoctor.builder()
                .tenantId(tenantId)
                .patient(savedPatient)
                .doctorUserId(createdByUserId)
                .isPrimary(true)
                .build();

        patientDoctorRepository.save(patientDoctor);
        log.debug("Relación médico-paciente creada: doctor={}, patient={}",
                createdByUserId, savedPatient.getId());

        // TODO: Publicar evento patient.created a RabbitMQ

        return patientMapper.toResponseDTO(savedPatient);
    }

    /**
     * Obtener paciente por ID (con validación de acceso)
     */
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(UUID patientId,
                                             UUID tenantId,
                                             UUID userId,
                                             List<String> roles) {
        log.debug("Buscando paciente ID: {} en tenant: {} por usuario: {}",
                patientId, tenantId, userId);

        // Buscar paciente
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new PatientNotFoundException(patientId, tenantId));

        // Validar acceso
        validateAccess(patientId, tenantId, userId, roles);

        return patientMapper.toResponseDTO(patient);
    }

    /**
     * Obtener todos los pacientes según rol del usuario
     */
    @Transactional(readOnly = true)
    public List<PatientSummaryDTO> getAllPatients(UUID tenantId,
                                                  UUID userId,
                                                  List<String> roles) {
        log.debug("Obteniendo pacientes para tenant: {} usuario: {} roles: {}",
                tenantId, userId, roles);

        List<Patient> patients;

        // Si es OWNER o ADMIN, puede ver todos los pacientes del tenant
        if (hasAdminRole(roles)) {
            patients = patientRepository.findByTenantIdAndStatus(tenantId, PatientStatus.ACTIVE);
            log.debug("Usuario con rol admin - {} pacientes encontrados", patients.size());
        } else {
            // Si es DOCTOR, solo ve sus pacientes
            List<UUID> patientIds = patientDoctorRepository
                    .findPatientIdsByDoctorAndTenant(userId, tenantId);

            if (patientIds.isEmpty()) {
                log.debug("Doctor no tiene pacientes asignados");
                return List.of();
            }

            patients = patientRepository.findByIdsAndTenantId(patientIds, tenantId);
            log.debug("Doctor - {} pacientes encontrados", patients.size());
        }

        return patientMapper.toSummaryDTOList(patients);
    }

    /**
     * Buscar pacientes por nombre
     */
    @Transactional(readOnly = true)
    public List<PatientSummaryDTO> searchPatientsByName(String searchTerm,
                                                        UUID tenantId,
                                                        UUID userId,
                                                        List<String> roles) {
        log.debug("Buscando pacientes con término: '{}' en tenant: {}", searchTerm, tenantId);

        List<Patient> patients = patientRepository.searchByNameInTenant(
                tenantId, PatientStatus.ACTIVE, searchTerm);

        // Si no es admin, filtrar solo sus pacientes
        if (!hasAdminRole(roles)) {
            List<UUID> doctorPatientIds = patientDoctorRepository
                    .findPatientIdsByDoctorAndTenant(userId, tenantId);

            patients = patients.stream()
                    .filter(p -> doctorPatientIds.contains(p.getId()))
                    .toList();
        }

        log.debug("Búsqueda completada - {} pacientes encontrados", patients.size());
        return patientMapper.toSummaryDTOList(patients);
    }

    /**
     * Actualizar datos del paciente
     */
    @Transactional
    public PatientResponseDTO updatePatient(UUID patientId,
                                            PatientUpdateDTO updateDTO,
                                            UUID tenantId,
                                            UUID userId,
                                            List<String> roles) {
        log.debug("Actualizando paciente ID: {} en tenant: {}", patientId, tenantId);

        // Buscar paciente
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new PatientNotFoundException(patientId, tenantId));

        // Validar acceso
        validateAccess(patientId, tenantId, userId, roles);

        // Actualizar solo campos permitidos
        if (updateDTO.getPhone() != null) {
            patient.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getEmail() != null) {
            patient.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getAddressLine1() != null) {
            patient.setAddressLine1(updateDTO.getAddressLine1());
        }
        if (updateDTO.getAddressLine2() != null) {
            patient.setAddressLine2(updateDTO.getAddressLine2());
        }
        if (updateDTO.getCity() != null) {
            patient.setCity(updateDTO.getCity());
        }
        if (updateDTO.getState() != null) {
            patient.setState(updateDTO.getState());
        }
        if (updateDTO.getZipCode() != null) {
            patient.setZipCode(updateDTO.getZipCode());
        }
        if (updateDTO.getCountry() != null) {
            patient.setCountry(updateDTO.getCountry());
        }

        Patient updatedPatient = patientRepository.save(patient);
        log.info("Paciente actualizado: {}", patientId);

        // TODO: Publicar evento patient.updated a RabbitMQ

        return patientMapper.toResponseDTO(updatedPatient);
    }

    /**
     * Desactivar paciente (baja lógica)
     */
    @Transactional
    public void deactivatePatient(UUID patientId,
                                  UUID tenantId,
                                  UUID userId,
                                  List<String> roles) {
        log.debug("Desactivando paciente ID: {} en tenant: {}", patientId, tenantId);

        // Solo OWNER o ADMIN pueden desactivar
        if (!hasAdminRole(roles)) {
            throw new UnauthorizedAccessException(
                    "Solo administradores pueden desactivar pacientes");
        }

        // Buscar paciente
        Patient patient = patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new PatientNotFoundException(patientId, tenantId));

        // Desactivar
        patient.setStatus(PatientStatus.INACTIVE);
        patientRepository.save(patient);

        log.info("Paciente desactivado: {}", patientId);

        // TODO: Publicar evento patient.deactivated a RabbitMQ
    }

    /**
     * Validar si el usuario tiene acceso al paciente
     */
    private void validateAccess(UUID patientId, UUID tenantId, UUID userId, List<String> roles) {
        // Si es admin, tiene acceso total
        if (hasAdminRole(roles)) {
            return;
        }

        // Si es doctor, verificar que tenga relación con el paciente
        boolean hasAccess = patientDoctorRepository
                .existsByPatientIdAndDoctorUserIdAndTenantId(patientId, userId, tenantId);

        if (!hasAccess) {
            log.warn("Acceso denegado: usuario {} intentó acceder al paciente {}",
                    userId, patientId);
            throw new UnauthorizedAccessException(userId, patientId);
        }
    }

    /**
     * Verificar si el usuario tiene rol administrativo
     */
    private boolean hasAdminRole(List<String> roles) {
        return roles.contains("TENANT_OWNER") ||
                roles.contains("TENANT_ADMIN_CONSULTORIO") ||
                roles.contains("ADMIN_PLATAFORMA");
    }

    /**
     * Obtener estadísticas de pacientes
     */
    @Transactional(readOnly = true)
    public long countActivePatients(UUID tenantId) {
        return patientRepository.countByTenantIdAndStatus(tenantId, PatientStatus.ACTIVE);
    }
}
