package com.jcode.jclinical.infrastructure.ui;

import com.jcode.jclinical.core.domain.model.Expediente;
import com.jcode.jclinical.core.domain.model.Plantilla;
import com.jcode.jclinical.core.port.in.GestionarExpedienteUseCase;
import com.jcode.jclinical.core.port.in.GestionarPlantillaUseCase;
import com.jcode.jclinical.infrastructure.ui.support.SceneNavigator;
import com.jcode.jclinical.infrastructure.ui.support.UiDialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@Scope("prototype")
public class ListaExpedientesController {

    private final GestionarExpedienteUseCase gestionarExpedienteUseCase;
    private final GestionarPlantillaUseCase gestionarPlantillaUseCase;
    private final SceneNavigator sceneNavigator;

    @FXML private ListView<Expediente> listViewExpedientes;
    @FXML private Button btnNuevo;
    @FXML private Button btnGestorPlantillas;

    public ListaExpedientesController(GestionarExpedienteUseCase gestionarExpedienteUseCase,
                                      GestionarPlantillaUseCase gestionarPlantillaUseCase,
                                      ApplicationContext springContext) {
        this.gestionarExpedienteUseCase = gestionarExpedienteUseCase;
        this.gestionarPlantillaUseCase = gestionarPlantillaUseCase;
        this.sceneNavigator = new SceneNavigator(springContext);
    }

    @FXML
    public void initialize() {
        cargarLista();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        listViewExpedientes.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Expediente exp, boolean empty) {
                super.updateItem(exp, empty);
                if (empty || exp == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText("Paciente: " + exp.getNombrePaciente()
                            + "  |  Creado: " + exp.getFechaCreacion().format(formatter)
                            + "  |  Última modificación: " + exp.getFechaUltimaModificacion().format(formatter));
                    setStyle("-fx-font-size: 14px; -fx-padding: 12px; -fx-border-color: #E0ECEF; -fx-border-width: 0 0 1 0;");
                }
            }
        });

        btnNuevo.setOnAction(e -> mostrarSeleccionPlantilla());
        if (btnGestorPlantillas != null) {
            btnGestorPlantillas.setOnAction(e -> abrirGestorPlantillas());
        }

        listViewExpedientes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && listViewExpedientes.getSelectionModel().getSelectedItem() != null) {
                abrirLienzoParaEdicion(listViewExpedientes.getSelectionModel().getSelectedItem());
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemEliminar = new MenuItem("Eliminar expediente");
        itemEliminar.setOnAction(e -> eliminarSeleccionado());
        contextMenu.getItems().add(itemEliminar);
        listViewExpedientes.setContextMenu(contextMenu);
    }

    private void mostrarSeleccionPlantilla() {
        List<Plantilla> plantillas = gestionarPlantillaUseCase.obtenerTodasLasPlantillas();

        ChoiceDialog<Object> dialog = new ChoiceDialog<>();
        dialog.getItems().add("Lienzo en Blanco");
        dialog.getItems().addAll(plantillas.stream().map(Plantilla::getNombre).toList());
        dialog.setSelectedItem("Lienzo en Blanco");
        dialog.setTitle("Nuevo expediente");
        dialog.setHeaderText("Selecciona una base para el expediente");

        Optional<Object> result = dialog.showAndWait();
        result.ifPresent(seleccion -> {
            if (seleccion.equals("Lienzo en Blanco")) {
                abrirLienzoNuevo(null);
            } else {
                Plantilla p = plantillas.stream()
                        .filter(pl -> pl.getNombre().equals(seleccion))
                        .findFirst().orElse(null);
                abrirLienzoNuevo(p != null ? p.getLayoutJson() : null);
            }
        });
    }

    private void cargarLista() {
        listViewExpedientes.getItems().setAll(gestionarExpedienteUseCase.obtenerTodosLosExpedientes());
    }

    private void abrirLienzoParaEdicion(Expediente expediente) {
        try {
            LienzoDinamicoController controller = sceneNavigator.navigate(getStage(), "/fxml/lienzo_dinamico.fxml", 1150, 800);
            controller.cargarExpedienteExistente(expediente);
        } catch (Exception e) {
            UiDialogs.error(getStage(), "Error al abrir expediente", "No se pudo cargar el expediente seleccionado.");
        }
    }

    private void abrirLienzoNuevo(String jsonPlantilla) {
        try {
            LienzoDinamicoController controller = sceneNavigator.navigate(getStage(), "/fxml/lienzo_dinamico.fxml", 1150, 800);
            controller.prepararLienzoNuevo(jsonPlantilla);
        } catch (Exception e) {
            UiDialogs.error(getStage(), "Error al abrir el editor", "No se pudo abrir el lienzo.");
        }
    }

    private void abrirGestorPlantillas() {
        try {
            sceneNavigator.navigate(getStage(), "/fxml/gestor_plantillas.fxml", 900, 600);
        } catch (Exception e) {
            UiDialogs.error(getStage(), "Error al abrir plantillas", "No se pudo abrir el gestor de plantillas.");
        }
    }

    private void eliminarSeleccionado() {
        Expediente seleccionado = listViewExpedientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        boolean confirmed = UiDialogs.confirm(getStage(), "Eliminar expediente",
                "¿Seguro que deseas eliminar el expediente de " + seleccionado.getNombrePaciente() + "?");
        if (!confirmed) {
            return;
        }
        gestionarExpedienteUseCase.eliminarExpediente(seleccionado.getId());
        cargarLista();
    }

    private Stage getStage() {
        return (Stage) btnNuevo.getScene().getWindow();
    }
}
