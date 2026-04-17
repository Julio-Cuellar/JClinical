package com.jcode.jclinical.infrastructure.ui.lienzo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SerializacionService {

    private final ObjectMapper mapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════
    //  SERIALIZACIÓN
    // ═══════════════════════════════════════════════════════════

    public ArrayNode serializarElementosActuales(javafx.scene.layout.Pane lienzoCarta) {
        ArrayNode elementos = mapper.createArrayNode();

        for (Node nodo : lienzoCarta.getChildren()) {
            if (esInfraestructura(nodo)) continue;
            ObjectNode el = elementos.addObject();
            el.put("x", round(nodo.getLayoutX()));
            el.put("y", round(nodo.getLayoutY()));
            serializarTamano(nodo, el);
            serializarContenido(nodo, el);
        }
        return elementos;
    }

    private boolean esInfraestructura(Node nodo) {
        return nodo.getStyleClass().contains("guia-visual")
                || nodo.getStyleClass().contains("resize-handle");
    }

    void serializarTamano(Node nodo, ObjectNode el) {
        if (nodo instanceof Region r) {
            el.put("w", round(r.getWidth()  > 0 ? r.getWidth()  : r.prefWidth(-1)));
            el.put("h", round(r.getHeight() > 0 ? r.getHeight() : r.prefHeight(-1)));
        } else if (nodo instanceof ImageView iv) {
            el.put("w", round(iv.getFitWidth()));
            el.put("h", round(iv.getFitHeight()));
        }
    }

    void serializarContenido(Node nodo, ObjectNode el) {
        if (nodo instanceof TextField tf) {
            String tipo = tf.getStyleClass().contains("input-titulo") ? "TITULO" : "TEXTO_CORTO";
            el.put("tipo", tipo);
            el.put("contenido", tf.getText());

        } else if (nodo instanceof TextArea ta) {
            el.put("tipo", "TEXTO_LARGO");
            el.put("contenido", ta.getText());

        } else if (nodo instanceof ImageView iv) {
            el.put("tipo", "IMAGEN");
            String stored = iv.getUserData() != null ? iv.getUserData().toString() : "";
            if (stored.startsWith("data:image/")) el.put("data", stored);
            else                                  el.put("url", stored);

        } else if (nodo.getStyleClass().contains("tabla-container")) {
            serializarTabla(nodo, el);

        } else if (nodo.getStyleClass().contains("odontograma-container")) {
            serializarOdontograma(nodo, el);

        } else if (nodo.getStyleClass().contains("separador-elemento")) {
            el.put("tipo", "SEPARADOR");
            el.put("contenido", "");

        } else if (nodo.getStyleClass().contains("area-firma-container")) {
            el.put("tipo", "AREA_FIRMA");
            el.put("contenido", "");
        }
    }

    // ── Serializar tabla ─────────────────────────────────────────

    private void serializarTabla(Node tableNode, ObjectNode jsonNode) {
        jsonNode.put("tipo", "TABLA");

        // Recuperar título si está guardado
        List<Node> barraLabels = new ArrayList<>();
        buscarNodosPorClase(tableNode, "tabla-barra-label", barraLabels);
        if (!barraLabels.isEmpty()) {
            String textoLabel = ((Label) barraLabels.get(0)).getText();
            String titulo = textoLabel.replace("≡  ", "").replace("≡ ", "").trim();
            jsonNode.put("titulo", titulo);
        }

        List<Node> celdas = new ArrayList<>();
        buscarNodosPorClase(tableNode, "tabla-celda", celdas);

        ArrayNode celdasArray = jsonNode.putArray("celdas");
        int maxR = 0, maxC = 0;

        for (Node c : celdas) {
            Integer r   = GridPane.getRowIndex(c);    if (r == null) r = 0;
            Integer col = GridPane.getColumnIndex(c); if (col == null) col = 0;
            maxR = Math.max(maxR, r);
            maxC = Math.max(maxC, col);

            ObjectNode cNode = celdasArray.addObject();
            cNode.put("r", r);
            cNode.put("c", col);
            cNode.put("txt", ((TextField) c).getText());
        }

        jsonNode.put("filas",    maxR + 1);
        jsonNode.put("columnas", maxC + 1);
    }

    // ── Serializar odontograma ───────────────────────────────────

    private void serializarOdontograma(Node odontoNode, ObjectNode jsonNode) {
        jsonNode.put("tipo", "ODONTOGRAMA");

        // Detectar si es temporal por el título
        List<Node> barraLabels = new ArrayList<>();
        buscarNodosPorClase(odontoNode, "odontograma-barra-label", barraLabels);
        if (!barraLabels.isEmpty()) {
            String text = ((Label) barraLabels.get(0)).getText();
            jsonNode.put("temporal", text.contains("Temporal") || text.contains("Deciduo"));
        }

        List<Node> dientes = new ArrayList<>();
        buscarNodosPorClase(odontoNode, "diente-base", dientes);
        ArrayNode dientesArray = jsonNode.putArray("dientes");

        for (Node d : dientes) {
            StackPane body = (StackPane) d;
            Label lblNum = (Label) body.getChildren().stream()
                    .filter(n -> n instanceof Label l && l.getStyleClass().contains("diente-numero"))
                    .findFirst().orElse(null);
            if (lblNum == null) continue;

            String estado = body.getStyleClass().stream()
                    .filter(s -> s.startsWith("estado-"))
                    .findFirst().orElse("estado-sano");

            // Buscar superficies en el wrapper padre
            Node wrapper = body.getParent();
            ObjectNode supData = serializarSuperficies(wrapper);

            boolean tieneAlgo = !estado.equals("estado-sano") || tieneSuperficiesActivas(supData);
            if (!tieneAlgo) continue;

            ObjectNode dNode = dientesArray.addObject();
            dNode.put("numero", lblNum.getText());
            dNode.put("estado", estado);
            if (supData != null) dNode.set("superficies", supData);
        }
    }

    private ObjectNode serializarSuperficies(Node wrapper) {
        if (wrapper == null) return null;
        ObjectNode supNode = mapper.createObjectNode();
        boolean tieneDatos = false;

        for (String sup : List.of("V", "M", "O", "D", "L")) {
            List<Node> found = new ArrayList<>();
            buscarNodosPorClase(wrapper, "superficie-" + sup, found);
            if (!found.isEmpty()) {
                StackPane cell = (StackPane) found.get(0);
                String estSup = cell.getStyleClass().stream()
                        .filter(s -> s.startsWith("sup-caries") || s.startsWith("sup-amalgama")
                                  || s.startsWith("sup-resina") || s.startsWith("sup-sellador"))
                        .findFirst().orElse("");
                supNode.put(sup, estSup);
                if (!estSup.isEmpty()) tieneDatos = true;
            }
        }
        return tieneDatos ? supNode : null;
    }

    private boolean tieneSuperficiesActivas(ObjectNode supData) {
        if (supData == null) return false;
        var it = supData.elements();
        while (it.hasNext()) { if (!it.next().asText().isEmpty()) return true; }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  RECREAR NODO DESDE JSON
    // ═══════════════════════════════════════════════════════════

    public Node recrearNodo(JsonNode el, ComponenteFactory factory) {
        String tipo = el.has("tipo") ? el.get("tipo").asText() : "";
        String cont = el.has("contenido") ? el.get("contenido").asText() : "";

        return switch (tipo) {
            case "TITULO" -> {
                TextField tf = new TextField(cont);
                tf.getStyleClass().add("input-titulo");
                yield tf;
            }
            case "TEXTO_CORTO" -> {
                TextField tf = new TextField(cont);
                tf.getStyleClass().add("input-texto");
                yield tf;
            }
            case "TEXTO_LARGO" -> {
                TextArea ta = new TextArea(cont);
                ta.getStyleClass().add("input-texto-largo");
                ta.setWrapText(true);
                ta.setPrefRowCount(4);
                yield ta;
            }
            case "IMAGEN"       -> recrearImagen(el);
            case "SEPARADOR"    -> factory.crearSeparador();
            case "AREA_FIRMA"   -> factory.crearAreaFirma();

            case "TABLA", "PRE_ANTECEDENTES", "PRE_CONTACTO",
                 "PRE_SIGNOS_VITALES", "PRE_NOTA_EVOLUCION" -> factory.reconstruirTabla(el);

            case "ODONTOGRAMA"          -> factory.reconstruirOdontograma(el);
            case "ODONTOGRAMA_TEMPORAL" -> factory.reconstruirOdontograma(el);

            default -> null;
        };
    }

    private Node recrearImagen(JsonNode el) {
        if (el.has("data") && !el.get("data").asText().isBlank()) {
            String dataUri = el.get("data").asText();
            ImageView iv   = new ImageView(new Image(dataUri));
            iv.setUserData(dataUri);
            return iv;
        }
        String url = el.has("url") ? el.get("url").asText() : "";
        if (!url.isEmpty() && new File(url).exists()) {
            ImageView iv = new ImageView(new Image(new File(url).toURI().toString()));
            iv.setUserData(url);
            return iv;
        }
        Label ph = new Label("📷  Imagen no disponible");
        ph.getStyleClass().add("image-placeholder");
        return ph;
    }

    // ═══════════════════════════════════════════════════════════
    //  SERIALIZAR UN NODO INDIVIDUAL (para portapapeles)
    // ═══════════════════════════════════════════════════════════

    public ObjectNode serializarNodo(Node nodo) {
        ObjectNode el = mapper.createObjectNode();
        el.put("x", round(nodo.getLayoutX()));
        el.put("y", round(nodo.getLayoutY()));
        serializarTamano(nodo, el);
        serializarContenido(nodo, el);
        return el;
    }

    // ═══════════════════════════════════════════════════════════
    //  IMPORTAR / EXPORTAR JSON
    // ═══════════════════════════════════════════════════════════

    public List<ArrayNode> importarJsonEstructurado(String json) {
        List<ArrayNode> paginas = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            if (root.has("paginas") && root.get("paginas").isArray()) {
                for (JsonNode pag : root.get("paginas")) {
                    if (pag.has("elementos")) paginas.add((ArrayNode) pag.get("elementos"));
                }
            } else if (root.has("elementos")) {
                paginas.add((ArrayNode) root.get("elementos"));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("El JSON del lienzo es inválido.", e);
        }
        if (paginas.isEmpty()) paginas.add(mapper.createArrayNode());
        return paginas;
    }

    public String construirJsonFinal(List<ArrayNode> paginasData) throws Exception {
        ObjectNode root  = mapper.createObjectNode();
        ArrayNode  pags  = root.putArray("paginas");
        for (ArrayNode pag : paginasData) {
            ObjectNode pNode = mapper.createObjectNode();
            pNode.set("elementos", pag);
            pags.add(pNode);
        }
        return mapper.writeValueAsString(root);
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILERÍAS ESTÁTICAS
    // ═══════════════════════════════════════════════════════════

    public static void aplicarTamano(Node n, JsonNode el) {
        if (!el.has("w")) return;
        double w = el.get("w").asDouble();
        double h = el.has("h") ? el.get("h").asDouble() : w;

        if (n instanceof Region r) {
            r.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            r.setPrefSize(w, h);
        } else if (n instanceof ImageView iv) {
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(true);
        }
    }

    public static void buscarNodosPorClase(Node nodo, String clase, List<Node> resultados) {
        if (nodo.getStyleClass().contains(clase)) resultados.add(nodo);
        if (nodo instanceof javafx.scene.Parent p) {
            for (Node hijo : p.getChildrenUnmodifiable()) buscarNodosPorClase(hijo, clase, resultados);
        }
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}