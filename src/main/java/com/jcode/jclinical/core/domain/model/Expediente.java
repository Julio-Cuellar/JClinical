package com.jcode.jclinical.core.domain.model;

import com.jcode.jclinical.core.domain.exception.ExpedienteInvalidoException;

import java.time.LocalDateTime;
import java.util.UUID;

public class Expediente {

    private final UUID id;
    private String nombrePaciente;
    private String lienzoDinamicoJson;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimaModificacion;

    public Expediente(String nombrePaciente, String lienzoDinamicoJson) {
        validarNombrePaciente(nombrePaciente);
        this.id = UUID.randomUUID();
        this.nombrePaciente = nombrePaciente.trim();
        this.fechaCreacion = LocalDateTime.now();
        actualizarLienzo(lienzoDinamicoJson);
    }

    public Expediente(UUID id, String nombrePaciente, String lienzoDinamicoJson,
                      LocalDateTime fechaCreacion, LocalDateTime fechaUltimaModificacion) {
        if (id == null) {
            throw new ExpedienteInvalidoException("El identificador del expediente es obligatorio.");
        }
        validarNombrePaciente(nombrePaciente);
        validarLienzo(lienzoDinamicoJson);
        this.id = id;
        this.nombrePaciente = nombrePaciente.trim();
        this.lienzoDinamicoJson = lienzoDinamicoJson;
        this.fechaCreacion = fechaCreacion == null ? LocalDateTime.now() : fechaCreacion;
        this.fechaUltimaModificacion = fechaUltimaModificacion == null ? this.fechaCreacion : fechaUltimaModificacion;
    }

    public void actualizarNombrePaciente(String nuevoNombrePaciente) {
        validarNombrePaciente(nuevoNombrePaciente);
        this.nombrePaciente = nuevoNombrePaciente.trim();
        this.fechaUltimaModificacion = LocalDateTime.now();
    }

    public void actualizarLienzo(String nuevoLienzoJson) {
        validarLienzo(nuevoLienzoJson);
        this.lienzoDinamicoJson = nuevoLienzoJson;
        this.fechaUltimaModificacion = LocalDateTime.now();
    }

    public void actualizarContenido(String nuevoNombrePaciente, String nuevoLienzoJson) {
        validarNombrePaciente(nuevoNombrePaciente);
        validarLienzo(nuevoLienzoJson);
        this.nombrePaciente = nuevoNombrePaciente.trim();
        this.lienzoDinamicoJson = nuevoLienzoJson;
        this.fechaUltimaModificacion = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNombrePaciente() { return nombrePaciente; }
    public String getLienzoDinamicoJson() { return lienzoDinamicoJson; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaUltimaModificacion() { return fechaUltimaModificacion; }

    private void validarNombrePaciente(String nombrePaciente) {
        if (nombrePaciente == null || nombrePaciente.trim().isEmpty()) {
            throw new ExpedienteInvalidoException("El nombre del paciente es obligatorio.");
        }
    }

    private void validarLienzo(String lienzoJson) {
        if (lienzoJson == null || lienzoJson.trim().isEmpty()) {
            throw new ExpedienteInvalidoException("El lienzo no puede estar vacío.");
        }
    }
}
