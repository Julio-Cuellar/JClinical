package com.jcode.patientservice.exception;

import java.util.UUID;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(UUID patientId) {
        super("Paciente no encontrado con ID: " + patientId);
    }

    public PatientNotFoundException(UUID patientId, UUID tenantId) {
        super("Paciente con ID: " + patientId + " no encontrado en el tenant: " + tenantId);
    }

    public PatientNotFoundException(String message) {
        super(message);
    }
}
