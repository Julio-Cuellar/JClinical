package com.jcode.patientservice.controller;

import com.jcode.patientservice.dto.PatientRequestDTO;
import com.jcode.patientservice.dto.PatientResponseDTO;
import com.jcode.patientservice.dto.PatientSummaryDTO;
import com.jcode.patientservice.dto.PatientUpdateDTO;
import com.jcode.patientservice.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;

    /**
     * Crear un nuevo paciente
     * POST /api/patients
     */
    @PostMapping
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Valid @RequestBody PatientRequestDTO requestDTO,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("POST /api/patients - Creando paciente en tenant: {}", tenantId);

        PatientResponseDTO response = patientService.createPatient(requestDTO, tenantId, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener paciente por ID
     * GET /api/patients/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> getPatientById(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Roles") String rolesHeader) {

        log.info("GET /api/patients/{} - tenant: {}", id, tenantId);

        List<String> roles = List.of(rolesHeader.split(","));
        PatientResponseDTO response = patientService.getPatientById(id, tenantId, userId, roles);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener todos los pacientes (según rol del usuario)
     * GET /api/patients
     */
    @GetMapping
    public ResponseEntity<List<PatientSummaryDTO>> getAllPatients(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Roles") String rolesHeader) {

        log.info("GET /api/patients - tenant: {} usuario: {}", tenantId, userId);

        List<String> roles = List.of(rolesHeader.split(","));
        List<PatientSummaryDTO> patients = patientService.getAllPatients(tenantId, userId, roles);

        return ResponseEntity.ok(patients);
    }

    /**
     * Buscar pacientes por nombre
     * GET /api/patients/search?term=Juan
     */
    @GetMapping("/search")
    public ResponseEntity<List<PatientSummaryDTO>> searchPatients(
            @RequestParam String term,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Roles") String rolesHeader) {

        log.info("GET /api/patients/search?term={} - tenant: {}", term, tenantId);

        List<String> roles = List.of(rolesHeader.split(","));
        List<PatientSummaryDTO> patients = patientService.searchPatientsByName(term, tenantId, userId, roles);

        return ResponseEntity.ok(patients);
    }

    /**
     * Actualizar datos del paciente
     * PUT /api/patients/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody PatientUpdateDTO updateDTO,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Roles") String rolesHeader) {

        log.info("PUT /api/patients/{} - tenant: {}", id, tenantId);

        List<String> roles = List.of(rolesHeader.split(","));
        PatientResponseDTO response = patientService.updatePatient(id, updateDTO, tenantId, userId, roles);

        return ResponseEntity.ok(response);
    }

    /**
     * Desactivar paciente (baja lógica)
     * PATCH /api/patients/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePatient(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Roles") String rolesHeader) {

        log.info("PATCH /api/patients/{}/deactivate - tenant: {}", id, tenantId);

        List<String> roles = List.of(rolesHeader.split(","));
        patientService.deactivatePatient(id, tenantId, userId, roles);

        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener estadísticas de pacientes
     * GET /api/patients/stats/count
     */
    @GetMapping("/stats/count")
    public ResponseEntity<Long> countActivePatients(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {

        log.info("GET /api/patients/stats/count - tenant: {}", tenantId);

        long count = patientService.countActivePatients(tenantId);

        return ResponseEntity.ok(count);
    }
}
