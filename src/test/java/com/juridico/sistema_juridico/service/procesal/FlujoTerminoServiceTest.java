package com.juridico.sistema_juridico.service.procesal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.TipoEvidenciaPresentacion;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.exception.BadRequestException;

class FlujoTerminoServiceTest {
    private final FlujoTerminoService flujo = new FlujoTerminoService();

    @Test
    void sóloAvanzaEnOrdenHastaLiberado() {
        assertEquals(EstatusTermino.REVISION, flujo.validarYResolverAvance(termino(EstatusTermino.PROYECTISTA), true));
        assertEquals(EstatusTermino.LIBERADO, flujo.validarYResolverAvance(termino(EstatusTermino.DIRECCION), true));
        assertThrows(BadRequestException.class,
                () -> flujo.validarYResolverAvance(termino(EstatusTermino.LIBERADO), true));
    }

    @Test
    void bloqueaAvanceSinDocumento() {
        assertThrows(BadRequestException.class,
                () -> flujo.validarYResolverAvance(termino(EstatusTermino.REVISION), false));
    }

    @Test
    void conclusiónExigePresentaciónConEvidencia() {
        Termino presentado = termino(EstatusTermino.PRESENTADO);
        assertThrows(BadRequestException.class, () -> flujo.validarConclusion(presentado));
        presentado.setTipoEvidenciaPresentacion(TipoEvidenciaPresentacion.ACUSE);
        presentado.setAcuseNombre("acuse.pdf");
        flujo.validarConclusion(presentado);
    }

    private Termino termino(EstatusTermino estado) {
        return Termino.builder().estatusTermino(estado).build();
    }
}
