package com.jcode.jclinical.core.application;

import com.jcode.jclinical.core.domain.exception.RecursoNoEncontradoException;
import com.jcode.jclinical.core.domain.model.Plantilla;
import com.jcode.jclinical.core.port.in.GestionarPlantillaUseCase;
import com.jcode.jclinical.core.port.out.PlantillaRepositoryPort;

import java.util.List;
import java.util.UUID;

public class PlantillaService implements GestionarPlantillaUseCase {
    private final PlantillaRepositoryPort repositoryPort;

    public PlantillaService(PlantillaRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public Plantilla crearPlantilla(String nombre, String layoutJson) {
        return repositoryPort.guardar(new Plantilla(nombre, layoutJson));
    }

    @Override
    public List<Plantilla> obtenerTodasLasPlantillas() {
        return repositoryPort.buscarTodas();
    }

    @Override
    public void eliminarPlantilla(UUID id) {
        repositoryPort.eliminar(id);
    }

    @Override
    public Plantilla duplicarPlantilla(UUID id, String nuevoNombre) {
        Plantilla original = repositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla no encontrada."));
        return repositoryPort.guardar(new Plantilla(nuevoNombre, original.getLayoutJson()));
    }

    @Override
    public Plantilla actualizarPlantilla(UUID id, String nuevoNombre, String nuevoJson) {
        Plantilla plantilla = repositoryPort.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla no encontrada."));
        plantilla.actualizarContenido(nuevoNombre, nuevoJson);
        return repositoryPort.guardar(plantilla);
    }
}
