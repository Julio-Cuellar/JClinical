package com.jcode.patientservice.exception;

import java.util.UUID;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(UUID patientId) {
        super("Paciente no encontrado con ID: " + patientId);
    }

    public PatientNotFoundException(UUID patientId, String tenantCode) {
        super("Paciente con ID: " + patientId + " no encontrado en el tenant: " + tenantCode);
    }

    public PatientNotFoundException(String message) {
        super(message);
    }
}
