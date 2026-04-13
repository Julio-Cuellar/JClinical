package com.jcode.jclinical.core.domain.model;

import com.jcode.jclinical.core.domain.exception.ExpedienteInvalidoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpedienteTest {

    @Test
    void debeActualizarContenidoDeFormaAtomica() {
        Expediente expediente = new Expediente("Juan Pérez", "{\"elementos\":[]}");

        expediente.actualizarContenido("Ana López", "{\"paginas\":[]}");

        assertEquals("Ana López", expediente.getNombrePaciente());
        assertEquals("{\"paginas\":[]}", expediente.getLienzoDinamicoJson());
    }

    @Test
    void noDebePermitirNombreVacio() {
        assertThrows(ExpedienteInvalidoException.class,
                () -> new Expediente(" ", "{\"elementos\":[]}"));
    }
}
