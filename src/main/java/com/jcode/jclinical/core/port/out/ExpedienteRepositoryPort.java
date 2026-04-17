package com.jcode.jclinical.core.port.out;

import com.jcode.jclinical.core.domain.model.Expediente;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpedienteRepositoryPort {
    Expediente guardar(Expediente expediente);
    Optional<Expediente> buscarPorId(UUID id);
    List<Expediente> buscarTodos();
    void eliminarPorId(UUID id);
}