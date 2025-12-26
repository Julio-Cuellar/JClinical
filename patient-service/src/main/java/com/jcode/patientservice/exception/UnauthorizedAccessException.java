package com.jcode.patientservice.exception;

import java.util.UUID;

public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(UUID userId, UUID patientId) {
        super("El usuario " + userId + " no tiene acceso al paciente " + patientId);
    }
}
