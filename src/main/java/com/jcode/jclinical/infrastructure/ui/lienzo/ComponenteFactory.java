package com.jcode.jclinical.infrastructure.ui.lienzo;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ComponenteFactory {

    private Window ownerWindow;

    public ComponenteFactory(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    public Node crearTabla(int filas, int columnas, String titulo) {
        VBox contenedor = new VBox(0);
        contenedor.getStyleClass().add("tabla-container");

        HBox barra = new HBox(new Label("≡ " + titulo));
        barra.setStyle("-fx-background-color: #E0ECEF; -fx-padding: 5; -fx-cursor: move;");

        VBox cuerpo = new VBox(8);
        cuerpo.setStyle("-fx-background-color: #FAFCFD; -fx-padding: 10;"
                + "-fx-border-color: #A8DADC; -fx-border-width: 0 2px 2px 2px;");
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        VBox.setVgrow(grid, Priority.ALWAYS);

        final int[] rows = {filas};
        final int[] cols = {columnas};

        Runnable actualizarConstraints = () -> {
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
                rc.setPercentHeight(100.0 / rows[0]);
                rc.setVgrow(Priority.ALWAYS);
                grid.getRowConstraints().add(rc);
            }
        };

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                grid.add(crearCeldaTabla(), j, i);
            }
        }
        actualizarConstraints.run();

        HBox controles = new HBox(5);
        Button btnAddCol = new Button("+ Col");
        Button btnRemCol = new Button("- Col");
        Button btnAddFila = new Button("+ Fila");
        Button btnRemFila = new Button("- Fila");

        btnAddCol.setOnAction(e -> {
            for (int i = 0; i < rows[0]; i++) grid.add(crearCeldaTabla(), cols[0], i);
            cols[0]++;
            actualizarConstraints.run();
        });
        btnRemCol.setOnAction(e -> {
            if (cols[0] > 1) {
                cols[0]--;
                int target = cols[0];
                grid.getChildren().removeIf(n -> GridPane.getColumnIndex(n) != null
                        && GridPane.getColumnIndex(n) == target);
                actualizarConstraints.run();
            }
        });
        btnAddFila.setOnAction(e -> {
            for (int j = 0; j < cols[0]; j++) grid.add(crearCeldaTabla(), j, rows[0]);
            rows[0]++;
            actualizarConstraints.run();
        });
        btnRemFila.setOnAction(e -> {
            if (rows[0] > 1) {
                rows[0]--;
                int target = rows[0];
                grid.getChildren().removeIf(n -> GridPane.getRowIndex(n) != null
                        && GridPane.getRowIndex(n) == target);
                actualizarConstraints.run();
            }
        });

        controles.getChildren().addAll(btnAddCol, btnRemCol, btnAddFila, btnRemFila);
        cuerpo.getChildren().addAll(controles, grid);
        contenedor.getChildren().addAll(barra, cuerpo);
        return contenedor;
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
        int filas = json.get("filas").asInt();
        int cols = json.get("columnas").asInt();
        Node tabla = crearTabla(filas, cols, "Tabla Cargada");

        if (json.has("celdas")) {
            List<Node> celdas = new ArrayList<>();
            SerializacionService.buscarNodosPorClase(tabla, "tabla-celda", celdas);

            for (JsonNode cd : json.get("celdas")) {
                int row = cd.get("r").asInt();
                int col = cd.get("c").asInt();
                for (Node vc : celdas) {
                    Integer r = GridPane.getRowIndex(vc);
                    Integer c = GridPane.getColumnIndex(vc);
                    if (r == null) r = 0;
                    if (c == null) c = 0;
                    if (r == row && c == col) {
                        ((TextField) vc).setText(cd.get("txt").asText());
                        break;
                    }
                }
            }
        }
        return tabla;
    }

    public Node crearOdontograma() {
        VBox contenedor = new VBox(0);
        contenedor.getStyleClass().add("odontograma-container");

        HBox barra = new HBox(new Label("≡ Mover Odontograma"));
        barra.setStyle("-fx-background-color: #E0ECEF; -fx-padding: 5; -fx-cursor: move;");

        VBox cuerpo = new VBox(10);
        cuerpo.setStyle("-fx-background-color: white; -fx-padding: 10;"
                + "-fx-border-color: #A8DADC; -fx-border-width: 0 2px 2px 2px;");
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        HBox hilera1 = new HBox(2);
        HBox hilera2 = new HBox(2);
        VBox.setVgrow(hilera1, Priority.ALWAYS);
        VBox.setVgrow(hilera2, Priority.ALWAYS);

        for (int i = 18; i >= 11; i--) hilera1.getChildren().add(crearOrganoDental(i));
        for (int i = 21; i <= 28; i++) hilera1.getChildren().add(crearOrganoDental(i));
        for (int i = 48; i >= 41; i--) hilera2.getChildren().add(crearOrganoDental(i));
        for (int i = 31; i <= 38; i++) hilera2.getChildren().add(crearOrganoDental(i));

        cuerpo.getChildren().addAll(hilera1, hilera2);
        contenedor.getChildren().addAll(barra, cuerpo);
        return contenedor;
    }

    private Node crearOrganoDental(int numero) {
        javafx.scene.layout.StackPane sp = new javafx.scene.layout.StackPane();
        sp.getStyleClass().addAll("diente-base", "estado-sano");
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lbl = new Label(String.valueOf(numero));
        lbl.getStyleClass().add("diente-numero");
        sp.getChildren().add(lbl);

        sp.setOnMouseClicked(e -> {
            ContextMenu menu = new ContextMenu();
            MenuItem sano = new MenuItem("Sano");
            MenuItem caries = new MenuItem("Caries");
            MenuItem extraccion = new MenuItem("Extracción");

            sano.setOnAction(ev -> cambiarEstadoDiente(sp, lbl, "estado-sano", false, ""));
            caries.setOnAction(ev -> cambiarEstadoDiente(sp, lbl, "estado-caries", false, ""));
            extraccion.setOnAction(ev -> cambiarEstadoDiente(sp, lbl, "estado-extraccion", true, "#E74C3C"));

            menu.getItems().addAll(sano, caries, extraccion);
            menu.show(sp, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        return sp;
    }

    private void cambiarEstadoDiente(javafx.scene.layout.StackPane sp, Label lbl,
                                     String nuevoEstado, boolean mostrarAspa, String colorAspa) {
        sp.getStyleClass().removeAll("estado-sano", "estado-caries",
                "estado-extraccion", "estado-perdido");
        sp.getChildren().removeIf(n -> n instanceof Label && !n.equals(lbl));
        sp.getStyleClass().add(nuevoEstado);

        if (mostrarAspa) {
            Label aspa = new Label("X");
            aspa.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + colorAspa + ";");
            sp.getChildren().add(aspa);
        }
    }

    public Node reconstruirOdontograma(JsonNode json) {
        Node odonto = crearOdontograma();

        if (json.has("dientes")) {
            List<Node> dientes = new ArrayList<>();
            SerializacionService.buscarNodosPorClase(odonto, "diente-base", dientes);

            for (JsonNode d : json.get("dientes")) {
                String numero = d.get("numero").asText();
                String estado = d.get("estado").asText();

                for (Node dv : dientes) {
                    javafx.scene.layout.StackPane sp = (javafx.scene.layout.StackPane) dv;
                    Label lbl = (Label) sp.getChildren().get(0);
                    if (lbl.getText().equals(numero)) {
                        cambiarEstadoDiente(sp, lbl, estado,
                                estado.contains("extr"), "#E74C3C");
                        break;
                    }
                }
            }
        }
        return odonto;
    }

    public ImageView solicitarImagen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Cargar Imagen");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        File f = fc.showOpenDialog(ownerWindow);

        if (f == null) {
            return null;
        }

        try {
            String mimeType = Files.probeContentType(f.toPath());
            if (mimeType == null) {
                mimeType = inferMimeType(f.getName());
            }
            String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath()));
            String dataUri = "data:" + mimeType + ";base64," + base64;

            ImageView iv = new ImageView(new Image(dataUri));
            iv.setFitWidth(200);
            iv.setPreserveRatio(true);
            iv.setUserData(dataUri);
            return iv;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la imagen seleccionada.", e);
        }
    }

    private String inferMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "application/octet-stream";
    }
}
