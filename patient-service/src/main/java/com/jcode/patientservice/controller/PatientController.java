package com.jcode.patientservice.controller;

import com.jcode.patientservice.dto.PatientRequestDTO;
import com.jcode.patientservice.dto.PatientResponseDTO;
import com.jcode.patientservice.dto.PatientSummaryDTO;
import com.jcode.patientservice.dto.PatientUpdateDTO;
import com.jcode.patientservice.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Valid @RequestBody PatientRequestDTO requestDTO,
            HttpServletRequest httpRequest) {

        UUID userId = (UUID) httpRequest.getAttribute("X-User-Id");
        String tenantCode = (String) httpRequest.getAttribute("X-Tenant-Code");

        log.info("POST /api/patients - Creando paciente en tenant: {} por usuario: {}",
                tenantCode, userId);

        PatientResponseDTO response = patientService.createPatient(requestDTO, tenantCode, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> getPatientById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID userId = (UUID) httpRequest.getAttribute("X-User-Id");
        String tenantCode = (String) httpRequest.getAttribute("X-Tenant-Code");
        String rolesHeader = (String) httpRequest.getAttribute("X-Roles");
        List<String> roles = rolesHeader != null && !rolesHeader.isBlank()
                ? List.of(rolesHeader.split(","))
                : List.of();

        log.info("GET /api/patients/{} - tenant: {} usuario: {}", id, tenantCode, userId);

        PatientResponseDTO response = patientService.getPatientById(id, tenantCode, userId, roles);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PatientSummaryDTO>> getAllPatients(HttpServletRequest httpRequest) {

        UUID userId = (UUID) httpRequest.getAttribute("X-User-Id");
        String tenantCode = (String) httpRequest.getAttribute("X-Tenant-Code");
        String rolesHeader = (String) httpRequest.getAttribute("X-Roles");
        List<String> roles = rolesHeader != null && !rolesHeader.isBlank()
                ? List.of(rolesHeader.split(","))
                : List.of();

        log.info("GET /api/patients - tenant: {} usuario: {}", tenantCode, userId);

        List<PatientSummaryDTO> patients = patientService.getAllPatients(tenantCode, userId, roles);
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PatientSummaryDTO>> searchPatients(
            @RequestParam String term,
            HttpServletRequest httpRequest) {

        UUID userId = (UUID) httpRequest.getAttribute("X-User-Id");
        String tenantCode = (String) httpRequest.getAttribute("X-Tenant-Code");
        String rolesHeader = (String) httpRequest.getAttribute("X-Roles");
        List<String> roles = rolesHeader != null && !rolesHeader.isBlank()
                ? List.of(rolesHeader.split(","))
                : List.of();

        log.info("GET /api/patients/search?term={} - tenant: {}", term, tenantCode);

        List<PatientSummaryDTO> patients =
                patientService.searchPatientsByName(term, tenantCode, userId, roles);

        return ResponseEntity.ok(patients);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody PatientUpdateDTO updateDTO,
            HttpServletRequest httpRequest) {

        UUID userId = (UUID) httpRequest.getAttribute("X-User-Id");
        String tenantCode = (String) httpRequest.getAttribute("X-Tenant-Code");
        String rolesHeader = (String) httpRequest.getAttribute("X-Roles");
        List<String> roles = rolesHeader != null && !rolesHeader.isBlank()
                ? List.of(rolesHeader.split(","))
                : List.of();

        log.info("PUT /api/patients/{} - tenant: {} usuario: {}", id, tenantCode, userId);

        PatientResponseDTO response =
                patientService.updatePatient(id, updateDTO, tenantCode, userId, roles);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePatient(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID userId = (UUID) httpRequest.getAttribute("X-User-Id");
        String tenantCode = (String) httpRequest.getAttribute("X-Tenant-Code");
        String rolesHeader = (String) httpRequest.getAttribute("X-Roles");
        List<String> roles = rolesHeader != null && !rolesHeader.isBlank()
                ? List.of(rolesHeader.split(","))
                : List.of();

        log.info("PATCH /api/patients/{}/deactivate - tenant: {} usuario: {}", id, tenantCode, userId);

        patientService.deactivatePatient(id, tenantCode, userId, roles);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/count")
    public ResponseEntity<Long> countActivePatients(HttpServletRequest httpRequest) {

        String tenantCode = (String) httpRequest.getAttribute("X-Tenant-Code");
        log.info("GET /api/patients/stats/count - tenant: {}", tenantCode);

        long count = patientService.countActivePatients(tenantCode);
        return ResponseEntity.ok(count);
    }
}
