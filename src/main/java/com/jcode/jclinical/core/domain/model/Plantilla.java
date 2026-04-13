package com.jcode.jclinical.core.domain.model;

import com.jcode.jclinical.core.domain.exception.PlantillaInvalidaException;

import java.util.UUID;

public class
Plantilla {
    private final UUID id;
    private String nombre;
    private String layoutJson;

    public Plantilla(String nombre, String layoutJson) {
        validarNombre(nombre);
        validarLayout(layoutJson);
        this.id = UUID.randomUUID();
        this.nombre = nombre.trim();
        this.layoutJson = layoutJson;
    }

    public Plantilla(UUID id, String nombre, String layoutJson) {
        if (id == null) {
            throw new PlantillaInvalidaException("El identificador de la plantilla es obligatorio.");
        }
        validarNombre(nombre);
        validarLayout(layoutJson);
        this.id = id;
        this.nombre = nombre.trim();
        this.layoutJson = layoutJson;
    }

    public void actualizarNombre(String nuevoNombre) {
        validarNombre(nuevoNombre);
        this.nombre = nuevoNombre.trim();
    }

    public void actualizarLayout(String nuevoLayoutJson) {
        validarLayout(nuevoLayoutJson);
        this.layoutJson = nuevoLayoutJson;
    }

    public void actualizarContenido(String nuevoNombre, String nuevoLayoutJson) {
        validarNombre(nuevoNombre);
        validarLayout(nuevoLayoutJson);
        this.nombre = nuevoNombre.trim();
        this.layoutJson = nuevoLayoutJson;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getLayoutJson() { return layoutJson; }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new PlantillaInvalidaException("El nombre de la plantilla es obligatorio.");
        }
    }

    private void validarLayout(String layoutJson) {
        if (layoutJson == null || layoutJson.trim().isEmpty()) {
            throw new PlantillaInvalidaException("El layout de la plantilla no puede estar vacío.");
        }
    }
}
