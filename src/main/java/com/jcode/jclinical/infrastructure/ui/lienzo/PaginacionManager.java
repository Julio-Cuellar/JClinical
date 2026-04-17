package com.jcode.jclinical.infrastructure.ui.lienzo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la navegación y el estado de múltiples páginas del lienzo.
 * Responsabilidad única: saber qué página está activa, guardarla en memoria
 * y renderizarla cuando el usuario navega.
 */
public class PaginacionManager {

    private final List<ArrayNode> paginasData = new ArrayList<>();
    private int indicePaginaActual = 0;

    // Referencias a componentes del lienzo necesarios para renderizar
    private final Pane lienzoCarta;
    private final SerializacionService serializacionService;
    private final NodoInteraccionService nodoInteraccionService;
    private final ComponenteFactory componenteFactory;

    // Controles de UI opcionales (pueden ser null si el FXML no los incluye)
    private Button btnPaginaPrevia;
    private Button btnPaginaSiguiente;
    private Button btnAgregarPagina;
    private Label lblContadorPaginas;

    public PaginacionManager(Pane lienzoCarta,
                             SerializacionService serializacionService,
                             NodoInteraccionService nodoInteraccionService,
                             ComponenteFactory componenteFactory) {
        this.lienzoCarta = lienzoCarta;
        this.serializacionService = serializacionService;
        this.nodoInteraccionService = nodoInteraccionService;
        this.componenteFactory = componenteFactory;
    }

    // --- Configuración de controles de UI ---

    public void configurarControlesUI(Button btnPrevia, Button btnSiguiente,
                                      Button btnAgregar, Label lblContador) {
        this.btnPaginaPrevia = btnPrevia;
        this.btnPaginaSiguiente = btnSiguiente;
        this.btnAgregarPagina = btnAgregar;
        this.lblContadorPaginas = lblContador;

        if (btnAgregarPagina != null) btnAgregarPagina.setOnAction(e -> agregarNuevaPagina());
        if (btnPaginaPrevia != null) btnPaginaPrevia.setOnAction(e -> navegarPagina(-1));
        if (btnPaginaSiguiente != null) btnPaginaSiguiente.setOnAction(e -> navegarPagina(1));
    }

    // --- API pública ---

    public void inicializarConPaginaVacia() {
        paginasData.clear();
        indicePaginaActual = 0;
        paginasData.add(new ObjectMapper().createArrayNode());
        renderizarPaginaActual();
    }

    public void cargarPaginas(List<ArrayNode> paginas) {
        paginasData.clear();
        paginasData.addAll(paginas);
        if (paginasData.isEmpty()) {
            paginasData.add(new ObjectMapper().createArrayNode());
        }
        indicePaginaActual = 0;
        renderizarPaginaActual();
    }

    public void salvarPaginaActualAMemoria() {
        if (paginasData.isEmpty()) {
            paginasData.add(new ObjectMapper().createArrayNode());
        }
        paginasData.set(indicePaginaActual, serializacionService.serializarElementosActuales(lienzoCarta));
    }

    public List<ArrayNode> getPaginasData() {
        return paginasData;
    }

    public void reset() {
        paginasData.clear();
        indicePaginaActual = 0;
    }

    // --- Navegación ---

    private void agregarNuevaPagina() {
        salvarPaginaActualAMemoria();
        paginasData.add(new ObjectMapper().createArrayNode());
        indicePaginaActual = paginasData.size() - 1;
        renderizarPaginaActual();
    }

    private void navegarPagina(int delta) {
        int nuevoIndice = indicePaginaActual + delta;
        if (nuevoIndice >= 0 && nuevoIndice < paginasData.size()) {
            salvarPaginaActualAMemoria();
            indicePaginaActual = nuevoIndice;
            renderizarPaginaActual();
        }
    }

    // --- Renderizado ---

    private void renderizarPaginaActual() {
        nodoInteraccionService.deseleccionarNodo();

        // Limpia el lienzo pero conserva las guías y el handle de resize
        Line guideX = nodoInteraccionService.getGuideX();
        Line guideY = nodoInteraccionService.getGuideY();
        Rectangle resizeHandle = nodoInteraccionService.getResizeHandle();

        lienzoCarta.getChildren().clear();
        lienzoCarta.getChildren().addAll(guideX, guideY, resizeHandle);

        ArrayNode elementos = paginasData.get(indicePaginaActual);
        for (JsonNode el : elementos) {
            Node n = serializacionService.recrearNodo(el, componenteFactory);
            if (n == null) continue;

            n.setLayoutX(el.get("x").asDouble());
            n.setLayoutY(el.get("y").asDouble());

            SerializacionService.aplicarTamano(n, el);
            nodoInteraccionService.aplicarLogicaNodo(n, el.get("tipo").asText(), lienzoCarta);
            lienzoCarta.getChildren().add(n);
        }

        actualizarUIContador();
        resizeHandle.toFront();
    }

    private void actualizarUIContador() {
        if (lblContadorPaginas != null) {
            lblContadorPaginas.setText("Página " + (indicePaginaActual + 1) + " de " + paginasData.size());
        }
        if (btnPaginaPrevia != null) {
            btnPaginaPrevia.setDisable(indicePaginaActual == 0);
        }
        if (btnPaginaSiguiente != null) {
            btnPaginaSiguiente.setDisable(indicePaginaActual == paginasData.size() - 1);
        }
    }
}