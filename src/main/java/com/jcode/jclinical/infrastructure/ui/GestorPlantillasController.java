package com.jcode.jclinical.infrastructure.ui;

import com.jcode.jclinical.core.domain.model.Plantilla;
import com.jcode.jclinical.core.port.in.GestionarPlantillaUseCase;
import com.jcode.jclinical.infrastructure.ui.support.SceneNavigator;
import com.jcode.jclinical.infrastructure.ui.support.UiDialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class GestorPlantillasController {

    private final GestionarPlantillaUseCase gestionarPlantillaUseCase;
    private final SceneNavigator sceneNavigator;

    @FXML private ListView<Plantilla> listViewPlantillas;
    @FXML private Button btnCrear, btnEditar, btnDuplicar, btnEliminar, btnVolver;

    public GestorPlantillasController(GestionarPlantillaUseCase gestionarPlantillaUseCase, ApplicationContext springContext) {
        this.gestionarPlantillaUseCase = gestionarPlantillaUseCase;
        this.sceneNavigator = new SceneNavigator(springContext);
    }

    @FXML
    public void initialize() {
        cargarLista();

        listViewPlantillas.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Plantilla item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText("📑 " + item.getNombre());
                    setStyle("-fx-font-size: 16px; -fx-padding: 12px; -fx-border-color: #E0ECEF; -fx-border-width: 0 0 1 0;");
                }
            }
        });

        btnCrear.setOnAction(e -> crearNuevaPlantilla());
        btnEditar.setOnAction(e -> editarPlantillaSeleccionada());
        btnDuplicar.setOnAction(e -> duplicarPlantillaSeleccionada());
        btnEliminar.setOnAction(e -> eliminarPlantillaSeleccionada());
        btnVolver.setOnAction(e -> volverAlDashboard());

        listViewPlantillas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && listViewPlantillas.getSelectionModel().getSelectedItem() != null) {
                editarPlantillaSeleccionada();
            }
        });
    }

    private void cargarLista() {
        listViewPlantillas.getItems().setAll(gestionarPlantillaUseCase.obtenerTodasLasPlantillas());
    }

    private void crearNuevaPlantilla() {
        TextInputDialog dialog = new TextInputDialog("Nueva Plantilla Base");
        dialog.setTitle("Crear plantilla");
        dialog.setHeaderText("Ingresa el nombre para la nueva plantilla");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombre -> abrirLienzoEnModoPlantilla(null, nombre));
    }

    private void editarPlantillaSeleccionada() {
        Plantilla seleccionada = listViewPlantillas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            abrirLienzoEnModoPlantilla(seleccionada, null);
        }
    }

    private void duplicarPlantillaSeleccionada() {
        Plantilla seleccionada = listViewPlantillas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog(seleccionada.getNombre() + " (Copia)");
        dialog.setTitle("Duplicar plantilla");
        dialog.setHeaderText("Ingresa el nombre de la copia");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nombre -> {
            gestionarPlantillaUseCase.duplicarPlantilla(seleccionada.getId(), nombre);
            cargarLista();
        });
    }

    private void eliminarPlantillaSeleccionada() {
        Plantilla seleccionada = listViewPlantillas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }
        boolean confirmed = UiDialogs.confirm(getStage(), "Eliminar plantilla",
                "¿Seguro que deseas eliminar la plantilla '" + seleccionada.getNombre() + "'?");
        if (!confirmed) {
            return;
        }
        gestionarPlantillaUseCase.eliminarPlantilla(seleccionada.getId());
        cargarLista();
    }

    private void abrirLienzoEnModoPlantilla(Plantilla plantilla, String nuevoNombre) {
        try {
            LienzoDinamicoController controller = sceneNavigator.navigate(getStage(), "/fxml/lienzo_dinamico.fxml", 1150, 800);
            if (plantilla != null) {
                controller.cargarPlantillaExistente(plantilla);
            } else if (nuevoNombre != null) {
                controller.configurarComoNuevaPlantilla(nuevoNombre);
            }
        } catch (Exception e) {
            UiDialogs.error(getStage(), "Error al abrir plantilla", "No se pudo abrir el editor de plantillas.");
        }
    }

    private void volverAlDashboard() {
        try {
            sceneNavigator.navigate(getStage(), "/fxml/lista_expedientes.fxml", 900, 600);
        } catch (Exception e) {
            UiDialogs.error(getStage(), "Error de navegación", "No se pudo volver al dashboard.");
        }
    }

    private Stage getStage() {
        return (Stage) btnVolver.getScene().getWindow();
    }
}
