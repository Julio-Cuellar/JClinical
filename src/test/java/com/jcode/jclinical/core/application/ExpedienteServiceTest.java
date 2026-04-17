package com.jcode.jclinical.core.application;

import com.jcode.jclinical.core.domain.model.Expediente;
import com.jcode.jclinical.core.port.out.ExpedienteRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpedienteServiceTest {

    @Test
    void debeActualizarNombreYLienzoEnUnaSolaOperacion() {
        InMemoryExpedienteRepository repository = new InMemoryExpedienteRepository();
        ExpedienteService service = new ExpedienteService(repository);
        Expediente creado = service.crearExpediente("Juan Pérez", "{\"elementos\":[]}");

        Expediente actualizado = service.actualizarExpediente(creado.getId(), "Ana López", "{\"paginas\":[]}");

        assertEquals("Ana López", actualizado.getNombrePaciente());
        assertEquals("{\"paginas\":[]}", actualizado.getLienzoDinamicoJson());
    }

    private static class InMemoryExpedienteRepository implements ExpedienteRepositoryPort {
        private final List<Expediente> expedientes = new ArrayList<>();

        @Override
        public Expediente guardar(Expediente expediente) {
            expedientes.removeIf(e -> e.getId().equals(expediente.getId()));
            expedientes.add(expediente);
            return expediente;
        }

        @Override
        public Optional<Expediente> buscarPorId(UUID id) {
            return expedientes.stream().filter(e -> e.getId().equals(id)).findFirst();
        }

        @Override
        public List<Expediente> buscarTodos() {
            return List.copyOf(expedientes);
        }

        @Override
        public void eliminarPorId(UUID id) {
            expedientes.removeIf(e -> e.getId().equals(id));
        }
    }
}
