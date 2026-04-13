package com.jcode.jclinical.core.port.in;

import com.jcode.jclinical.core.domain.model.Expediente;

import java.util.List;
import java.util.UUID;

public interface GestionarExpedienteUseCase {
    Expediente crearExpediente(String nombrePaciente, String jsonInicial);
    Expediente obtenerExpediente(UUID expedienteId);
    Expediente actualizarExpediente(UUID expedienteId, String nombrePaciente, String nuevoJson);
    List<Expediente> obtenerTodosLosExpedientes();
    void eliminarExpediente(UUID expedienteId);
}
