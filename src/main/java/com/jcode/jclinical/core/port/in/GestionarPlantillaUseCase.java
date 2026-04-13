package com.jcode.jclinical.core.port.in;

import com.jcode.jclinical.core.domain.model.Plantilla;
import java.util.List;
import java.util.UUID;

public interface GestionarPlantillaUseCase {
    Plantilla crearPlantilla(String nombre, String layoutJson);
    List<Plantilla> obtenerTodasLasPlantillas();
    void eliminarPlantilla(UUID id);
    Plantilla duplicarPlantilla(UUID id, String nuevoNombre);
    Plantilla actualizarPlantilla(UUID id, String nuevoNombre, String nuevoJson);
}