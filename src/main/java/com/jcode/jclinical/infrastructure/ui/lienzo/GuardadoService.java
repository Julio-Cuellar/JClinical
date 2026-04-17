package com.jcode.jclinical.infrastructure.ui.lienzo;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jcode.jclinical.core.domain.model.Expediente;
import com.jcode.jclinical.core.domain.model.Plantilla;
import com.jcode.jclinical.core.port.in.GestionarExpedienteUseCase;
import com.jcode.jclinical.core.port.in.GestionarPlantillaUseCase;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.util.List;
import java.util.Optional;

public class GuardadoService {

    private final GestionarExpedienteUseCase expedienteUseCase;
    private final GestionarPlantillaUseCase plantillaUseCase;
    private final SerializacionService serializacionService;
    private Label lblEstado;

    public GuardadoService(GestionarExpedienteUseCase expedienteUseCase,
                           GestionarPlantillaUseCase plantillaUseCase,
                           SerializacionService serializacionService) {
        this.expedienteUseCase = expedienteUseCase;
        this.plantillaUseCase = plantillaUseCase;
        this.serializacionService = serializacionService;
    }

    public void setLblEstado(Label lblEstado) {
        this.lblEstado = lblEstado;
    }

    public Expediente guardarExpediente(Expediente expedienteActual,
                                        List<ArrayNode> paginasData,
                                        String nombrePaciente) {
        try {
            String jsonFinal = serializacionService.construirJsonFinal(paginasData);
            String nombreFinal = normalizarNombre(nombrePaciente, "Sin Nombre");

            Expediente resultado = expedienteActual == null
                    ? expedienteUseCase.crearExpediente(nombreFinal, jsonFinal)
                    : expedienteUseCase.actualizarExpediente(expedienteActual.getId(), nombreFinal, jsonFinal);

            mostrarExito(expedienteActual == null ? "¡Expediente creado!" : "¡Expediente actualizado!");
            return resultado;
        } catch (Exception e) {
            mostrarError("Error crítico al guardar expediente");
            throw new IllegalStateException("No se pudo guardar el expediente.", e);
        }
    }

    public Plantilla guardarPlantilla(Plantilla plantillaActual,
                                      List<ArrayNode> paginasData,
                                      String nombre) {
        try {
            String jsonFinal = serializacionService.construirJsonFinal(paginasData);
            String nombreFinal = normalizarNombre(nombre, "Plantilla Sin Nombre");

            Plantilla resultado = plantillaActual == null
                    ? plantillaUseCase.crearPlantilla(nombreFinal, jsonFinal)
                    : plantillaUseCase.actualizarPlantilla(plantillaActual.getId(), nombreFinal, jsonFinal);

            mostrarExito(plantillaActual == null ? "¡Plantilla guardada!" : "¡Plantilla actualizada!");
            return resultado;
        } catch (Exception e) {
            mostrarError("Error crítico al guardar plantilla");
            throw new IllegalStateException("No se pudo guardar la plantilla.", e);
        }
    }

    public void guardarComoPlantillaNueva(List<ArrayNode> paginasData) {
        TextInputDialog dialog = new TextInputDialog("Plantilla Nueva");
        dialog.setTitle("Nueva Plantilla Base");
        dialog.setHeaderText("Ingresa el nombre para la nueva plantilla");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(nombre -> {
            try {
                String jsonFinal = serializacionService.construirJsonFinal(paginasData);
                plantillaUseCase.crearPlantilla(nombre, jsonFinal);
                mostrarExito("¡Plantilla base creada!");
            } catch (Exception e) {
                mostrarError("Error al crear la plantilla base");
                throw new IllegalStateException("No se pudo crear la plantilla base.", e);
            }
        });
    }

    private String normalizarNombre(String nombre, String fallback) {
        return (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : fallback;
    }

    private void mostrarExito(String mensaje) {
        if (lblEstado != null) {
            lblEstado.setText(mensaje);
            lblEstado.setStyle("-fx-text-fill: #4A8A9C; -fx-font-weight: bold;");
        }
    }

    private void mostrarError(String mensaje) {
        if (lblEstado != null) {
            lblEstado.setText(mensaje);
            lblEstado.setStyle("-fx-text-fill: red;");
        }
    }
}
