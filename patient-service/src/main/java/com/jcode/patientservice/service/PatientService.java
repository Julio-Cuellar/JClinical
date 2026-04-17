package com.jcode.patientservice.service;

import com.jcode.patientservice.domain.Patient;
import com.jcode.patientservice.domain.PatientDoctor;
import com.jcode.patientservice.domain.enums.PatientStatus;
import com.jcode.patientservice.dto.PatientRequestDTO;
import com.jcode.patientservice.dto.PatientResponseDTO;
import com.jcode.patientservice.dto.PatientSummaryDTO;
import com.jcode.patientservice.dto.PatientUpdateDTO;
import com.jcode.patientservice.event.PatientCreatedEvent;
import com.jcode.patientservice.event.PatientDeactivatedEvent;
import com.jcode.patientservice.event.PatientEventPublisher;
import com.jcode.patientservice.event.PatientUpdatedEvent;
import com.jcode.patientservice.exception.DuplicateCurpException;
import com.jcode.patientservice.exception.PatientNotFoundException;
import com.jcode.patientservice.exception.UnauthorizedAccessException;
import com.jcode.patientservice.mapper.PatientMapper;
import com.jcode.patientservice.repository.PatientDoctorRepository;
import com.jcode.patientservice.repository.PatientRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientDoctorRepository patientDoctorRepository;
    private final PatientMapper patientMapper;
    private final PatientEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Crear un nuevo paciente
     */
    @Transactional
    public PatientResponseDTO createPatient(PatientRequestDTO requestDTO,
                                            String tenantCode,
                                            UUID createdByUserId) {
        log.debug("Creando paciente en tenant: {} por usuario: {}", tenantCode, createdByUserId);

        // Validar CURP único dentro del tenant
        if (requestDTO.getCurp() != null &&
                patientRepository.existsByCurpAndTenantCode(requestDTO.getCurp(), tenantCode)) {
            throw new DuplicateCurpException(requestDTO.getCurp());
        }

        // Crear entidad Patient
        Patient patient = patientMapper.toEntity(requestDTO);
        patient.setTenantCode(tenantCode);
        patient.setCreatedByUserId(createdByUserId);
        patient.setStatus(PatientStatus.ACTIVE);

        // Guardar paciente
        Patient savedPatient = patientRepository.save(patient);
        log.info("Paciente creado con ID: {} en tenant: {}", savedPatient.getId(), tenantCode);

        // Crear relación con el médico que lo registró
        PatientDoctor patientDoctor = PatientDoctor.builder()
                .tenantCode(tenantCode)
                .patient(savedPatient)
                .doctorUserId(createdByUserId)
                .isPrimary(true)
                .build();

        patientDoctorRepository.save(patientDoctor);
        log.debug("Relación médico-paciente creada: doctor={}, patient={}",
                createdByUserId, savedPatient.getId());

        // Publicar evento patient.created
        PatientCreatedEvent event = PatientCreatedEvent.builder()
                .patientId(savedPatient.getId())
                .tenantCode(tenantCode)
                .firstName(savedPatient.getFirstName())
                .lastName(savedPatient.getLastName())
                .middleName(savedPatient.getMiddleName())
                .dateOfBirth(savedPatient.getDateOfBirth())
                .sex(savedPatient.getSex())
                .curp(savedPatient.getCurp())
                .phone(savedPatient.getPhone())
                .email(savedPatient.getEmail())
                .createdByUserId(createdByUserId)
                .primaryDoctorUserId(createdByUserId)
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishPatientCreated(event);

        return patientMapper.toResponseDTO(savedPatient);
    }

    /**
     * Obtener paciente por ID (con validación de acceso)
     */
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(UUID patientId,
                                             String tenantCode,
                                             UUID userId,
                                             List<String> roles) {
        log.debug("Buscando paciente ID: {} en tenant: {} por usuario: {}",
                patientId, tenantCode, userId);

        Patient patient = patientRepository.findByIdAndTenantCode(patientId, tenantCode)
                .orElseThrow(() -> new PatientNotFoundException(patientId, tenantCode));

        validateAccess(patientId, tenantCode, userId, roles);

        return patientMapper.toResponseDTO(patient);
    }

    /**
     * Obtener todos los pacientes según rol del usuario
     */
    @Transactional(readOnly = true)
    public List<PatientSummaryDTO> getAllPatients(String tenantCode,
                                                  UUID userId,
                                                  List<String> roles) {
        log.debug("Obteniendo pacientes para tenant: {} usuario: {} roles: {}",
                tenantCode, userId, roles);

        List<Patient> patients;

        if (hasAdminRole(roles)) {
            patients = patientRepository.findByTenantCodeAndStatus(tenantCode, PatientStatus.ACTIVE);
            log.debug("Usuario con rol admin - {} pacientes encontrados", patients.size());
        } else {
            List<UUID> patientIds = patientDoctorRepository
                    .findPatientIdsByDoctorAndTenantCode(userId, tenantCode);

            if (patientIds.isEmpty()) {
                log.debug("Doctor no tiene pacientes asignados");
                return List.of();
            }

            patients = patientRepository.findByIdsAndTenantCode(patientIds, tenantCode);
            log.debug("Doctor - {} pacientes encontrados", patients.size());
        }

        return patientMapper.toSummaryDTOList(patients);
    }

    /**
     * Buscar pacientes por nombre
     */
    @Transactional(readOnly = true)
    public List<PatientSummaryDTO> searchPatientsByName(String searchTerm,
                                                        String tenantCode,
                                                        UUID userId,
                                                        List<String> roles) {
        log.debug("Buscando pacientes con término: '{}' en tenant: {}", searchTerm, tenantCode);

        List<Patient> patients = patientRepository.searchByNameInTenant(
                tenantCode, PatientStatus.ACTIVE, searchTerm);

        if (!hasAdminRole(roles)) {
            List<UUID> doctorPatientIds = patientDoctorRepository
                    .findPatientIdsByDoctorAndTenantCode(userId, tenantCode);

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
                                            String tenantCode,
                                            UUID userId,
                                            List<String> roles) {
        log.debug("Actualizando paciente ID: {} en tenant: {}", patientId, tenantCode);

        Patient patient = patientRepository.findByIdAndTenantCode(patientId, tenantCode)
                .orElseThrow(() -> new PatientNotFoundException(patientId, tenantCode));

        validateAccess(patientId, tenantCode, userId, roles);

        Map<String, Object> updatedFields = new HashMap<>();

        if (updateDTO.getPhone() != null) {
            patient.setPhone(updateDTO.getPhone());
            updatedFields.put("phone", updateDTO.getPhone());
        }
        if (updateDTO.getEmail() != null) {
            patient.setEmail(updateDTO.getEmail());
            updatedFields.put("email", updateDTO.getEmail());
        }
        if (updateDTO.getAddressLine1() != null) {
            patient.setAddressLine1(updateDTO.getAddressLine1());
            updatedFields.put("addressLine1", updateDTO.getAddressLine1());
        }
        if (updateDTO.getAddressLine2() != null) {
            patient.setAddressLine2(updateDTO.getAddressLine2());
            updatedFields.put("addressLine2", updateDTO.getAddressLine2());
        }
        if (updateDTO.getCity() != null) {
            patient.setCity(updateDTO.getCity());
            updatedFields.put("city", updateDTO.getCity());
        }
        if (updateDTO.getState() != null) {
            patient.setState(updateDTO.getState());
            updatedFields.put("state", updateDTO.getState());
        }
        if (updateDTO.getZipCode() != null) {
            patient.setZipCode(updateDTO.getZipCode());
            updatedFields.put("zipCode", updateDTO.getZipCode());
        }
        if (updateDTO.getCountry() != null) {
            patient.setCountry(updateDTO.getCountry());
            updatedFields.put("country", updateDTO.getCountry());
        }

        Patient updatedPatient = patientRepository.save(patient);
        log.info("Paciente actualizado: {}", patientId);

        try {
            String updatedFieldsJson = objectMapper.writeValueAsString(updatedFields);
            PatientUpdatedEvent event = PatientUpdatedEvent.builder()
                    .patientId(patientId)
                    .tenantCode(tenantCode)
                    .updatedByUserId(userId)
                    .updatedFields(updatedFieldsJson)
                    .timestamp(LocalDateTime.now())
                    .build();

            eventPublisher.publishPatientUpdated(event);
        } catch (JsonProcessingException e) {
            log.error("Error serializando campos actualizados: {}", e.getMessage());
        }

        return patientMapper.toResponseDTO(updatedPatient);
    }

    /**
     * Desactivar paciente (baja lógica)
     */
    @Transactional
    public void deactivatePatient(UUID patientId,
                                  String tenantCode,
                                  UUID userId,
                                  List<String> roles) {
        log.debug("Desactivando paciente ID: {} en tenant: {}", patientId, tenantCode);

        if (!hasAdminRole(roles)) {
            throw new UnauthorizedAccessException(
                    "Solo administradores pueden desactivar pacientes");
        }

        Patient patient = patientRepository.findByIdAndTenantCode(patientId, tenantCode)
                .orElseThrow(() -> new PatientNotFoundException(patientId, tenantCode));

        patient.setStatus(PatientStatus.INACTIVE);
        patientRepository.save(patient);

        log.info("Paciente desactivado: {}", patientId);

        PatientDeactivatedEvent event = PatientDeactivatedEvent.builder()
                .patientId(patientId)
                .tenantCode(tenantCode)
                .deactivatedByUserId(userId)
                .reason("Desactivación manual por administrador")
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishPatientDeactivated(event);
    }

    private void validateAccess(UUID patientId, String tenantCode, UUID userId, List<String> roles) {
        if (hasAdminRole(roles)) {
            return;
        }

        boolean hasAccess = patientDoctorRepository
                .existsByPatientIdAndDoctorUserIdAndTenantCode(patientId, userId, tenantCode);

        if (!hasAccess) {
            log.warn("Acceso denegado: usuario {} intentó acceder al paciente {}", userId, patientId);
            throw new UnauthorizedAccessException(userId, patientId);
        }
    }

    private boolean hasAdminRole(List<String> roles) {
        return roles.contains("TENANT_OWNER") ||
                roles.contains("TENANT_ADMIN_CONSULTORIO") ||
                roles.contains("ADMIN_PLATAFORMA");
    }

    @Transactional(readOnly = true)
    public long countActivePatients(String tenantCode) {
        return patientRepository.countByTenantCodeAndStatus(tenantCode, PatientStatus.ACTIVE);
    }
}
