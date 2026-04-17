package com.jcode.jclinical.infrastructure.adapter;

import com.jcode.jclinical.core.domain.model.Expediente;
import com.jcode.jclinical.core.port.out.ExpedienteRepositoryPort;
import com.jcode.jclinical.infrastructure.persistence.entity.ExpedienteEntity;
import com.jcode.jclinical.infrastructure.persistence.repository.SpringDataExpedienteRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ExpedienteJpaAdapter implements ExpedienteRepositoryPort {

    private final SpringDataExpedienteRepository repository;

    public ExpedienteJpaAdapter(SpringDataExpedienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public Expediente guardar(Expediente expediente) {
        ExpedienteEntity saved = repository.save(toEntity(expediente));
        return toDomain(saved);
    }

    @Override
    public Optional<Expediente> buscarPorId(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Expediente> buscarTodos() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void eliminarPorId(UUID id) {
        repository.deleteById(id);
    }

    private ExpedienteEntity toEntity(Expediente expediente) {
        return ExpedienteEntity.builder()
                .id(expediente.getId())
                .nombrePaciente(expediente.getNombrePaciente())
                .lienzoJson(expediente.getLienzoDinamicoJson())
                .fechaCreacion(expediente.getFechaCreacion())
                .fechaUltimaModificacion(expediente.getFechaUltimaModificacion())
                .build();
    }

    private Expediente toDomain(ExpedienteEntity entity) {
        return new Expediente(
                entity.getId(),
                entity.getNombrePaciente(),
                entity.getLienzoJson(),
                entity.getFechaCreacion(),
                entity.getFechaUltimaModificacion()
        );
    }
}
