package com.jcode.jclinical.core.domain.model;

import com.jcode.jclinical.core.domain.exception.PlantillaInvalidaException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlantillaTest {

    @Test
    void debeActualizarContenido() {
        Plantilla plantilla = new Plantilla("Ingreso", "{\"elementos\":[]}");

        plantilla.actualizarContenido("Evolución", "{\"paginas\":[]}");

        assertEquals("Evolución", plantilla.getNombre());
        assertEquals("{\"paginas\":[]}", plantilla.getLayoutJson());
    }

    @Test
    void noDebePermitirLayoutVacio() {
        assertThrows(PlantillaInvalidaException.class,
                () -> new Plantilla("Ingreso", " "));
    }
}
