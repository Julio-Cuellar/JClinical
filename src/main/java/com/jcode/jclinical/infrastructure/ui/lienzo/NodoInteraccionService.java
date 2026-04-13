package com.jcode.jclinical.infrastructure.ui.lienzo;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona toda la interacción del usuario con los nodos del lienzo:
 * selección visual, arrastre con snapping magnético, y redimensionado.
 * No sabe nada de persistencia ni de páginas.
 */
public class NodoInteraccionService {

    private static final double SNAP_THRESHOLD = 8.0;
    private static final String EFECTO_SELECCION =
            "-fx-effect: dropshadow(gaussian, #4A8A9C, 8, 0.4, 0, 0);";

    // Nodo actualmente seleccionado
    private Node nodoSeleccionado;

    // Handle de resize (esquina inferior derecha)
    private final Rectangle resizeHandle;

    // Guías de alineación inteligente
    private final Line guideX;
    private final Line guideY;

    // Estado del arrastre
    private double mouseAnchorX;
    private double mouseAnchorY;
    private double startW;
    private double startH;

    // Listeners para mover el handle cuando el nodo cambia de posición/tamaño
    private ChangeListener<Bounds> boundsListener;
    private ChangeListener<Number> posListener;

    public NodoInteraccionService() {
        this.guideX = crearGuia();
        this.guideY = crearGuia();
        this.resizeHandle = crearResizeHandle();
        inicializarListeners();
    }

    // --- Inicialización ---

    private Line crearGuia() {
        Line guia = new Line();
        guia.setStroke(Color.web("#FF6B6B"));
        guia.setStrokeWidth(1.5);
        guia.getStrokeDashArray().addAll(5d, 5d);
        guia.setVisible(false);
        guia.getStyleClass().add("guia-visual");
        return guia;
    }

    private Rectangle crearResizeHandle() {
        Rectangle handle = new Rectangle(12, 12, Color.web("#4A8A9C"));
        handle.setCursor(Cursor.SE_RESIZE);
        handle.setVisible(false);
        handle.getStyleClass().add("resize-handle");
        return handle;
    }

    private void inicializarListeners() {
        boundsListener = (obs, oldB, newB) -> actualizarPosicionHandle();
        posListener = (obs, oldV, newV) -> actualizarPosicionHandle();
    }

    /**
     * Registra los eventos de resize en el handle y el click en el lienzo
     * para deseleccionar. Debe llamarse una vez desde initialize().
     */
    public void configurarEnLienzo(Pane lienzoCarta) {
        lienzoCarta.getChildren().addAll(guideX, guideY, resizeHandle);
        lienzoCarta.setOnMouseClicked(e -> {
            if (e.getTarget() == lienzoCarta) deseleccionarNodo();
        });

        resizeHandle.setOnMousePressed(e -> {
            if (nodoSeleccionado != null) {
                mouseAnchorX = e.getSceneX();
                mouseAnchorY = e.getSceneY();
                if (nodoSeleccionado instanceof Region r) {
                    startW = r.getWidth();
                    startH = r.getHeight();
                } else if (nodoSeleccionado instanceof ImageView iv) {
                    startW = iv.getFitWidth();
                    startH = iv.getFitHeight();
                }
            }
            e.consume();
        });

        resizeHandle.setOnMouseDragged(e -> {
            if (nodoSeleccionado != null) {
                double newW = Math.max(50, startW + (e.getSceneX() - mouseAnchorX));
                double newH = Math.max(30, startH + (e.getSceneY() - mouseAnchorY));
                if (nodoSeleccionado instanceof Region r) {
                    r.setPrefSize(newW, newH);
                } else if (nodoSeleccionado instanceof ImageView iv) {
                    iv.setFitWidth(newW);
                    iv.setFitHeight(newH);
                }
            }
            e.consume();
        });
    }

    // --- Selección ---

    public void seleccionarNodo(Node nodo) {
        deseleccionarNodo();
        nodoSeleccionado = nodo;
        nodoSeleccionado.boundsInParentProperty().addListener(boundsListener);
        nodoSeleccionado.layoutXProperty().addListener(posListener);
        nodoSeleccionado.layoutYProperty().addListener(posListener);
        nodoSeleccionado.setStyle(nodoSeleccionado.getStyle() + EFECTO_SELECCION);
        resizeHandle.setVisible(true);
        actualizarPosicionHandle();
    }

    public void deseleccionarNodo() {
        if (nodoSeleccionado != null) {
            nodoSeleccionado.boundsInParentProperty().removeListener(boundsListener);
            nodoSeleccionado.layoutXProperty().removeListener(posListener);
            nodoSeleccionado.layoutYProperty().removeListener(posListener);
            nodoSeleccionado.setStyle(
                    nodoSeleccionado.getStyle().replace(EFECTO_SELECCION, ""));
        }
        nodoSeleccionado = null;
        resizeHandle.setVisible(false);
    }

    private void actualizarPosicionHandle() {
        if (nodoSeleccionado != null) {
            Bounds b = nodoSeleccionado.getBoundsInParent();
            resizeHandle.setLayoutX(b.getMaxX() - 6);
            resizeHandle.setLayoutY(b.getMaxY() - 6);
            resizeHandle.toFront();
        }
    }

    // --- Lógica de interacción por nodo ---

    /**
     * Aplica todos los handlers de mouse/teclado a un nodo recién añadido
     * o recargado al lienzo.
     */
    public void aplicarLogicaNodo(Node nodo, String tipo, Pane lienzoCarta) {
        nodo.setFocusTraversable(true);

        nodo.setOnMouseClicked(e -> {
            nodo.requestFocus();
            seleccionarNodo(nodo);
            // Clic derecho doble = eliminar
            if (e.getButton() == MouseButton.SECONDARY && e.getClickCount() == 2) {
                deseleccionarNodo();
                lienzoCarta.getChildren().remove(nodo);
            }
            e.consume();
        });

        nodo.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                deseleccionarNodo();
                lienzoCarta.getChildren().remove(nodo);
            }
        });

        nodo.setOnMouseReleased(e -> ocultarGuias());

        // Las tablas y odontogramas se arrastran desde su barra de título (primer hijo)
        boolean esContenedor = tipo.contains("TABLA") || tipo.contains("ANTECEDENTES")
                || tipo.contains("CONTACTO") || tipo.equals("ODONTOGRAMA");
        Node handleDrag = (esContenedor && nodo instanceof VBox vb && !vb.getChildren().isEmpty())
                ? vb.getChildren().get(0)
                : nodo;

        handleDrag.setOnMousePressed(e -> {
            nodo.toFront();
            resizeHandle.toFront();
            seleccionarNodo(nodo);
            mouseAnchorX = e.getSceneX() - nodo.getLayoutX();
            mouseAnchorY = e.getSceneY() - nodo.getLayoutY();
            e.consume();
        });

        handleDrag.setOnMouseDragged(e -> {
            aplicarArrastreConGuias(nodo,
                    e.getSceneX() - mouseAnchorX,
                    e.getSceneY() - mouseAnchorY,
                    lienzoCarta);
            e.consume();
        });
    }

    // --- Snapping y guías inteligentes ---

    private void aplicarArrastreConGuias(Node nodo, double proposedX, double proposedY,
                                         Pane lienzoCarta) {
        double finalX = proposedX, finalY = proposedY;
        boolean snapX = false, snapY = false;

        Bounds b = nodo.getBoundsInParent();
        double w = b.getWidth(), h = b.getHeight();
        double canvasCX = lienzoCarta.getWidth() / 2;
        double canvasCY = lienzoCarta.getHeight() / 2;

        // Snap al centro del lienzo
        if (Math.abs((proposedX + w / 2) - canvasCX) < SNAP_THRESHOLD) {
            finalX = canvasCX - w / 2;
            snapX = true;
            mostrarGuiaY(canvasCX, lienzoCarta);
        }
        if (Math.abs((proposedY + h / 2) - canvasCY) < SNAP_THRESHOLD) {
            finalY = canvasCY - h / 2;
            snapY = true;
            mostrarGuiaX(canvasCY, lienzoCarta);
        }

        // Snap a bordes de otros nodos
        if (!snapX || !snapY) {
            List<Node> otros = new ArrayList<>(lienzoCarta.getChildren());
            for (Node o : otros) {
                if (o == nodo || o == resizeHandle || o == guideX || o == guideY) continue;
                Bounds ob = o.getBoundsInParent();

                if (!snapX) {
                    if (Math.abs(proposedX - ob.getMinX()) < SNAP_THRESHOLD) {
                        finalX = ob.getMinX(); snapX = true; mostrarGuiaY(ob.getMinX(), lienzoCarta);
                    } else if (Math.abs(proposedX + w - ob.getMaxX()) < SNAP_THRESHOLD) {
                        finalX = ob.getMaxX() - w; snapX = true; mostrarGuiaY(ob.getMaxX(), lienzoCarta);
                    }
                }
                if (!snapY) {
                    if (Math.abs(proposedY - ob.getMinY()) < SNAP_THRESHOLD) {
                        finalY = ob.getMinY(); snapY = true; mostrarGuiaX(ob.getMinY(), lienzoCarta);
                    } else if (Math.abs(proposedY + h - ob.getMaxY()) < SNAP_THRESHOLD) {
                        finalY = ob.getMaxY() - h; snapY = true; mostrarGuiaX(ob.getMaxY(), lienzoCarta);
                    }
                }
            }
        }

        if (!snapX) guideY.setVisible(false);
        if (!snapY) guideX.setVisible(false);

        nodo.setLayoutX(finalX);
        nodo.setLayoutY(finalY);
        actualizarPosicionHandle();
    }

    private void mostrarGuiaY(double x, Pane lienzoCarta) {
        guideY.setStartX(x); guideY.setEndX(x);
        guideY.setStartY(0); guideY.setEndY(lienzoCarta.getHeight());
        guideY.setVisible(true); guideY.toFront();
    }

    private void mostrarGuiaX(double y, Pane lienzoCarta) {
        guideX.setStartX(0); guideX.setEndX(lienzoCarta.getWidth());
        guideX.setStartY(y); guideX.setEndY(y);
        guideX.setVisible(true); guideX.toFront();
    }

    private void ocultarGuias() {
        guideX.setVisible(false);
        guideY.setVisible(false);
    }

    // --- Getters para que PaginacionManager pueda reponer guías tras limpiar el lienzo ---

    public Line getGuideX() { return guideX; }
    public Line getGuideY() { return guideY; }
    public Rectangle getResizeHandle() { return resizeHandle; }
}