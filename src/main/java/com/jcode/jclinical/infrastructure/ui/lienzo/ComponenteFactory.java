package com.jcode.jclinical.infrastructure.ui.lienzo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Fábrica de componentes visuales para el lienzo dinámico.
 * Cumple con los estándares de la NOM-013-SSA2-2015 (salud bucal)
 * y NOM-004-SSA3-2012 (expediente clínico electrónico).
 */
public class ComponenteFactory {

    private Window ownerWindow;

    // ─── Estados del diente (FDI / NOM-013) ─────────────────────
    private static final String[] ESTADOS_DIENTE = {
        "estado-sano", "estado-caries", "estado-amalgama", "estado-resina",
        "estado-corona", "estado-puente", "estado-endodoncia", "estado-extraccion",
        "estado-perdido", "estado-implante", "estado-protesis-fija",
        "estado-protesis-removible", "estado-sellador", "estado-fractura",
        "estado-erupcion", "estado-faceta", "estado-movilidad",
        "estado-giro", "estado-supernumerario", "estado-agenesia"
    };

    private static final String[] NOMBRES_ESTADO = {
        "✓ Sano", "● Caries", "▪ Amalgama", "■ Resina/Composite",
        "👑 Corona", "⊓ Puente", "◎ Endodoncia", "✕ Extracción indicada",
        "✗ Pieza ausente", "⊕ Implante", "⌇ Prótesis fija",
        "⌒ Prótesis removible", "S Sellador", "╲ Fractura",
        "↑ Diente en erupción", "◇ Faceta/Carilla", "~ Movilidad",
        "↻ Giro/Rotación", "* Supernumerario", "○ Agenesia"
    };

    // ─── Símbolos de estado para mostrar en el diente ──────────
    private static final String[] SIMBOLOS_ESTADO = {
        "", "●", "", "", "C", "P", "◎", "X",
        "X", "I", "PF", "PR", "S", "F",
        "↑", "◇", "M", "G", "+", "A"
    };

    public ComponenteFactory(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    // ═══════════════════════════════════════════════════════════
    //  TABLA DINÁMICA
    // ═══════════════════════════════════════════════════════════

    public Node crearTabla(int filas, int columnas, String titulo) {
        VBox contenedor = new VBox(0);
        contenedor.getStyleClass().add("tabla-container");

        // Barra de título con colapsar
        HBox barra = new HBox(6);
        barra.getStyleClass().add("tabla-barra");
        barra.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = new Label("≡  " + titulo);
        lblTitulo.getStyleClass().add("tabla-barra-label");
        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnColapsar = new Button("▲");
        btnColapsar.getStyleClass().add("btn-colapsar");

        barra.getChildren().addAll(lblTitulo, spacer, btnColapsar);

        // Cuerpo
        VBox cuerpo = new VBox(6);
        cuerpo.getStyleClass().add("tabla-cuerpo");
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        GridPane grid = construirGrid(filas, columnas);
        HBox controles = construirControlesTabla(grid);

        cuerpo.getChildren().addAll(controles, grid);
        contenedor.getChildren().addAll(barra, cuerpo);

        // Colapsar / Expandir
        btnColapsar.setOnAction(e -> toggleColapsar(cuerpo, btnColapsar));

        return contenedor;
    }

    private GridPane construirGrid(int filas, int columnas) {
        GridPane grid = new GridPane();
        grid.setHgap(1); grid.setVgap(1);
        VBox.setVgrow(grid, Priority.ALWAYS);

        final int[] rows = {filas};
        final int[] cols = {columnas};

        // Constraints actualizables
        grid.getProperties().put("updateConstraints", (Runnable) () -> {
            grid.getColumnConstraints().clear();
            grid.getRowConstraints().clear();
            for (int i = 0; i < cols[0]; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(100.0 / cols[0]);
                cc.setHgrow(Priority.ALWAYS);
                grid.getColumnConstraints().add(cc);
            }
            for (int i = 0; i < rows[0]; i++) {
                RowConstraints rc = new RowConstraints();
                rc.setVgrow(Priority.ALWAYS);
                rc.setMinHeight(28);
                grid.getRowConstraints().add(rc);
            }
        });

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                grid.add(crearCeldaTabla(), j, i);
            }
        }
        runUpdateConstraints(grid);
        // Guardar contadores en properties del grid
        grid.getProperties().put("rows", rows);
        grid.getProperties().put("cols", cols);
        return grid;
    }

    @SuppressWarnings("unchecked")
    private HBox construirControlesTabla(GridPane grid) {
        HBox controles = new HBox(4);
        Button btnAddCol  = new Button("+ Col");
        Button btnRemCol  = new Button("− Col");
        Button btnAddFila = new Button("+ Fila");
        Button btnRemFila = new Button("− Fila");
        for (Button b : new Button[]{btnAddCol, btnRemCol, btnAddFila, btnRemFila}) {
            b.getStyleClass().add("btn-tabla");
        }

        btnAddCol.setOnAction(e -> {
            int[] cols = (int[]) grid.getProperties().get("cols");
            int[] rows = (int[]) grid.getProperties().get("rows");
            for (int i = 0; i < rows[0]; i++) grid.add(crearCeldaTabla(), cols[0], i);
            cols[0]++;
            runUpdateConstraints(grid);
        });
        btnRemCol.setOnAction(e -> {
            int[] cols = (int[]) grid.getProperties().get("cols");
            if (cols[0] > 1) {
                cols[0]--;
                int target = cols[0];
                grid.getChildren().removeIf(n -> GridPane.getColumnIndex(n) != null && GridPane.getColumnIndex(n) == target);
                runUpdateConstraints(grid);
            }
        });
        btnAddFila.setOnAction(e -> {
            int[] rows = (int[]) grid.getProperties().get("rows");
            int[] cols = (int[]) grid.getProperties().get("cols");
            for (int j = 0; j < cols[0]; j++) grid.add(crearCeldaTabla(), j, rows[0]);
            rows[0]++;
            runUpdateConstraints(grid);
        });
        btnRemFila.setOnAction(e -> {
            int[] rows = (int[]) grid.getProperties().get("rows");
            if (rows[0] > 1) {
                rows[0]--;
                int target = rows[0];
                grid.getChildren().removeIf(n -> GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) == target);
                runUpdateConstraints(grid);
            }
        });

        controles.getChildren().addAll(btnAddCol, btnRemCol, btnAddFila, btnRemFila);
        return controles;
    }

    private void runUpdateConstraints(GridPane grid) {
        Runnable r = (Runnable) grid.getProperties().get("updateConstraints");
        if (r != null) r.run();
    }

    private TextField crearCeldaTabla() {
        TextField tf = new TextField();
        tf.getStyleClass().add("tabla-celda");
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setMaxHeight(Double.MAX_VALUE);
        tf.setOnMousePressed(Event::consume);
        return tf;
    }

    public Node reconstruirTabla(JsonNode json) {
        int filas = json.has("filas") ? json.get("filas").asInt() : 2;
        int cols  = json.has("columnas") ? json.get("columnas").asInt() : 2;
        String titulo = json.has("titulo") ? json.get("titulo").asText() : "Tabla";
        Node tabla = crearTabla(filas, cols, titulo);

        if (json.has("celdas")) {
            List<Node> celdas = new ArrayList<>();
            SerializacionService.buscarNodosPorClase(tabla, "tabla-celda", celdas);
            for (JsonNode cd : json.get("celdas")) {
                int row = cd.get("r").asInt();
                int col = cd.get("c").asInt();
                for (Node vc : celdas) {
                    Integer r = GridPane.getRowIndex(vc); if (r == null) r = 0;
                    Integer c = GridPane.getColumnIndex(vc); if (c == null) c = 0;
                    if (r == row && c == col) {
                        ((TextField) vc).setText(cd.get("txt").asText());
                        break;
                    }
                }
            }
        }
        return tabla;
    }

    // ═══════════════════════════════════════════════════════════
    //  ODONTOGRAMA PERMANENTE (FDI / NOM-013-SSA2-2015)
    // ═══════════════════════════════════════════════════════════

    public Node crearOdontograma() {
        return construirOdontograma(false);
    }

    public Node crearOdontogramaTemporal() {
        return construirOdontograma(true);
    }

    private Node construirOdontograma(boolean temporal) {
        VBox contenedor = new VBox(0);
        contenedor.getStyleClass().add("odontograma-container");

        // Barra de título
        HBox barra = new HBox(6);
        barra.getStyleClass().add("odontograma-barra");
        barra.setAlignment(Pos.CENTER_LEFT);

        String titulo = temporal ? "≡  Odontograma Temporal (Deciduo)" : "≡  Odontograma Permanente";
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("odontograma-barra-label");
        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnColapsar = new Button("▲");
        btnColapsar.getStyleClass().add("btn-colapsar");
        barra.getChildren().addAll(lblTitulo, spacer, btnColapsar);

        // Cuerpo
        VBox cuerpo = new VBox(8);
        cuerpo.getStyleClass().add("odontograma-body");

        if (temporal) {
            cuerpo.getChildren().addAll(
                construirFilaDientes(new int[]{55,54,53,52,51}, new int[]{61,62,63,64,65}, true, "Arco Superior Temporal"),
                crearDivisorArcos(),
                construirFilaDientes(new int[]{85,84,83,82,81}, new int[]{71,72,73,74,75}, false, "Arco Inferior Temporal")
            );
        } else {
            cuerpo.getChildren().addAll(
                construirFilaDientes(new int[]{18,17,16,15,14,13,12,11}, new int[]{21,22,23,24,25,26,27,28}, true, "Arco Superior"),
                crearDivisorArcos(),
                construirFilaDientes(new int[]{48,47,46,45,44,43,42,41}, new int[]{31,32,33,34,35,36,37,38}, false, "Arco Inferior")
            );
        }

        contenedor.getChildren().addAll(barra, cuerpo);
        btnColapsar.setOnAction(e -> toggleColapsar(cuerpo, btnColapsar));
        return contenedor;
    }

    /** Construye una fila con dientes del cuadrante derecho + divisor + cuadrante izquierdo. */
    private HBox construirFilaDientes(int[] cuadranteDer, int[] cuadranteIzq,
                                       boolean esMaxilar, String etiqueta) {
        HBox fila = new HBox(2);
        fila.getStyleClass().add("odontograma-fila");
        fila.setAlignment(Pos.CENTER);

        Label lblEtiq = new Label(etiqueta);
        lblEtiq.getStyleClass().add("odontograma-arco-label");
        lblEtiq.setMinWidth(90);

        HBox zonaDer = new HBox(2); zonaDer.setAlignment(Pos.CENTER_RIGHT);
        for (int n : cuadranteDer) zonaDer.getChildren().add(crearOrganoDental(n, esMaxilar));

        // Divisor central
        VBox divisor = new VBox();
        divisor.getStyleClass().add("cuadrante-divider");

        HBox zonaIzq = new HBox(2); zonaIzq.setAlignment(Pos.CENTER_LEFT);
        for (int n : cuadranteIzq) zonaIzq.getChildren().add(crearOrganoDental(n, esMaxilar));

        fila.getChildren().addAll(lblEtiq, zonaDer, divisor, zonaIzq);
        return fila;
    }

    private HBox crearDivisorArcos() {
        HBox divArcos = new HBox();
        divArcos.setStyle("-fx-border-color: #2D5F5F; -fx-border-width: 1 0 0 0; -fx-opacity: 0.2; -fx-min-height: 4px; -fx-pref-height: 4px;");
        HBox.setHgrow(divArcos, Priority.ALWAYS);
        return divArcos;
    }

    /**
     * Crea la representación visual completa de un diente:
     * superficie superior (V/I) + cuerpo del diente + superficie inferior (P/L)
     * para dientes superiores; orden invertido para inferiores.
     */
    private Node crearOrganoDental(int numero, boolean esMaxilar) {
        VBox wrapper = new VBox(1);
        wrapper.getStyleClass().add("diente-wrapper");
        wrapper.setAlignment(Pos.CENTER);

        // Grilla de superficies (V-oclusal-L/P)
        GridPane gridSup = crearGridSuperficies(numero);

        // Cuerpo principal del diente
        StackPane body = new StackPane();
        body.getStyleClass().addAll("diente-base", "estado-sano");

        Label lblNum = new Label(String.valueOf(numero));
        lblNum.getStyleClass().add("diente-numero");
        body.getChildren().add(lblNum);

        // Menú contextual de estados
        body.setOnMouseClicked(e -> {
            ContextMenu menu = crearMenuEstadoDiente(body, lblNum);
            menu.show(body, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        if (esMaxilar) {
            // Superior: superficies arriba (Vestibular en top), luego el diente
            wrapper.getChildren().addAll(gridSup, body);
        } else {
            // Inferior: diente arriba, superficies abajo
            wrapper.getChildren().addAll(body, gridSup);
        }
        return wrapper;
    }

    /** Crea la cuadrícula de 5 superficies en cruz (V, M, O/I, D, L/P). */
    private GridPane crearGridSuperficies(int numeroDiente) {
        GridPane grid = new GridPane();
        grid.setHgap(1); grid.setVgap(1);
        grid.setAlignment(Pos.CENTER);

        StackPane supV = crearCeldaSuperficie("V");   // Vestibular
        StackPane supM = crearCeldaSuperficie("M");   // Mesial
        StackPane supO = crearCeldaSuperficie("O");   // Oclusal / Incisal
        StackPane supD = crearCeldaSuperficie("D");   // Distal
        StackPane supL = crearCeldaSuperficie("L");   // Lingual / Palatino

        // Posiciones en grid 3×3 (solo 5 celdas en forma de cruz)
        grid.add(supV, 1, 0);
        grid.add(supM, 0, 1);
        grid.add(supO, 1, 1);
        grid.add(supD, 2, 1);
        grid.add(supL, 1, 2);

        return grid;
    }

    /** Celda de superficie individual, identificada por clase CSS superficie-[X]. */
    private StackPane crearCeldaSuperficie(String supKey) {
        StackPane sp = new StackPane();
        sp.getStyleClass().addAll("superficie-base", "superficie-" + supKey);
        sp.setMinSize(9, 9); sp.setMaxSize(9, 9); sp.setPrefSize(9, 9);

        sp.setOnMouseClicked(e -> {
            ContextMenu menu = new ContextMenu();
            MenuItem sana    = new MenuItem("Sana");
            MenuItem caries  = new MenuItem("● Caries");
            MenuItem amalg   = new MenuItem("▪ Amalgama");
            MenuItem resina  = new MenuItem("■ Resina");
            MenuItem sell    = new MenuItem("S Sellador");
            MenuItem limpiar = new MenuItem("✕ Limpiar");

            sana.setOnAction   (ev -> marcarSuperficie(sp, ""));
            caries.setOnAction (ev -> marcarSuperficie(sp, "sup-caries"));
            amalg.setOnAction  (ev -> marcarSuperficie(sp, "sup-amalgama"));
            resina.setOnAction (ev -> marcarSuperficie(sp, "sup-resina"));
            sell.setOnAction   (ev -> marcarSuperficie(sp, "sup-sellador"));
            limpiar.setOnAction(ev -> marcarSuperficie(sp, ""));

            menu.getItems().addAll(sana, new SeparatorMenuItem(), caries, amalg, resina, sell,
                                   new SeparatorMenuItem(), limpiar);
            menu.show(sp, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        return sp;
    }

    private void marcarSuperficie(StackPane sp, String clase) {
        sp.getStyleClass().removeAll("sup-caries", "sup-amalgama", "sup-resina", "sup-sellador");
        if (!clase.isEmpty()) sp.getStyleClass().add(clase);
    }

    /** Menú contextual completo con todos los estados del diente según NOM-013. */
    private ContextMenu crearMenuEstadoDiente(StackPane body, Label lblNum) {
        ContextMenu menu = new ContextMenu();
        for (int i = 0; i < ESTADOS_DIENTE.length; i++) {
            final String estado   = ESTADOS_DIENTE[i];
            final String nombre   = NOMBRES_ESTADO[i];
            final String simbolo  = SIMBOLOS_ESTADO[i];
            MenuItem item = new MenuItem(nombre);
            item.setOnAction(ev -> cambiarEstadoDiente(body, lblNum, estado, simbolo));
            menu.getItems().add(item);
            if (i == 0 || i == 3 || i == 8 || i == 11 || i == 13) {
                menu.getItems().add(new SeparatorMenuItem());
            }
        }
        return menu;
    }

    private void cambiarEstadoDiente(StackPane body, Label lblNum,
                                     String nuevoEstado, String simbolo) {
        // Quitar todos los estados anteriores
        body.getStyleClass().removeAll(ESTADOS_DIENTE);
        // Quitar símbolos extra (etiquetas adicionales)
        body.getChildren().removeIf(n -> n instanceof Label lbl && !lbl.equals(lblNum));

        body.getStyleClass().add(nuevoEstado);

        // Mostrar símbolo encima del número para algunos estados
        if (!simbolo.isEmpty()) {
            Label lblSim = new Label(simbolo);
            lblSim.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: inherit;");
            // Para extracción, mostrar X grande en rojo
            if (nuevoEstado.equals("estado-extraccion") || nuevoEstado.equals("estado-perdido")) {
                lblSim.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #E74C3C;");
            }
            body.getChildren().add(lblSim);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  RECONSTRUIR ODONTOGRAMA
    // ═══════════════════════════════════════════════════════════

    public Node reconstruirOdontograma(JsonNode json) {
        boolean temporal = json.has("temporal") && json.get("temporal").asBoolean();
        Node odonto = temporal ? crearOdontogramaTemporal() : crearOdontograma();

        if (!json.has("dientes")) return odonto;

        List<Node> bodies = new ArrayList<>();
        SerializacionService.buscarNodosPorClase(odonto, "diente-base", bodies);

        for (JsonNode d : json.get("dientes")) {
            String numero = d.get("numero").asText();
            String estado = d.has("estado") ? d.get("estado").asText() : "estado-sano";

            for (Node dv : bodies) {
                StackPane body = (StackPane) dv;
                Label lbl = (Label) body.getChildren().stream()
                        .filter(n -> n instanceof Label l && l.getStyleClass().contains("diente-numero"))
                        .findFirst().orElse(null);
                if (lbl == null || !lbl.getText().equals(numero)) continue;

                // Buscar el símbolo correspondiente al estado
                String simbolo = "";
                for (int i = 0; i < ESTADOS_DIENTE.length; i++) {
                    if (ESTADOS_DIENTE[i].equals(estado)) { simbolo = SIMBOLOS_ESTADO[i]; break; }
                }
                cambiarEstadoDiente(body, lbl, estado, simbolo);

                // Restaurar superficies
                if (d.has("superficies")) {
                    Node wrapper = body.getParent();
                    if (wrapper != null) restaurarSuperficies(wrapper, d.get("superficies"));
                }
                break;
            }
        }
        return odonto;
    }

    private void restaurarSuperficies(Node wrapper, JsonNode superficies) {
        for (String sup : List.of("V", "M", "O", "D", "L")) {
            if (!superficies.has(sup)) continue;
            String claseEstado = superficies.get(sup).asText();
            if (claseEstado.isEmpty()) continue;

            List<Node> supNodes = new ArrayList<>();
            SerializacionService.buscarNodosPorClase(wrapper, "superficie-" + sup, supNodes);
            if (!supNodes.isEmpty()) {
                StackPane cell = (StackPane) supNodes.get(0);
                marcarSuperficie(cell, claseEstado);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  NUEVOS ELEMENTOS
    // ═══════════════════════════════════════════════════════════

    /** TextArea multilinea para notas extensas. */
    public Node crearTextArea(String placeholder) {
        TextArea ta = new TextArea();
        ta.setPromptText(placeholder.isEmpty() ? "Escriba aquí..." : placeholder);
        ta.getStyleClass().add("input-texto-largo");
        ta.setWrapText(true);
        ta.setPrefRowCount(4);
        ta.setOnMousePressed(Event::consume);
        return ta;
    }

    /** Línea separadora de sección. */
    public Node crearSeparador() {
        HBox sep = new HBox();
        sep.getStyleClass().add("separador-elemento");
        sep.setMinHeight(4); sep.setPrefHeight(4); sep.setMaxHeight(4);
        sep.setPrefWidth(600);
        return sep;
    }

    /** Área de firma con líneas para nombre, cédula y fecha. */
    public Node crearAreaFirma() {
        VBox contenedor = new VBox(0);
        contenedor.getStyleClass().add("area-firma-container");

        HBox barra = new HBox(6);
        barra.getStyleClass().add("area-firma-barra");
        barra.setAlignment(Pos.CENTER_LEFT);
        Label lblT = new Label("≡  Área de Firma");
        lblT.getStyleClass().add("tabla-barra-label");
        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnCol = new Button("▲"); btnCol.getStyleClass().add("btn-colapsar");
        barra.getChildren().addAll(lblT, spacer, btnCol);

        VBox cuerpo = new VBox(8);
        cuerpo.getStyleClass().add("area-firma-body");

        HBox firmasRow = new HBox(20);
        firmasRow.setAlignment(Pos.CENTER);

        firmasRow.getChildren().addAll(
            crearBloqueFirema("Nombre del Responsable Sanitario"),
            crearBloqueFirema("Cédula Profesional"),
            crearBloqueFirema("Firma y Sello")
        );

        HBox fechaRow = new HBox(20);
        fechaRow.setAlignment(Pos.CENTER_LEFT);
        TextField tfFecha = new TextField();
        tfFecha.setPromptText("Fecha: dd/mm/aaaa");
        tfFecha.getStyleClass().add("input-texto");
        tfFecha.setPrefWidth(200);
        tfFecha.setOnMousePressed(Event::consume);
        fechaRow.getChildren().add(tfFecha);

        cuerpo.getChildren().addAll(firmasRow, fechaRow);
        contenedor.getChildren().addAll(barra, cuerpo);

        btnCol.setOnAction(e -> toggleColapsar(cuerpo, btnCol));
        return contenedor;
    }

    private VBox crearBloqueFirema(String etiqueta) {
        VBox bloque = new VBox(4);
        bloque.setAlignment(Pos.CENTER);
        HBox.setHgrow(bloque, Priority.ALWAYS);

        Pane linea = new Pane();
        linea.getStyleClass().add("firma-linea");
        linea.setPrefWidth(180);
        linea.setPrefHeight(55);

        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("firma-etiqueta");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER);

        bloque.getChildren().addAll(linea, lbl);
        return bloque;
    }

    /** Tabla preconfigurada para signos vitales. */
    public Node crearSignosVitales() {
        VBox tabla = (VBox) crearTabla(3, 5, "Signos Vitales");
        // Precargar encabezados
        List<Node> celdas = new ArrayList<>();
        SerializacionService.buscarNodosPorClase(tabla, "tabla-celda", celdas);
        String[] headers = {"T.A. (mmHg)", "F.C. (lpm)", "F.R. (rpm)", "Temperatura", "SatO₂ (%)"};
        for (int i = 0; i < Math.min(headers.length, celdas.size()); i++) {
            ((TextField) celdas.get(i)).setText(headers[i]);
        }
        return tabla;
    }

    /** Tabla preconfigurada para nota de evolución. */
    public Node crearNotaEvolucion() {
        VBox tabla = (VBox) crearTabla(4, 4, "Nota de Evolución");
        List<Node> celdas = new ArrayList<>();
        SerializacionService.buscarNodosPorClase(tabla, "tabla-celda", celdas);
        String[] headers = {"Fecha", "Tratamiento Realizado", "Observaciones Clínicas", "Firma"};
        for (int i = 0; i < Math.min(headers.length, celdas.size()); i++) {
            ((TextField) celdas.get(i)).setText(headers[i]);
        }
        return tabla;
    }

    // ═══════════════════════════════════════════════════════════
    //  IMAGEN
    // ═══════════════════════════════════════════════════════════

    public ImageView solicitarImagen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Cargar Imagen o Rx");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes / Radiografías",
                "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif", "*.tiff")
        );
        File f = fc.showOpenDialog(ownerWindow);
        if (f == null) return null;

        try {
            String mimeType = Files.probeContentType(f.toPath());
            if (mimeType == null) mimeType = inferMimeType(f.getName());
            String base64  = Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath()));
            String dataUri = "data:" + mimeType + ";base64," + base64;
            ImageView iv   = new ImageView(new Image(dataUri));
            iv.setFitWidth(220);
            iv.setPreserveRatio(true);
            iv.setUserData(dataUri);
            return iv;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la imagen.", e);
        }
    }

    private String inferMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".bmp"))  return "image/bmp";
        if (lower.endsWith(".gif"))  return "image/gif";
        return "application/octet-stream";
    }

    // ═══════════════════════════════════════════════════════════
    //  PLANTILLA NOM (NOM-004-SSA3-2012 + NOM-013-SSA2-2015)
    // ═══════════════════════════════════════════════════════════

    /**
     * Genera el JSON completo de la Historia Clínica Estomatológica
     * conforme a NOM-004-SSA3-2012 y NOM-013-SSA2-2015.
     * 4 páginas: 1-Identificación, 2-Antecedentes, 3-Exploración+Odontograma, 4-Diagnóstico+Notas
     */
    public String generarJsonPlantillaNOM() {
        try {
            ObjectMapper m = new ObjectMapper();
            ObjectNode root = m.createObjectNode();
            ArrayNode pags  = root.putArray("paginas");

            pags.add(pagina1Identificacion(m));
            pags.add(pagina2Antecedentes(m));
            pags.add(pagina3ExploracionOdontograma(m));
            pags.add(pagina4DiagnosticoNotas(m));

            return m.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar la plantilla NOM.", e);
        }
    }

    private ObjectNode pagina1Identificacion(ObjectMapper m) {
        ArrayNode els = m.createArrayNode();

        // Título principal
        els.add(elem(m, "TITULO",     "HISTORIA CLÍNICA ESTOMATOLÓGICA",     50, 20,  700, 38));
        els.add(elem(m, "TEXTO_CORTO","NOM-013-SSA2-2015  |  NOM-004-SSA3-2012", 50, 62, 700, 28));
        els.add(elem(m, "SEPARADOR",  "",                                      50, 96,  700, 4));

        // Datos del establecimiento
        els.add(elem(m, "TITULO",     "I. Datos del Establecimiento",          50, 110, 700, 34));
        ObjectNode tEstab = tablaNode(m, 3, 2, "Establecimiento",              50, 148, 700, 110);
        setCeldas(tEstab, new String[][]{
            {"Nombre / Razón Social:", ""}, {"Dirección:", ""}, {"Cédula Profesional:", ""}
        });
        els.add(tEstab);

        // Datos del paciente
        els.add(elem(m, "TITULO",     "II. Datos del Paciente",                50, 268, 700, 34));
        ObjectNode tPac = tablaNode(m, 6, 4, "Datos del Paciente",             50, 306, 700, 220);
        setCeldas(tPac, new String[][]{
            {"Nombre completo:", "", "Fecha de nacimiento:", ""},
            {"Sexo:", "", "Edad:", ""},
            {"CURP:", "", "NSS / Folio:", ""},
            {"Teléfono:", "", "Correo electrónico:", ""},
            {"Dirección:", "", "Ocupación:", ""},
            {"Médico remitente:", "", "Fecha de consulta:", ""}
        });
        els.add(tPac);

        // Motivo de consulta
        els.add(elem(m, "TITULO",     "III. Motivo de Consulta",               50, 535, 700, 34));
        els.add(elem(m, "TEXTO_LARGO","",                                       50, 573, 700, 90));

        ObjectNode pag = m.createObjectNode();
        pag.set("elementos", els);
        return pag;
    }

    private ObjectNode pagina2Antecedentes(ObjectMapper m) {
        ArrayNode els = m.createArrayNode();

        // Antecedentes Heredofamiliares
        els.add(elem(m, "TITULO", "IV. Antecedentes Heredofamiliares", 50, 20, 700, 34));
        ObjectNode tHF = tablaNode(m, 5, 3, "Antecedentes HF", 50, 58, 700, 165);
        setCeldas(tHF, new String[][]{
            {"Condición / Enfermedad", "Familiar afectado", "Observaciones"},
            {"Diabetes mellitus", "", ""},
            {"Hipertensión arterial", "", ""},
            {"Cardiopatías", "", ""},
            {"Cáncer / Oncológico", "", ""}
        });
        els.add(tHF);

        // Antecedentes personales no patológicos
        els.add(elem(m, "TITULO", "V. Antecedentes No Patológicos", 50, 233, 700, 34));
        ObjectNode tNP = tablaNode(m, 4, 2, "No Patológicos", 50, 271, 700, 140);
        setCeldas(tNP, new String[][]{
            {"Higiene bucal (frecuencia/técnica):", ""},
            {"Hábitos (tabaco, alcohol, parafuncionales):", ""},
            {"Dieta / Tipo de alimentación:", ""},
            {"Actividad física:", ""}
        });
        els.add(tNP);

        // Antecedentes personales patológicos
        els.add(elem(m, "TITULO", "VI. Antecedentes Patológicos", 50, 421, 700, 34));
        ObjectNode tP = tablaNode(m, 5, 2, "Patológicos", 50, 459, 700, 175);
        setCeldas(tP, new String[][]{
            {"Alergias (medicamentos, alimentos, látex):", ""},
            {"Medicamentos actuales:", ""},
            {"Enfermedades sistémicas:", ""},
            {"Cirugías / Hospitalizaciones previas:", ""},
            {"Antecedentes estomatológicos:", ""}
        });
        els.add(tP);

        // Signos vitales
        els.add(elem(m, "TITULO", "VII. Signos Vitales", 50, 644, 700, 34));
        ObjectNode tSV = tablaNode(m, 2, 5, "Signos Vitales", 50, 682, 700, 88);
        setCeldas(tSV, new String[][]{
            {"T.A. (mmHg)", "F.C. (lpm)", "F.R. (rpm)", "Temperatura (°C)", "SatO₂ (%)"},
            {"", "", "", "", ""}
        });
        els.add(tSV);

        ObjectNode pag = m.createObjectNode();
        pag.set("elementos", els);
        return pag;
    }

    private ObjectNode pagina3ExploracionOdontograma(ObjectMapper m) {
        ArrayNode els = m.createArrayNode();

        // Exploración estomatológica
        els.add(elem(m, "TITULO", "VIII. Exploración Estomatológica", 50, 20, 700, 34));
        ObjectNode tEx = tablaNode(m, 7, 2, "Exploración", 50, 58, 700, 230);
        setCeldas(tEx, new String[][]{
            {"Estructura a explorar", "Hallazgos clínicos"},
            {"Labios y comisuras:", ""},
            {"Mucosa yugal / carrillos:", ""},
            {"Encías (periodontograma):", ""},
            {"Lengua (dorso, bordes, cara ventral):", ""},
            {"Paladar duro y blando / Orofaringe:", ""},
            {"Piso de boca:", ""}
        });
        els.add(tEx);

        // Odontograma
        els.add(elem(m, "TITULO", "IX. Odontograma", 50, 298, 700, 34));
        ObjectNode odonto = m.createObjectNode();
        odonto.put("tipo", "ODONTOGRAMA");
        odonto.put("x", 50); odonto.put("y", 336);
        odonto.put("w", 700); odonto.put("h", 220);
        odonto.putArray("dientes");
        els.add(odonto);

        // Padecimiento actual
        els.add(elem(m, "TITULO", "X. Padecimiento Actual", 50, 566, 700, 34));
        els.add(elem(m, "TEXTO_LARGO", "", 50, 604, 700, 110));

        ObjectNode pag = m.createObjectNode();
        pag.set("elementos", els);
        return pag;
    }

    private ObjectNode pagina4DiagnosticoNotas(ObjectMapper m) {
        ArrayNode els = m.createArrayNode();

        // Diagnóstico
        els.add(elem(m, "TITULO", "XI. Diagnóstico(s)", 50, 20, 700, 34));
        els.add(elem(m, "TEXTO_LARGO", "", 50, 58, 700, 90));

        // Plan de tratamiento
        els.add(elem(m, "TITULO", "XII. Plan de Tratamiento", 50, 158, 700, 34));
        ObjectNode tPT = tablaNode(m, 6, 5, "Plan de Tratamiento", 50, 196, 700, 200);
        setCeldas(tPT, new String[][]{
            {"#", "Diente", "Tratamiento", "Fecha estimada", "Costo"},
            {"1", "", "", "", ""},
            {"2", "", "", "", ""},
            {"3", "", "", "", ""},
            {"4", "", "", "", ""},
            {"5", "", "", "", ""}
        });
        els.add(tPT);

        // Pronóstico
        els.add(elem(m, "TITULO", "XIII. Pronóstico", 50, 406, 700, 34));
        els.add(elem(m, "TEXTO_LARGO", "", 50, 444, 700, 70));

        // Notas de evolución
        els.add(elem(m, "TITULO", "XIV. Notas de Evolución", 50, 524, 700, 34));
        ObjectNode tNE = tablaNode(m, 4, 4, "Notas de Evolución", 50, 562, 700, 160);
        setCeldas(tNE, new String[][]{
            {"Fecha", "Tratamiento realizado", "Observaciones", "Firma/Sello"},
            {"", "", "", ""},
            {"", "", "", ""},
            {"", "", "", ""}
        });
        els.add(tNE);

        // Firma responsable
        ObjectNode firma = m.createObjectNode();
        firma.put("tipo", "AREA_FIRMA");
        firma.put("x", 50); firma.put("y", 732);
        firma.put("w", 700); firma.put("h", 160);
        els.add(firma);

        ObjectNode pag = m.createObjectNode();
        pag.set("elementos", els);
        return pag;
    }

    // ── Helpers para construir JSON de plantilla ─────────────
    private ObjectNode elem(ObjectMapper m, String tipo, String contenido,
                            double x, double y, double w, double h) {
        ObjectNode n = m.createObjectNode();
        n.put("tipo", tipo); n.put("contenido", contenido);
        n.put("x", x); n.put("y", y); n.put("w", w); n.put("h", h);
        return n;
    }

    private ObjectNode tablaNode(ObjectMapper m, int filas, int cols, String titulo,
                                  double x, double y, double w, double h) {
        ObjectNode n = m.createObjectNode();
        n.put("tipo", "TABLA"); n.put("titulo", titulo);
        n.put("x", x); n.put("y", y); n.put("w", w); n.put("h", h);
        n.put("filas", filas); n.put("columnas", cols);
        n.putArray("celdas");
        return n;
    }

    private void setCeldas(ObjectNode tablaNode, String[][] datos) {
        ArrayNode celdas = (ArrayNode) tablaNode.get("celdas");
        ObjectMapper m = new ObjectMapper();
        for (int r = 0; r < datos.length; r++) {
            for (int c = 0; c < datos[r].length; c++) {
                ObjectNode cd = m.createObjectNode();
                cd.put("r", r); cd.put("c", c); cd.put("txt", datos[r][c]);
                celdas.add(cd);
            }
        }
    }

    // ── Helper collapse/expand ────────────────────────────────
    private void toggleColapsar(VBox cuerpo, Button btn) {
        boolean visible = cuerpo.isVisible();
        cuerpo.setVisible(!visible);
        cuerpo.setManaged(!visible);
        btn.setText(visible ? "▼" : "▲");
    }
}