package com.jcode.jclinical.infrastructure.ui;

import com.jcode.jclinical.core.domain.model.Expediente;
import com.jcode.jclinical.core.domain.model.Plantilla;
import com.jcode.jclinical.core.port.in.GestionarExpedienteUseCase;
import com.jcode.jclinical.core.port.in.GestionarPlantillaUseCase;
import com.jcode.jclinical.infrastructure.ui.lienzo.ComponenteFactory;
import com.jcode.jclinical.infrastructure.ui.lienzo.GuardadoService;
import com.jcode.jclinical.infrastructure.ui.lienzo.NodoInteraccionService;
import com.jcode.jclinical.infrastructure.ui.lienzo.PaginacionManager;
import com.jcode.jclinical.infrastructure.ui.lienzo.SerializacionService;
import com.jcode.jclinical.infrastructure.ui.support.SceneNavigator;
import com.jcode.jclinical.infrastructure.ui.support.UiDialogs;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class LienzoDinamicoController {

    private final GestionarExpedienteUseCase expedienteUseCase;
    private final GestionarPlantillaUseCase plantillaUseCase;
    private final SceneNavigator sceneNavigator;

    private Expediente expedienteCargado;
    private Plantilla plantillaCargada;
    private boolean isModoPlantilla = false;

    private SerializacionService serializacionService;
    private NodoInteraccionService nodoInteraccionService;
    private ComponenteFactory componenteFactory;
    private PaginacionManager paginacionManager;
    private GuardadoService guardadoService;

    @FXML private Pane lienzoCarta;
    @FXML private Label toolTitulo, toolTextoCorto, toolImagen, toolTabla, toolOdontograma;
    @FXML private Label toolAntecedentes, toolContacto;
    @FXML private Button btnGuardar, btnVolver;
    @FXML private Button btnPaginaPrevia, btnPaginaSiguiente, btnAgregarPagina;
    @FXML private Label lblEstado, lblContadorPaginas;
    @FXML private TextField txtNombreExpediente;

    public LienzoDinamicoController(GestionarExpedienteUseCase expedienteUseCase,
                                    GestionarPlantillaUseCase plantillaUseCase,
                                    ApplicationContext springContext) {
        this.expedienteUseCase = expedienteUseCase;
        this.plantillaUseCase = plantillaUseCase;
        this.sceneNavigator = new SceneNavigator(springContext);
    }

    @FXML
    public void initialize() {
        if (lienzoCarta == null) return;

        serializacionService = new SerializacionService();
        nodoInteraccionService = new NodoInteraccionService();
        componenteFactory = new ComponenteFactory(null);
        paginacionManager = new PaginacionManager(
                lienzoCarta, serializacionService, nodoInteraccionService, componenteFactory);
        guardadoService = new GuardadoService(expedienteUseCase, plantillaUseCase, serializacionService);

        nodoInteraccionService.configurarEnLienzo(lienzoCarta);
        paginacionManager.configurarControlesUI(
                btnPaginaPrevia, btnPaginaSiguiente, btnAgregarPagina, lblContadorPaginas);
        guardadoService.setLblEstado(lblEstado);

        configurarHerramientas();
        configurarLienzoParaDrop();

        btnGuardar.setOnAction(e -> ejecutarGuardado());
        btnVolver.setOnAction(e -> volverALista());
        lienzoCarta.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getWindow() != null) {
                componenteFactory.setOwnerWindow(newScene.getWindow());
            }
        });
    }

    private void configurarHerramientas() {
        configurarDrag(toolTitulo, "TITULO");
        configurarDrag(toolTextoCorto, "TEXTO_CORTO");
        configurarDrag(toolImagen, "IMAGEN");
        configurarDrag(toolTabla, "TABLA");
        configurarDrag(toolOdontograma, "ODONTOGRAMA");
        configurarDrag(toolAntecedentes, "PRE_ANTECEDENTES");
        configurarDrag(toolContacto, "PRE_CONTACTO");
    }

    private void configurarDrag(Label herramienta, String tipo) {
        if (herramienta == null) {
            return;
        }
        herramienta.setOnDragDetected(event -> {
            Dragboard db = herramienta.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(tipo);
            db.setContent(content);
            event.consume();
        });
    }

    private void configurarLienzoParaDrop() {
        lienzoCarta.setOnDragOver(event -> {
            if (event.getGestureSource() != lienzoCarta && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        lienzoCarta.setOnDragDropped(event -> {
            boolean completed = false;
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                Node nuevoNodo = crearNodoDesdeTipo(db.getString());
                if (nuevoNodo != null) {
                    nuevoNodo.setLayoutX(event.getX());
                    nuevoNodo.setLayoutY(event.getY());
                    nodoInteraccionService.aplicarLogicaNodo(nuevoNodo, db.getString(), lienzoCarta);
                    lienzoCarta.getChildren().add(nuevoNodo);
                    nodoInteraccionService.seleccionarNodo(nuevoNodo);
                    completed = true;
                }
            }
            event.setDropCompleted(completed);
            event.consume();
        });
    }

    private Node crearNodoDesdeTipo(String tipo) {
        return switch (tipo) {
            case "TITULO" -> {
                TextField tf = new TextField("Título");
                tf.getStyleClass().add("input-titulo");
                yield tf;
            }
            case "TEXTO_CORTO" -> {
                TextField tf = new TextField();
                tf.setPromptText("Texto...");
                tf.getStyleClass().add("input-texto");
                yield tf;
            }
            case "IMAGEN" -> componenteFactory.solicitarImagen();
            case "TABLA" -> componenteFactory.crearTabla(2, 2, "Mover Tabla");
            case "ODONTOGRAMA" -> componenteFactory.crearOdontograma();
            case "PRE_ANTECEDENTES" -> componenteFactory.crearTabla(3, 2, "Antecedentes");
            case "PRE_CONTACTO" -> componenteFactory.crearTabla(2, 2, "Contacto");
            default -> null;
        };
    }

    public void prepararLienzoNuevo(String jsonPlantilla) {
        resetEstado();
        if (jsonPlantilla != null && !jsonPlantilla.isEmpty()) {
            cargarJsonEnPaginacion(jsonPlantilla);
            setEstado("Nuevo expediente desde plantilla");
        } else {
            paginacionManager.inicializarConPaginaVacia();
            setEstado("Nuevo expediente en blanco");
        }
    }

    public void configurarComoNuevaPlantilla(String nombre) {
        resetEstado();
        isModoPlantilla = true;
        setNombre(nombre);
        paginacionManager.inicializarConPaginaVacia();
        setEstado("Creando plantilla base");
    }

    public void cargarPlantillaExistente(Plantilla plantilla) {
        resetEstado();
        isModoPlantilla = true;
        plantillaCargada = plantilla;
        setNombre(plantilla.getNombre());
        cargarJsonEnPaginacion(plantilla.getLayoutJson());
        setEstado("Editando plantilla: " + plantilla.getNombre());
    }

    public void cargarExpedienteExistente(Expediente expediente) {
        resetEstado();
        expedienteCargado = expediente;
        setNombre(expediente.getNombrePaciente());
        cargarJsonEnPaginacion(expediente.getLienzoDinamicoJson());
        setEstado("Editando expediente: " + expediente.getNombrePaciente());
    }

    private void ejecutarGuardado() {
        try {
            paginacionManager.salvarPaginaActualAMemoria();
            String nombre = getNombre();

            if (isModoPlantilla) {
                plantillaCargada = guardadoService.guardarPlantilla(
                        plantillaCargada, paginacionManager.getPaginasData(), nombre);
            } else {
                expedienteCargado = guardadoService.guardarExpediente(
                        expedienteCargado, paginacionManager.getPaginasData(), nombre);
            }
        } catch (Exception e) {
            UiDialogs.error(getStage(), "Error al guardar", e.getMessage());
        }
    }

    private void volverALista() {
        try {
            String fxml = isModoPlantilla ? "/fxml/gestor_plantillas.fxml" : "/fxml/lista_expedientes.fxml";
            sceneNavigator.navigate(getStage(), fxml, 900, 600);
        } catch (Exception e) {
            UiDialogs.error(getStage(), "Error de navegación", "No se pudo volver a la pantalla anterior.");
        }
    }

    private void resetEstado() {
        expedienteCargado = null;
        plantillaCargada = null;
        isModoPlantilla = false;
        if (paginacionManager != null) {
            paginacionManager.reset();
        }
        if (lienzoCarta != null) {
            lienzoCarta.getChildren().clear();
        }
    }

    private void cargarJsonEnPaginacion(String json) {
        paginacionManager.cargarPaginas(serializacionService.importarJsonEstructurado(json));
    }

    private void setEstado(String texto) {
        if (lblEstado != null) lblEstado.setText(texto);
    }

    private void setNombre(String nombre) {
        if (txtNombreExpediente != null) txtNombreExpediente.setText(nombre);
    }

    private String getNombre() {
        return (txtNombreExpediente != null && !txtNombreExpediente.getText().trim().isEmpty())
                ? txtNombreExpediente.getText().trim()
                : "Sin Nombre";
    }

    private Stage getStage() {
        return (Stage) btnVolver.getScene().getWindow();
    }
}
