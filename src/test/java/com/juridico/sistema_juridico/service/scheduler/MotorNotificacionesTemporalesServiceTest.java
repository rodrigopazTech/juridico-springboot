package com.juridico.sistema_juridico.service.scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;

class MotorNotificacionesTemporalesServiceTest {

    private final MotorNotificacionesTemporalesService motor =
            new MotorNotificacionesTemporalesService(null, null, null, null, null, null);

    @Test
    void presentadoYConcluidoDetienenAlertasDeTermino() {
        Termino termino = Termino.builder().fechaVencimiento(java.time.LocalDate.now()).build();
        termino.setEstatusTermino(EstatusTermino.LIBERADO);
        assertTrue(motor.generaRecordatorio(termino));
        termino.setEstatusTermino(EstatusTermino.PRESENTADO);
        assertFalse(motor.generaRecordatorio(termino));
        termino.setEstatusTermino(EstatusTermino.CONCLUIDO);
        assertFalse(motor.generaRecordatorio(termino));
    }

    @Test
    void audienciaSoloAlertaMientrasEstaPendiente() {
        Audiencia audiencia = Audiencia.builder().fechaAudiencia(java.time.LocalDate.now())
                .estatusAudiencia(EstatusAudiencia.PENDIENTE).build();
        assertTrue(motor.generaRecordatorio(audiencia));
        audiencia.setEstatusAudiencia(EstatusAudiencia.CON_ACTA);
        assertFalse(motor.generaRecordatorio(audiencia));
        audiencia.setEstatusAudiencia(EstatusAudiencia.SUSPENDIDA);
        assertFalse(motor.generaRecordatorio(audiencia));
        audiencia.setEstatusAudiencia(EstatusAudiencia.CONCLUIDA);
        assertFalse(motor.generaRecordatorio(audiencia));
    }
}
