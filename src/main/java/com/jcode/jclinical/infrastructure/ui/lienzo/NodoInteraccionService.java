package com.jcode.jclinical.infrastructure.ui.lienzo;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Gestiona toda la interacción del usuario con los nodos del lienzo:
 * selección, arrastre con snapping magnético, redimensionado,
 * menú contextual y portapapeles interno.
 *
 * Guías de alineación: turquesa (#7ECECE) en lugar de rojo, más visibles.
 * Snap magnético: centro del lienzo + márgenes estándar + bordes de otros nodos.
 */
public class NodoInteraccionService {

    private static final double SNAP_THRESHOLD  = 10.0;
    private static final double MARGEN_LIENZO   = 50.0;
    private static final String EFECTO_SELECCION =
        "-fx-effect: dropshadow(gaussian, #5BBDBD, 10, 0.45, 0, 0);";

    // ── Nodo actualmente seleccionado ───────────────────────────
    private Node nodoSeleccionado;
    private String tipoNodoSeleccionado = "";

    // ── Infraestructura visual ──────────────────────────────────
    private final Rectangle resizeHandle;
    private final Line guideX;  // horizontal
    private final Line guideY;  // vertical

    // ── Estado del arrastre y resize ───────────────────────────
    private double mouseAnchorX, mouseAnchorY;
    private double startW, startH;
    private boolean arrastrando = false;

    // ── Listeners para sincronizar el handle ───────────────────
    private ChangeListener<Bounds>  boundsListener;
    private ChangeListener<Number>  posListener;

    // ── Portapapeles interno ────────────────────────────────────
    private String clipboardJson = null;

    // ── Callbacks para operaciones de portapapeles ──────────────
    private BiConsumer<Node, String> onCopiarCallback;
    private BiConsumer<Node, String> onCortarCallback;
    private BiConsumer<Node, String> onDuplicarCallback;
    private Runnable                 onPegarCallback;
    private Runnable                 onSnapshotCallback;   // notificar al controller antes de mover

    // ───────────────────────────────────────────────────────────

    public NodoInteraccionService() {
        this.guideX      = crearGuia();
        this.guideY      = crearGuia();
        this.resizeHandle = crearResizeHandle();
        inicializarListeners();
    }

    // ── Fábricas ────────────────────────────────────────────────

    private Line crearGuia() {
        Line guia = new Line();
        guia.setStroke(Color.web("#7ECECE"));
        guia.setStrokeWidth(1.5);
        guia.getStrokeDashArray().addAll(8d, 5d);
        guia.setVisible(false);
        guia.getStyleClass().add("guia-visual");
        guia.setOpacity(0.85);
        return guia;
    }

    private Rectangle crearResizeHandle() {
        Rectangle h = new Rectangle(13, 13, Color.web("#5BBDBD"));
        h.setArcWidth(3); h.setArcHeight(3);
        h.setCursor(Cursor.SE_RESIZE);
        h.setVisible(false);
        h.getStyleClass().add("resize-handle");
        h.setOpacity(0.9);
        return h;
    }

    private void inicializarListeners() {
        boundsListener = (obs, o, n) -> actualizarPosicionHandle();
        posListener    = (obs, o, n) -> actualizarPosicionHandle();
    }

    // ── Configuración en el lienzo ──────────────────────────────

    public void configurarEnLienzo(Pane lienzoCarta) {
        lienzoCarta.getChildren().addAll(guideX, guideY, resizeHandle);

        // Deseleccionar al clic en el lienzo vacío
        lienzoCarta.setOnMouseClicked(e -> {
            if (e.getTarget() == lienzoCarta) deseleccionarNodo();
        });

        // Handle de resize
        resizeHandle.setOnMousePressed(e -> {
            if (nodoSeleccionado != null) {
                if (onSnapshotCallback != null) onSnapshotCallback.run();
                mouseAnchorX = e.getSceneX();
                mouseAnchorY = e.getSceneY();
                if (nodoSeleccionado instanceof Region r) {
                    startW = r.getWidth(); startH = r.getHeight();
                } else if (nodoSeleccionado instanceof ImageView iv) {
                    startW = iv.getFitWidth(); startH = iv.getFitHeight();
                }
            }
            e.consume();
        });

        resizeHandle.setOnMouseDragged(e -> {
            if (nodoSeleccionado != null) {
                double newW = Math.max(60,  startW + (e.getSceneX() - mouseAnchorX));
                double newH = Math.max(30,  startH + (e.getSceneY() - mouseAnchorY));
                if (nodoSeleccionado instanceof Region r) r.setPrefSize(newW, newH);
                else if (nodoSeleccionado instanceof ImageView iv) {
                    iv.setFitWidth(newW); iv.setFitHeight(newH);
                }
            }
            e.consume();
        });

        resizeHandle.setOnMouseReleased(e -> e.consume());
    }

    // ── Selección ───────────────────────────────────────────────

    public void seleccionarNodo(Node nodo) {
        deseleccionarNodo();
        nodoSeleccionado = nodo;
        nodo.boundsInParentProperty().addListener(boundsListener);
        nodo.layoutXProperty().addListener(posListener);
        nodo.layoutYProperty().addListener(posListener);
        String estiloActual = nodo.getStyle() == null ? "" : nodo.getStyle();
        nodo.setStyle(estiloActual + EFECTO_SELECCION);
        resizeHandle.setVisible(true);
        actualizarPosicionHandle();
    }

    public void deseleccionarNodo() {
        if (nodoSeleccionado != null) {
            nodoSeleccionado.boundsInParentProperty().removeListener(boundsListener);
            nodoSeleccionado.layoutXProperty().removeListener(posListener);
            nodoSeleccionado.layoutYProperty().removeListener(posListener);
            String estilo = nodoSeleccionado.getStyle();
            if (estilo != null) {
                nodoSeleccionado.setStyle(estilo.replace(EFECTO_SELECCION, ""));
            }
        }
        nodoSeleccionado = null;
        tipoNodoSeleccionado = "";
        resizeHandle.setVisible(false);
    }

    public Node getNodoSeleccionado() { return nodoSeleccionado; }
    public String getTipoNodoSeleccionado() { return tipoNodoSeleccionado; }

    private void actualizarPosicionHandle() {
        if (nodoSeleccionado != null) {
            Bounds b = nodoSeleccionado.getBoundsInParent();
            resizeHandle.setLayoutX(b.getMaxX() - 7);
            resizeHandle.setLayoutY(b.getMaxY() - 7);
            resizeHandle.toFront();
        }
    }

    // ── Lógica de interacción por nodo ──────────────────────────

    public void aplicarLogicaNodo(Node nodo, String tipo, Pane lienzoCarta) {
        nodo.setFocusTraversable(true);

        // Clic izquierdo → seleccionar
        nodo.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                nodo.requestFocus();
                seleccionarNodo(nodo);
                tipoNodoSeleccionado = tipo;
            }
            e.consume();
        });

        // Menú contextual con clic derecho ÚNICO
        nodo.setOnContextMenuRequested(e -> {
            seleccionarNodo(nodo);
            tipoNodoSeleccionado = tipo;
            mostrarMenuContextual(nodo, tipo, lienzoCarta, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // Tecla Delete → eliminar (no interceptar Backspace para no romper TextFields)
        nodo.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.DELETE) {
                deseleccionarNodo();
                lienzoCarta.getChildren().remove(nodo);
                e.consume();
            }
        });

        // Soltar mouse → ocultar guías
        nodo.setOnMouseReleased(e -> ocultarGuias());

        // Determinar el handle de arrastre
        boolean esContenedor = tipo.contains("TABLA") || tipo.contains("ANTECEDENTES")
                || tipo.contains("CONTACTO") || tipo.contains("ODONTOGRAMA")
                || tipo.contains("FIRMA") || tipo.contains("SIGNOS") || tipo.contains("NOTA");

        Node handleDrag = (esContenedor && nodo instanceof VBox vb && !vb.getChildren().isEmpty())
                ? vb.getChildren().get(0)
                : nodo;

        handleDrag.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (onSnapshotCallback != null) onSnapshotCallback.run();
                nodo.toFront();
                resizeHandle.toFront();
                seleccionarNodo(nodo);
                tipoNodoSeleccionado = tipo;
                mouseAnchorX = e.getSceneX() - nodo.getLayoutX();
                mouseAnchorY = e.getSceneY() - nodo.getLayoutY();
                arrastrando = true;
            }
            e.consume();
        });

        handleDrag.setOnMouseDragged(e -> {
            if (arrastrando && e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                aplicarArrastreConGuias(nodo,
                        e.getSceneX() - mouseAnchorX,
                        e.getSceneY() - mouseAnchorY,
                        lienzoCarta);
            }
            e.consume();
        });

        handleDrag.setOnMouseReleased(e -> {
            arrastrando = false;
            ocultarGuias();
        });
    }

    // ── Menú contextual ─────────────────────────────────────────

    private void mostrarMenuContextual(Node nodo, String tipo, Pane lienzoCarta,
                                        double screenX, double screenY) {
        ContextMenu menu = new ContextMenu();

        // Pegar (sólo si hay contenido en portapapeles)
        if (clipboardJson != null) {
            MenuItem pegar = new MenuItem("📌  Pegar");
            pegar.setOnAction(e -> { if (onPegarCallback != null) onPegarCallback.run(); });
            menu.getItems().add(pegar);
            menu.getItems().add(new SeparatorMenuItem());
        }

        MenuItem copiar   = new MenuItem("📋  Copiar            Ctrl+C");
        MenuItem cortar   = new MenuItem("✂    Cortar            Ctrl+X");
        MenuItem duplicar = new MenuItem("📄  Duplicar          Ctrl+D");
        MenuItem sep      = new SeparatorMenuItem();
        MenuItem eliminar = new MenuItem("🗑  Eliminar          Supr");

        copiar.setOnAction(e -> { if (onCopiarCallback != null)   onCopiarCallback.accept(nodo, tipo); });
        cortar.setOnAction(e -> { if (onCortarCallback != null)   onCortarCallback.accept(nodo, tipo); });
        duplicar.setOnAction(e -> { if (onDuplicarCallback != null) onDuplicarCallback.accept(nodo, tipo); });
        eliminar.setOnAction(e -> {
            deseleccionarNodo();
            lienzoCarta.getChildren().remove(nodo);
        });

        // Estilo de "peligro" en Eliminar
        eliminar.setStyle("-fx-text-fill: #E74C3C;");

        menu.getItems().addAll(copiar, cortar, duplicar, sep, eliminar);
        menu.show(nodo, screenX, screenY);
    }

    // ── Snapping y guías inteligentes ───────────────────────────

    private void aplicarArrastreConGuias(Node nodo, double propX, double propY,
                                          Pane lienzoCarta) {
        double finalX = propX, finalY = propY;
        boolean snapX = false, snapY = false;

        Bounds b = nodo.getBoundsInParent();
        double w = b.getWidth(), h = b.getHeight();
        double cW = lienzoCarta.getWidth(),  cH = lienzoCarta.getHeight();
        double cX = cW / 2,                  cY = cH / 2;

        // ── 1. Snap al centro del lienzo ───────────────────────
        if (!snapX && Math.abs((propX + w / 2) - cX) < SNAP_THRESHOLD) {
            finalX = cX - w / 2; snapX = true; mostrarGuiaY(cX, lienzoCarta);
        }
        if (!snapY && Math.abs((propY + h / 2) - cY) < SNAP_THRESHOLD) {
            finalY = cY - h / 2; snapY = true; mostrarGuiaX(cY, lienzoCarta);
        }

        // ── 2. Snap a márgenes del lienzo (izq/der/sup/inf) ───
        if (!snapX && Math.abs(propX - MARGEN_LIENZO) < SNAP_THRESHOLD) {
            finalX = MARGEN_LIENZO; snapX = true;
            mostrarGuiaY(MARGEN_LIENZO, lienzoCarta);
        }
        if (!snapX && Math.abs((propX + w) - (cW - MARGEN_LIENZO)) < SNAP_THRESHOLD) {
            finalX = cW - MARGEN_LIENZO - w; snapX = true;
            mostrarGuiaY(cW - MARGEN_LIENZO, lienzoCarta);
        }
        if (!snapY && Math.abs(propY - MARGEN_LIENZO) < SNAP_THRESHOLD) {
            finalY = MARGEN_LIENZO; snapY = true;
            mostrarGuiaX(MARGEN_LIENZO, lienzoCarta);
        }

        // ── 3. Snap a bordes de otros nodos ────────────────────
        if (!snapX || !snapY) {
            for (Node o : new ArrayList<>(lienzoCarta.getChildren())) {
                if (o == nodo || o == resizeHandle || o == guideX || o == guideY) continue;
                Bounds ob = o.getBoundsInParent();

                if (!snapX) {
                    if (Math.abs(propX - ob.getMinX()) < SNAP_THRESHOLD) {
                        finalX = ob.getMinX(); snapX = true; mostrarGuiaY(ob.getMinX(), lienzoCarta);
                    } else if (Math.abs(propX + w - ob.getMaxX()) < SNAP_THRESHOLD) {
                        finalX = ob.getMaxX() - w; snapX = true; mostrarGuiaY(ob.getMaxX(), lienzoCarta);
                    } else if (Math.abs(propX - ob.getMaxX()) < SNAP_THRESHOLD) {
                        finalX = ob.getMaxX(); snapX = true; mostrarGuiaY(ob.getMaxX(), lienzoCarta);
                    }
                }
                if (!snapY) {
                    if (Math.abs(propY - ob.getMinY()) < SNAP_THRESHOLD) {
                        finalY = ob.getMinY(); snapY = true; mostrarGuiaX(ob.getMinY(), lienzoCarta);
                    } else if (Math.abs(propY + h - ob.getMaxY()) < SNAP_THRESHOLD) {
                        finalY = ob.getMaxY() - h; snapY = true; mostrarGuiaX(ob.getMaxY(), lienzoCarta);
                    } else if (Math.abs(propY - ob.getMaxY()) < SNAP_THRESHOLD) {
                        finalY = ob.getMaxY(); snapY = true; mostrarGuiaX(ob.getMaxY(), lienzoCarta);
                    }
                }
            }
        }

        if (!snapX) guideY.setVisible(false);
        if (!snapY) guideX.setVisible(false);

        // Límites del lienzo (no salir)
        finalX = Math.max(0, Math.min(finalX, cW - w));
        finalY = Math.max(0, Math.min(finalY, cH - h));

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

    // ── Setters de callbacks ─────────────────────────────────────

    public void setOnCopiar(BiConsumer<Node, String> cb)    { onCopiarCallback   = cb; }
    public void setOnCortar(BiConsumer<Node, String> cb)    { onCortarCallback   = cb; }
    public void setOnDuplicar(BiConsumer<Node, String> cb)  { onDuplicarCallback = cb; }
    public void setOnPegar(Runnable cb)                     { onPegarCallback    = cb; }
    public void setOnSnapshot(Runnable cb)                  { onSnapshotCallback = cb; }

    public void setClipboard(String json) { clipboardJson = json; }
    public String getClipboard()          { return clipboardJson; }

    // ── Getters para PaginacionManager ──────────────────────────

    public Line      getGuideX()      { return guideX; }
    public Line      getGuideY()      { return guideY; }
    public Rectangle getResizeHandle(){ return resizeHandle; }
}