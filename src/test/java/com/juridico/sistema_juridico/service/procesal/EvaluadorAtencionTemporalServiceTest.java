package com.juridico.sistema_juridico.service.procesal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.Entity.enums.NivelAtencionTemporal;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;

class EvaluadorAtencionTemporalServiceTest {

    private final EvaluadorAtencionTemporalService evaluador =
            new EvaluadorAtencionTemporalService(new CalculadoraPlazoService());
    private final LocalDate hoy = LocalDate.of(2026, 9, 23);

    @Test
    void plazoCalculadoRespetaTodosLosLimites() {
        TipoTermino tipo = TipoTermino.builder().modoPlazo(ModoPlazoTermino.CALCULADO).tipoDias("NATURALES")
                .perfilAlerta(perfilDias()).build();

        assertEquals(NivelAtencionTemporal.VERDE, evaluador.evaluarTermino(termino(tipo, hoy.plusDays(3)), hoy).nivel());
        assertEquals(NivelAtencionTemporal.AMARILLO, evaluador.evaluarTermino(termino(tipo, hoy.plusDays(2)), hoy).nivel());
        assertEquals(NivelAtencionTemporal.ROJO, evaluador.evaluarTermino(termino(tipo, hoy.plusDays(1)), hoy).nivel());
        assertEquals(NivelAtencionTemporal.ROJO, evaluador.evaluarTermino(termino(tipo, hoy), hoy).nivel());
        assertEquals(NivelAtencionTemporal.VENCIDO, evaluador.evaluarTermino(termino(tipo, hoy.minusDays(1)), hoy).nivel());
    }

    @Test
    void plazoCapturadoAplicaPorcentajeConMinimoYMaximo() {
        PerfilAlertaTermino perfil = PerfilAlertaTermino.builder()
                .nombre("Variable estándar").modo(ModoPerfilAlerta.PROPORCIONAL_LIMITADO)
                .amarilloPorcentaje(50).amarilloMinimoDias(3).amarilloMaximoDias(10)
                .rojoPorcentaje(20).rojoMinimoDias(1).rojoMaximoDias(4).build();
        TipoTermino tipo = TipoTermino.builder().modoPlazo(ModoPlazoTermino.CAPTURADO).perfilAlerta(perfil).build();
        Termino termino = termino(tipo, hoy.plusDays(20));
        termino.setFechaIngreso(hoy);

        assertEquals(10, evaluador.resolverUmbrales(termino, perfil)[0]);
        assertEquals(4, evaluador.resolverUmbrales(termino, perfil)[1]);

        termino.setFechaVencimiento(hoy.plusDays(90));
        assertEquals(10, evaluador.resolverUmbrales(termino, perfil)[0]);
        assertEquals(4, evaluador.resolverUmbrales(termino, perfil)[1]);
    }

    @Test
    void sinVencimientoNuncaGeneraColorActivo() {
        TipoTermino tipo = TipoTermino.builder().modoPlazo(ModoPlazoTermino.SIN_VENCIMIENTO).build();
        assertEquals(NivelAtencionTemporal.SIN_CALCULO,
                evaluador.evaluarTermino(termino(tipo, null), hoy).nivel());
    }

    @Test
    void presentadoDetieneLaAtencionTemporal() {
        Termino termino = termino(TipoTermino.builder().modoPlazo(ModoPlazoTermino.CALCULADO)
                .perfilAlerta(perfilDias()).build(), hoy.minusDays(5));
        termino.setEstatusTermino(EstatusTermino.PRESENTADO);
        assertEquals(NivelAtencionTemporal.CERRADO, evaluador.evaluarTermino(termino, hoy).nivel());
    }

    @Test
    void audienciaUsaProximidadYPasadaSinResultado() {
        PerfilRecordatorioAudiencia perfil = PerfilRecordatorioAudiencia.builder()
                .nombre("Audiencia estándar").anticipacionAmarillaHoras(72).anticipacionRojaHoras(24).build();
        TipoAudiencia tipo = TipoAudiencia.builder().perfilRecordatorio(perfil).build();
        LocalDateTime ahora = LocalDateTime.of(hoy, LocalTime.of(10, 0));

        assertEquals(NivelAtencionTemporal.VERDE, evaluador.evaluarAudiencia(audiencia(tipo, hoy.plusDays(4), LocalTime.NOON), ahora).nivel());
        assertEquals(NivelAtencionTemporal.AMARILLO, evaluador.evaluarAudiencia(audiencia(tipo, hoy.plusDays(2), LocalTime.NOON), ahora).nivel());
        assertEquals(NivelAtencionTemporal.ROJO, evaluador.evaluarAudiencia(audiencia(tipo, hoy, LocalTime.NOON), ahora).nivel());
        assertEquals(NivelAtencionTemporal.PASADA_SIN_RESULTADO,
                evaluador.evaluarAudiencia(audiencia(tipo, hoy.minusDays(1), LocalTime.NOON), ahora).nivel());
    }

    @Test
    void audienciaSuspendidaNoSeConfundeConResultadoRegistrado() {
        Audiencia audiencia = audiencia(TipoAudiencia.builder().build(), hoy.plusDays(4), LocalTime.NOON);
        audiencia.setEstatusAudiencia(EstatusAudiencia.SUSPENDIDA);

        assertEquals("Suspendida", evaluador.evaluarAudiencia(audiencia).etiqueta());
    }

    private PerfilAlertaTermino perfilDias() {
        return PerfilAlertaTermino.builder().nombre("Plazo corto").modo(ModoPerfilAlerta.DIAS_RESTANTES)
                .amarilloDias(2).rojoDias(1).build();
    }

    private Termino termino(TipoTermino tipo, LocalDate vencimiento) {
        return Termino.builder().tipoTerminoRef(tipo).fechaVencimiento(vencimiento)
                .fechaIngreso(hoy).estatusTermino(EstatusTermino.LIBERADO).tipoDiasCalculado("NATURALES").build();
    }

    private Audiencia audiencia(TipoAudiencia tipo, LocalDate fecha, LocalTime hora) {
        return Audiencia.builder().tipoAudiencia(tipo).fechaAudiencia(fecha).horaAudiencia(hora)
                .estatusAudiencia(EstatusAudiencia.PENDIENTE).build();
    }
}
