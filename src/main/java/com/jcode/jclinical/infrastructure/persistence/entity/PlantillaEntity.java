package com.jcode.jclinical.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "plantillas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlantillaEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String layoutJson;
}