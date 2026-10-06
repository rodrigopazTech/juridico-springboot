package com.juridico.sistema_juridico.service.procesal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.exception.BadRequestException;

class FlujoAudienciaServiceTest {
    private final FlujoAudienciaService flujo = new FlujoAudienciaService();

    @Test
    void resultadoSoloSeRegistraDesdePendiente() {
        Audiencia audiencia = Audiencia.builder().estatusAudiencia(EstatusAudiencia.PENDIENTE).build();
        assertDoesNotThrow(() -> flujo.validarRegistroResultado(audiencia, "Se celebró la audiencia"));
        audiencia.setEstatusAudiencia(EstatusAudiencia.CON_ACTA);
        assertThrows(BadRequestException.class,
                () -> flujo.validarRegistroResultado(audiencia, "Otro resultado"));
    }

    @Test
    void audienciaNormalRequiereResultadoYActaParaConcluir() {
        Audiencia audiencia = Audiencia.builder().estatusAudiencia(EstatusAudiencia.CON_ACTA)
                .resultado("Se celebró").build();
        assertThrows(BadRequestException.class, () -> flujo.validarConclusion(audiencia));
        audiencia.setActaDocumento("acta.pdf");
        assertDoesNotThrow(() -> flujo.validarConclusion(audiencia));
    }

    @Test
    void audienciaSuspendidaRequiereNotaParaConcluir() {
        Audiencia audiencia = Audiencia.builder().estatusAudiencia(EstatusAudiencia.SUSPENDIDA).build();
        assertThrows(BadRequestException.class, () -> flujo.validarConclusion(audiencia));
        audiencia.setNotaInformativaDocumento("nota.pdf");
        assertDoesNotThrow(() -> flujo.validarConclusion(audiencia));
    }
}
