package com.juridico.sistema_juridico.service.configuracion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.repository.configuracion.PerfilAlertaTerminoRepository;
import com.juridico.sistema_juridico.repository.configuracion.PerfilRecordatorioAudienciaRepository;

class PerfilAtencionServiceTest {

    private final PerfilAtencionService service = new PerfilAtencionService(
            mock(PerfilAlertaTerminoRepository.class), mock(PerfilRecordatorioAudienciaRepository.class));

    @Test
    void perfilDiasExigeAmarilloMayorQueRojo() {
        PerfilAlertaTermino invalido = baseTermino().modo(ModoPerfilAlerta.DIAS_RESTANTES)
                .amarilloDias(1).rojoDias(2).build();
        assertThrows(BadRequestException.class, () -> service.validar(invalido));

        PerfilAlertaTermino valido = baseTermino().modo(ModoPerfilAlerta.DIAS_RESTANTES)
                .amarilloDias(2).rojoDias(1).build();
        assertDoesNotThrow(() -> service.validar(valido));
    }

    @Test
    void perfilProporcionalValidaPorcentajesYLimites() {
        PerfilAlertaTermino valido = baseTermino().modo(ModoPerfilAlerta.PROPORCIONAL_LIMITADO)
                .amarilloPorcentaje(50).amarilloMinimoDias(3).amarilloMaximoDias(10)
                .rojoPorcentaje(20).rojoMinimoDias(1).rojoMaximoDias(4).build();
        assertDoesNotThrow(() -> service.validar(valido));

        valido.setRojoPorcentaje(80);
        assertThrows(BadRequestException.class, () -> service.validar(valido));
    }

    @Test
    void audienciaExigeAnticipacionOrdenada() {
        PerfilRecordatorioAudiencia perfil = PerfilRecordatorioAudiencia.builder()
                .codigo("AUD").nombre("Audiencia").descripcion("Prueba")
                .anticipacionAmarillaHoras(24).anticipacionRojaHoras(48)
                .recordatoriosMinutos("1440,120").build();
        assertThrows(BadRequestException.class, () -> service.validar(perfil));
    }

    private PerfilAlertaTermino.PerfilAlertaTerminoBuilder baseTermino() {
        return PerfilAlertaTermino.builder().codigo("PERFIL").nombre("Perfil").descripcion("Perfil de prueba");
    }
}
