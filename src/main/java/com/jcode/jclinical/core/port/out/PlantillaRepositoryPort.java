package com.jcode.jclinical.core.port.out;

import com.jcode.jclinical.core.domain.model.Plantilla;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantillaRepositoryPort {
    Plantilla guardar(Plantilla plantilla);
    List<Plantilla> buscarTodas();
    Optional<Plantilla> buscarPorId(UUID id);
    void eliminar(UUID id);
}