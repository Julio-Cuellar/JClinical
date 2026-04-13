package com.jcode.jclinical.infrastructure.ui.lienzo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SerializacionService {

    private final ObjectMapper mapper = new ObjectMapper();

    public ArrayNode serializarElementosActuales(javafx.scene.layout.Pane lienzoCarta) {
        ArrayNode elementos = mapper.createArrayNode();

        for (Node nodo : lienzoCarta.getChildren()) {
            if (esInfraestructuraVisual(nodo)) continue;

            ObjectNode el = elementos.addObject();
            el.put("x", round(nodo.getLayoutX()));
            el.put("y", round(nodo.getLayoutY()));
            serializarTamano(nodo, el);
            serializarContenido(nodo, el);
        }

        return elementos;
    }

    private boolean esInfraestructuraVisual(Node nodo) {
        return nodo.getStyleClass().contains("guia-visual")
                || nodo.getStyleClass().contains("resize-handle");
    }

    private void serializarTamano(Node nodo, ObjectNode el) {
        if (nodo instanceof Region r) {
            el.put("w", round(r.getWidth() > 0 ? r.getWidth() : r.prefWidth(-1)));
            el.put("h", round(r.getHeight() > 0 ? r.getHeight() : r.prefHeight(-1)));
        } else if (nodo instanceof ImageView iv) {
            el.put("w", round(iv.getFitWidth()));
            el.put("h", round(iv.getFitHeight()));
        }
    }

    private void serializarContenido(Node nodo, ObjectNode el) {
        if (nodo instanceof TextField tf) {
            String tipo = tf.getStyleClass().contains("input-titulo") ? "TITULO" : "TEXTO_CORTO";
            el.put("tipo", tipo);
            el.put("contenido", tf.getText());
        } else if (nodo instanceof ImageView iv) {
            el.put("tipo", "IMAGEN");
            String stored = iv.getUserData() != null ? iv.getUserData().toString() : "";
            if (stored.startsWith("data:image/")) {
                el.put("data", stored);
            } else {
                el.put("url", stored);
            }
        } else if (nodo.getStyleClass().contains("tabla-container")) {
            el.put("tipo", "TABLA");
            serializarTablaInterna(nodo, el);
        } else if (nodo.getStyleClass().contains("odontograma-container")) {
            el.put("tipo", "ODONTOGRAMA");
            serializarOdontogramaInterno(nodo, el);
        }
    }

    private void serializarTablaInterna(Node tableNode, ObjectNode jsonNode) {
        List<Node> celdas = new ArrayList<>();
        buscarNodosPorClase(tableNode, "tabla-celda", celdas);

        ArrayNode celdasArray = jsonNode.putArray("celdas");
        int maxR = 0, maxC = 0;

        for (Node c : celdas) {
            Integer r = GridPane.getRowIndex(c);
            Integer col = GridPane.getColumnIndex(c);
            if (r == null) r = 0;
            if (col == null) col = 0;

            maxR = Math.max(maxR, r);
            maxC = Math.max(maxC, col);

            ObjectNode cNode = celdasArray.addObject();
            cNode.put("r", r);
            cNode.put("c", col);
            cNode.put("txt", ((TextField) c).getText());
        }

        jsonNode.put("filas", maxR + 1);
        jsonNode.put("columnas", maxC + 1);
    }

    private void serializarOdontogramaInterno(Node odontoNode, ObjectNode jsonNode) {
        List<Node> dientes = new ArrayList<>();
        buscarNodosPorClase(odontoNode, "diente-base", dientes);
        ArrayNode dientesArray = jsonNode.putArray("dientes");

        for (Node d : dientes) {
            javafx.scene.layout.StackPane sp = (javafx.scene.layout.StackPane) d;
            Label lblNum = (Label) sp.getChildren().stream()
                    .filter(n -> n instanceof Label label && label.getStyleClass().contains("diente-numero"))
                    .findFirst()
                    .orElse(null);

            if (lblNum == null) continue;

            String estado = sp.getStyleClass().stream()
                    .filter(s -> s.startsWith("estado-"))
                    .findFirst()
                    .orElse("estado-sano");

            if (!estado.equals("estado-sano")) {
                ObjectNode dNode = dientesArray.addObject();
                dNode.put("numero", lblNum.getText());
                dNode.put("estado", estado);
            }
        }
    }

    public Node recrearNodo(JsonNode el, ComponenteFactory factory) {
        String tipo = el.get("tipo").asText();
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
            case "IMAGEN" -> recrearImagen(el);
            case "TABLA", "PRE_ANTECEDENTES", "PRE_CONTACTO" -> factory.reconstruirTabla(el);
            case "ODONTOGRAMA" -> factory.reconstruirOdontograma(el);
            default -> null;
        };
    }

    private Node recrearImagen(JsonNode el) {
        if (el.has("data") && !el.get("data").asText().isBlank()) {
            String dataUri = el.get("data").asText();
            ImageView iv = new ImageView(new Image(dataUri));
            iv.setUserData(dataUri);
            return iv;
        }

        String url = el.has("url") ? el.get("url").asText() : "";
        if (!url.isEmpty() && new File(url).exists()) {
            ImageView iv = new ImageView(new Image(new File(url).toURI().toString()));
            iv.setUserData(url);
            return iv;
        }

        Label placeholder = new Label("📷 Imagen no disponible");
        placeholder.getStyleClass().add("image-placeholder");
        return placeholder;
    }

    public List<ArrayNode> importarJsonEstructurado(String json) {
        List<ArrayNode> paginas = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);

            if (root.has("paginas") && root.get("paginas").isArray()) {
                for (JsonNode pag : root.get("paginas")) {
                    paginas.add((ArrayNode) pag.get("elementos"));
                }
            } else if (root.has("elementos")) {
                paginas.add((ArrayNode) root.get("elementos"));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("El JSON del lienzo es inválido.", e);
        }

        if (paginas.isEmpty()) {
            paginas.add(mapper.createArrayNode());
        }
        return paginas;
    }

    public String construirJsonFinal(List<ArrayNode> paginasData) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode paginasArray = root.putArray("paginas");

        for (ArrayNode pag : paginasData) {
            ObjectNode pNode = mapper.createObjectNode();
            pNode.set("elementos", pag);
            paginasArray.add(pNode);
        }

        return mapper.writeValueAsString(root);
    }

    public static void aplicarTamano(Node n, JsonNode el) {
        if (n instanceof Region r && el.has("w")) {
            double width = el.get("w").asDouble();
            double height = el.get("h").asDouble();
            r.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            r.setPrefSize(width, height);
        } else if (n instanceof ImageView iv && el.has("w")) {
            iv.setFitWidth(el.get("w").asDouble());
            iv.setFitHeight(el.get("h").asDouble());
            iv.setPreserveRatio(true);
        }
    }

    public static void buscarNodosPorClase(Node nodo, String claseCss, List<Node> resultados) {
        if (nodo.getStyleClass().contains(claseCss)) resultados.add(nodo);
        if (nodo instanceof javafx.scene.Parent p) {
            for (Node hijo : p.getChildrenUnmodifiable()) {
                buscarNodosPorClase(hijo, claseCss, resultados);
            }
        }
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
