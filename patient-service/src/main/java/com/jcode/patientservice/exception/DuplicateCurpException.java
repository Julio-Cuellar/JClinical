package com.jcode.patientservice.exception;

public class DuplicateCurpException extends RuntimeException {

    public DuplicateCurpException(String curp) {
        super("Ya existe un paciente registrado con el CURP: " + curp);
    }
}
