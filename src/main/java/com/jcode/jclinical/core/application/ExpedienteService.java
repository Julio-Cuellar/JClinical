package com.jcode.jclinical.core.application;

import com.jcode.jclinical.core.domain.exception.RecursoNoEncontradoException;
import com.jcode.jclinical.core.domain.model.Expediente;
import com.jcode.jclinical.core.port.in.GestionarExpedienteUseCase;
import com.jcode.jclinical.core.port.out.ExpedienteRepositoryPort;

import java.util.List;
import java.util.UUID;

public class ExpedienteService implements GestionarExpedienteUseCase {

    private final ExpedienteRepositoryPort expedienteRepositoryPort;

    public ExpedienteService(ExpedienteRepositoryPort expedienteRepositoryPort) {
        this.expedienteRepositoryPort = expedienteRepositoryPort;
    }

    @Override
    public Expediente crearExpediente(String nombrePaciente, String jsonInicial) {
        Expediente nuevoExpediente = new Expediente(nombrePaciente, jsonInicial);
        return expedienteRepositoryPort.guardar(nuevoExpediente);
    }

    @Override
    public Expediente obtenerExpediente(UUID expedienteId) {
        return expedienteRepositoryPort.buscarPorId(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el expediente."));
    }

    @Override
    public Expediente actualizarExpediente(UUID expedienteId, String nombrePaciente, String nuevoJson) {
        Expediente expediente = obtenerExpediente(expedienteId);
        expediente.actualizarContenido(nombrePaciente, nuevoJson);
        return expedienteRepositoryPort.guardar(expediente);
    }

    @Override
    public List<Expediente> obtenerTodosLosExpedientes() {
        return expedienteRepositoryPort.buscarTodos();
    }

    @Override
    public void eliminarExpediente(UUID expedienteId) {
        expedienteRepositoryPort.eliminarPorId(expedienteId);
    }
}
