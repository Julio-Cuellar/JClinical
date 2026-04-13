package com.jcode.jclinical.infrastructure.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcode.jclinical.core.domain.model.Expediente;
import com.jcode.jclinical.core.domain.model.Plantilla;
import com.jcode.jclinical.core.port.in.GestionarExpedienteUseCase;
import com.jcode.jclinical.core.port.in.GestionarPlantillaUseCase;
import com.jcode.jclinical.infrastructure.ui.lienzo.*;
import com.jcode.jclinical.infrastructure.ui.support.SceneNavigator;
import com.jcode.jclinical.infrastructure.ui.support.UiDialogs;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

@Component
@Scope("prototype")
public class LienzoDinamicoController {

    // ── Spring / Navegación ────────────────────────────────────
    private final GestionarExpedienteUseCase expedienteUseCase;
    private final GestionarPlantillaUseCase  plantillaUseCase;
    private final SceneNavigator             sceneNavigator;

    // ── Estado del editor ──────────────────────────────────────
    private Expediente expedienteCargado;
    private Plantilla  plantillaCargada;
    private boolean    isModoPlantilla = false;

    // ── Servicios del lienzo ───────────────────────────────────
    private SerializacionService   serializacionService;
    private NodoInteraccionService nodoInteraccionService;
    private ComponenteFactory      componenteFactory;
    private PaginacionManager      paginacionManager;
    private GuardadoService        guardadoService;

    // ── Undo / Redo ────────────────────────────────────────────
    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORIAL = 30;

    // ── FXML — Herramientas ─────────────────────────────────────
    @FXML private Label toolTitulo, toolTextoCorto, toolTextoLargo;
    @FXML private Label toolImagen, toolSeparador, toolAreaFirma;
    @FXML private Label toolTabla, toolSignosVitales, toolNotaEvolucion;
    @FXML private Label toolOdontograma, toolOdontogramaTemporal;
    @FXML private Label toolAntecedentes, toolContacto;
    @FXML private Label btnNomTemplate;

    // ── FXML — Controles ────────────────────────────────────────
    @FXML private Pane     lienzoCarta;
    @FXML private Button   btnGuardar, btnVolver;
    @FXML private Button   btnPaginaPrevia, btnPaginaSiguiente, btnAgregarPagina;
    @FXML private Label    lblEstado, lblContadorPaginas;
    @FXML private TextField txtNombreExpediente;

    // ─────────────────────────────────────────────────────────────

    public LienzoDinamicoController(GestionarExpedienteUseCase expedienteUseCase,
                                    GestionarPlantillaUseCase plantillaUseCase,
                                    ApplicationContext springContext) {
        this.expedienteUseCase = expedienteUseCase;
        this.plantillaUseCase  = plantillaUseCase;
        this.sceneNavigator    = new SceneNavigator(springContext);
    }

    @FXML
    public void initialize() {
        if (lienzoCarta == null) return;

        // Instanciar servicios
        serializacionService   = new SerializacionService();
        nodoInteraccionService = new NodoInteraccionService();
        componenteFactory      = new ComponenteFactory(null);
        paginacionManager      = new PaginacionManager(
                lienzoCarta, serializacionService, nodoInteraccionService, componenteFactory);
        guardadoService        = new GuardadoService(
                expedienteUseCase, plantillaUseCase, serializacionService);

        // Conectar servicios
        nodoInteraccionService.configurarEnLienzo(lienzoCarta);
        paginacionManager.configurarControlesUI(
                btnPaginaPrevia, btnPaginaSiguiente, btnAgregarPagina, lblContadorPaginas);
        guardadoService.setLblEstado(lblEstado);

        // Callbacks de portapapeles
        nodoInteraccionService.setOnCopiar   ((n, t) -> copiarNodo(n, t));
        nodoInteraccionService.setOnCortar   ((n, t) -> cortarNodo(n, t));
        nodoInteraccionService.setOnDuplicar ((n, t) -> duplicarNodo(n, t));
        nodoInteraccionService.setOnPegar    (this::pegarNodo);
        nodoInteraccionService.setOnSnapshot (this::tomarSnapshot);

        // Configurar drag de herramientas y drop del lienzo
        configurarHerramientas();
        configurarLienzoParaDrop();

        // Botones de barra inferior
        btnGuardar.setOnAction(e -> ejecutarGuardado());
        btnVolver.setOnAction (e -> volverALista());

        // Cuando la ventana esté lista, inyectar owner a ComponenteFactory
        lienzoCarta.sceneProperty().addListener((obs, old, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o2, o, win) -> {
                    if (win != null) componenteFactory.setOwnerWindow(win);
                });
                if (newScene.getWindow() != null) componenteFactory.setOwnerWindow(newScene.getWindow());

                // Atajos de teclado a nivel de escena
                newScene.setOnKeyPressed(this::manejarAtajoTeclado);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  ATAJOS DE TECLADO
    // ═══════════════════════════════════════════════════════════

    private void manejarAtajoTeclado(KeyEvent e) {
        boolean ctrl  = e.isControlDown();
        boolean shift = e.isShiftDown();
        KeyCode code  = e.getCode();

        // Ignorar si el foco está en un campo de texto
        if (e.getTarget() instanceof TextField || e.getTarget() instanceof TextArea) {
            // Sólo interceptar Ctrl+S (guardar) y dejar el resto libre
            if (ctrl && code == KeyCode.S) {
                ejecutarGuardado(); e.consume();
            }
            return;
        }

        if (ctrl) {
            switch (code) {
                case S -> { ejecutarGuardado(); e.consume(); }
                case Z -> { if (!shift) undo(); else redo(); e.consume(); }
                case Y -> { redo(); e.consume(); }
                case C -> { copiarSeleccionado(); e.consume(); }
                case X -> { cortarSeleccionado(); e.consume(); }
                case V -> { pegarNodo(); e.consume(); }
                case D -> { duplicarSeleccionado(); e.consume(); }
                default -> {}
            }
        } else if (code == KeyCode.DELETE) {
            eliminarSeleccionado();
            e.consume();
        } else if (code == KeyCode.ESCAPE) {
            nodoInteraccionService.deseleccionarNodo();
            e.consume();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  UNDO / REDO
    // ═══════════════════════════════════════════════════════════

    /** Toma un snapshot de la página actual antes de una operación destructiva. */
    public void tomarSnapshot() {
        try {
            paginacionManager.salvarPaginaActualAMemoria();
            String json = serializacionService.construirJsonFinal(paginacionManager.getPaginasData());
            undoStack.push(json);
            redoStack.clear();
            if (undoStack.size() > MAX_HISTORIAL) undoStack.pollLast();
        } catch (Exception ex) { /* silencioso */ }
    }

    private void undo() {
        if (undoStack.isEmpty()) { setEstado("Sin cambios para deshacer"); return; }
        try {
            paginacionManager.salvarPaginaActualAMemoria();
            String current = serializacionService.construirJsonFinal(paginacionManager.getPaginasData());
            redoStack.push(current);
            String prev = undoStack.pop();
            cargarJsonEnPaginacion(prev);
            setEstado("Deshecho ↩");
        } catch (Exception ex) { setEstado("Error al deshacer"); }
    }

    private void redo() {
        if (redoStack.isEmpty()) { setEstado("Sin cambios para rehacer"); return; }
        try {
            paginacionManager.salvarPaginaActualAMemoria();
            String current = serializacionService.construirJsonFinal(paginacionManager.getPaginasData());
            undoStack.push(current);
            String next = redoStack.pop();
            cargarJsonEnPaginacion(next);
            setEstado("Rehecho ↪");
        } catch (Exception ex) { setEstado("Error al rehacer"); }
    }

    // ═══════════════════════════════════════════════════════════
    //  PORTAPAPELES
    // ═══════════════════════════════════════════════════════════

    private void copiarSeleccionado() {
        Node n = nodoInteraccionService.getNodoSeleccionado();
        String t = nodoInteraccionService.getTipoNodoSeleccionado();
        if (n != null) copiarNodo(n, t);
    }

    private void cortarSeleccionado() {
        Node n = nodoInteraccionService.getNodoSeleccionado();
        String t = nodoInteraccionService.getTipoNodoSeleccionado();
        if (n != null) cortarNodo(n, t);
    }

    private void duplicarSeleccionado() {
        Node n = nodoInteraccionService.getNodoSeleccionado();
        String t = nodoInteraccionService.getTipoNodoSeleccionado();
        if (n != null) duplicarNodo(n, t);
    }

    private void eliminarSeleccionado() {
        Node n = nodoInteraccionService.getNodoSeleccionado();
        if (n != null) {
            tomarSnapshot();
            nodoInteraccionService.deseleccionarNodo();
            lienzoCarta.getChildren().remove(n);
        }
    }

    void copiarNodo(Node nodo, String tipo) {
        try {
            ObjectNode json = serializacionService.serializarNodo(nodo);
            if (!json.has("tipo")) json.put("tipo", tipo);
            nodoInteraccionService.setClipboard(json.toString());
            setEstado("Copiado al portapapeles 📋");
        } catch (Exception ex) { /* silencioso */ }
    }

    void cortarNodo(Node nodo, String tipo) {
        tomarSnapshot();
        copiarNodo(nodo, tipo);
        nodoInteraccionService.deseleccionarNodo();
        lienzoCarta.getChildren().remove(nodo);
        setEstado("Cortado al portapapeles ✂");
    }

    void duplicarNodo(Node nodo, String tipo) {
        tomarSnapshot();
        try {
            ObjectNode json = serializacionService.serializarNodo(nodo);
            if (!json.has("tipo")) json.put("tipo", tipo);
            Node copia = serializacionService.recrearNodo(json, componenteFactory);
            if (copia != null) {
                copia.setLayoutX(nodo.getLayoutX() + 24);
                copia.setLayoutY(nodo.getLayoutY() + 24);
                SerializacionService.aplicarTamano(copia, json);
                nodoInteraccionService.aplicarLogicaNodo(copia, tipo, lienzoCarta);
                lienzoCarta.getChildren().add(copia);
                nodoInteraccionService.seleccionarNodo(copia);
                setEstado("Elemento duplicado 📄");
            }
        } catch (Exception ex) { setEstado("Error al duplicar"); }
    }

    void pegarNodo() {
        String clipboard = nodoInteraccionService.getClipboard();
        if (clipboard == null) { setEstado("Portapapeles vacío"); return; }
        tomarSnapshot();
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json   = om.readTree(clipboard);
            Node copia      = serializacionService.recrearNodo(json, componenteFactory);
            if (copia != null) {
                double x = json.has("x") ? json.get("x").asDouble() + 20 : 60;
                double y = json.has("y") ? json.get("y").asDouble() + 20 : 60;
                copia.setLayoutX(x); copia.setLayoutY(y);
                String tipo = json.has("tipo") ? json.get("tipo").asText() : "TEXTO_CORTO";
                SerializacionService.aplicarTamano(copia, json);
                nodoInteraccionService.aplicarLogicaNodo(copia, tipo, lienzoCarta);
                lienzoCarta.getChildren().add(copia);
                nodoInteraccionService.seleccionarNodo(copia);
                setEstado("Pegado 📌");
            }
        } catch (Exception ex) { setEstado("Error al pegar"); }
    }

    // ═══════════════════════════════════════════════════════════
    //  CONFIGURAR HERRAMIENTAS (Drag & Drop desde toolbar)
    // ═══════════════════════════════════════════════════════════

    private void configurarHerramientas() {
        configurarDrag(toolTitulo,              "TITULO");
        configurarDrag(toolTextoCorto,          "TEXTO_CORTO");
        configurarDrag(toolTextoLargo,          "TEXTO_LARGO");
        configurarDrag(toolImagen,              "IMAGEN");
        configurarDrag(toolSeparador,           "SEPARADOR");
        configurarDrag(toolAreaFirma,           "AREA_FIRMA");
        configurarDrag(toolTabla,               "TABLA");
        configurarDrag(toolSignosVitales,       "PRE_SIGNOS_VITALES");
        configurarDrag(toolNotaEvolucion,       "PRE_NOTA_EVOLUCION");
        configurarDrag(toolOdontograma,         "ODONTOGRAMA");
        configurarDrag(toolOdontogramaTemporal, "ODONTOGRAMA_TEMPORAL");
        configurarDrag(toolAntecedentes,        "PRE_ANTECEDENTES");
        configurarDrag(toolContacto,            "PRE_CONTACTO");

        // Botón NOM: inserta la plantilla NOM completa (no es drag)
        if (btnNomTemplate != null) {
            btnNomTemplate.setOnMouseClicked(e -> insertarPlantillaNOM());
        }
    }

    private void configurarDrag(Label herramienta, String tipo) {
        if (herramienta == null) return;
        herramienta.setOnDragDetected(event -> {
            Dragboard db = herramienta.startDragAndDrop(TransferMode.COPY);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(tipo);
            db.setContent(cc);
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
            boolean done = false;
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                String tipo = db.getString();
                Node nuevoNodo = crearNodoDesdeTipo(tipo);
                if (nuevoNodo != null) {
                    tomarSnapshot();
                    nuevoNodo.setLayoutX(event.getX());
                    nuevoNodo.setLayoutY(event.getY());
                    nodoInteraccionService.aplicarLogicaNodo(nuevoNodo, tipo, lienzoCarta);
                    lienzoCarta.getChildren().add(nuevoNodo);
                    nodoInteraccionService.seleccionarNodo(nuevoNodo);
                    done = true;
                }
            }
            event.setDropCompleted(done);
            event.consume();
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  FÁBRICA DE NODOS POR TIPO
    // ═══════════════════════════════════════════════════════════

    private Node crearNodoDesdeTipo(String tipo) {
        return switch (tipo) {
            case "TITULO" -> {
                TextField tf = new TextField("Título");
                tf.getStyleClass().add("input-titulo");
                yield tf;
            }
            case "TEXTO_CORTO" -> {
                TextField tf = new TextField();
                tf.setPromptText("Texto corto...");
                tf.getStyleClass().add("input-texto");
                yield tf;
            }
            case "TEXTO_LARGO" -> componenteFactory.crearTextArea("");
            case "IMAGEN"      -> componenteFactory.solicitarImagen();
            case "SEPARADOR"   -> componenteFactory.crearSeparador();
            case "AREA_FIRMA"  -> componenteFactory.crearAreaFirma();

            case "TABLA"              -> componenteFactory.crearTabla(2, 2, "Tabla");
            case "PRE_ANTECEDENTES"   -> componenteFactory.crearTabla(4, 2, "Antecedentes Heredofamiliares");
            case "PRE_CONTACTO"       -> componenteFactory.crearTabla(3, 2, "Datos de Contacto");
            case "PRE_SIGNOS_VITALES" -> componenteFactory.crearSignosVitales();
            case "PRE_NOTA_EVOLUCION" -> componenteFactory.crearNotaEvolucion();

            case "ODONTOGRAMA"          -> componenteFactory.crearOdontograma();
            case "ODONTOGRAMA_TEMPORAL" -> componenteFactory.crearOdontogramaTemporal();

            default -> null;
        };
    }

    /** Inserta la plantilla NOM completa (4 páginas) en el expediente actual. */
    private void insertarPlantillaNOM() {
        boolean confirmar = UiDialogs.confirm(getStage(),
                "Historia Clínica NOM",
                "¿Deseas reemplazar el contenido actual con la plantilla Historia Clínica\n" +
                "Estomatológica (NOM-004-SSA3-2012 / NOM-013-SSA2-2015)?\n\n" +
                "⚠ Esta acción reemplazará el lienzo actual.");
        if (!confirmar) return;

        tomarSnapshot();
        try {
            String jsonNOM = componenteFactory.generarJsonPlantillaNOM();
            cargarJsonEnPaginacion(jsonNOM);
            setEstado("Plantilla Historia Clínica NOM cargada ✓");
        } catch (Exception ex) {
            UiDialogs.error(getStage(), "Error", "No se pudo cargar la plantilla NOM: " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  CARGA / CONFIGURACIÓN DEL LIENZO
    // ═══════════════════════════════════════════════════════════

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
        setEstado("Creando plantilla: " + nombre);
    }

    public void cargarPlantillaExistente(Plantilla plantilla) {
        resetEstado();
        isModoPlantilla  = true;
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
        setEstado("Editando: " + expediente.getNombrePaciente());
    }

    // ═══════════════════════════════════════════════════════════
    //  GUARDADO
    // ═══════════════════════════════════════════════════════════

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
            undoStack.clear(); // Limpiar historial tras guardado exitoso
        } catch (Exception e) {
            UiDialogs.error(getStage(), "Error al guardar", e.getMessage());
        }
    }

    private void volverALista() {
        try {
            String fxml = isModoPlantilla
                    ? "/fxml/gestor_plantillas.fxml"
                    : "/fxml/lista_expedientes.fxml";
            sceneNavigator.navigate(getStage(), fxml, 960, 640);
        } catch (Exception e) {
            UiDialogs.error(getStage(), "Error de navegación", "No se pudo regresar a la pantalla anterior.");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS PRIVADOS
    // ═══════════════════════════════════════════════════════════

    private void resetEstado() {
        expedienteCargado  = null;
        plantillaCargada   = null;
        isModoPlantilla    = false;
        undoStack.clear();
        redoStack.clear();
        if (paginacionManager != null) paginacionManager.reset();
        if (lienzoCarta       != null) lienzoCarta.getChildren().clear();
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
        if (txtNombreExpediente != null && !txtNombreExpediente.getText().trim().isEmpty()) {
            return txtNombreExpediente.getText().trim();
        }
        return "Sin Nombre";
    }

    private Stage getStage() {
        return (Stage) btnVolver.getScene().getWindow();
    }
}