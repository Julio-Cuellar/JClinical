package com.jcode.jclinical.infrastructure.adapter;

import com.jcode.jclinical.core.domain.model.Plantilla;
import com.jcode.jclinical.core.port.out.PlantillaRepositoryPort;
import com.jcode.jclinical.infrastructure.persistence.entity.PlantillaEntity;
import com.jcode.jclinical.infrastructure.persistence.repository.SpringDataPlantillaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PlantillaJpaAdapter implements PlantillaRepositoryPort {

    private final SpringDataPlantillaRepository repository;

    public PlantillaJpaAdapter(SpringDataPlantillaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Plantilla guardar(Plantilla plantilla) {
        PlantillaEntity saved = repository.save(toEntity(plantilla));
        return toDomain(saved);
    }

    @Override
    public List<Plantilla> buscarTodas() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Plantilla> buscarPorId(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public void eliminar(UUID id) {
        repository.deleteById(id);
    }

    private PlantillaEntity toEntity(Plantilla plantilla) {
        return PlantillaEntity.builder()
                .id(plantilla.getId())
                .nombre(plantilla.getNombre())
                .layoutJson(plantilla.getLayoutJson())
                .build();
    }

    private Plantilla toDomain(PlantillaEntity entity) {
        return new Plantilla(
                entity.getId(),
                entity.getNombre(),
                entity.getLayoutJson()
        );
    }
}
