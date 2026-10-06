package com.juridico.sistema_juridico.service.procesal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.service.configuracion.CalendarioInstitucionalService;
import com.juridico.sistema_juridico.service.configuracion.CalendarioSnapshot;

class CalculadoraPlazoServiceTest {

    private final CalculadoraPlazoService calculadora = new CalculadoraPlazoService();

    @Test
    void calculaDiasNaturalesDesdeLaFechaOrigen() {
        TipoTermino tipo = tipo(ModoPlazoTermino.CALCULADO, 5, "NATURALES");
        assertEquals(LocalDate.of(2027, 1, 6), calculadora.resolverVencimiento(
                tipo, LocalDate.of(2027, 1, 1), null));
    }

    @Test
    void calculaDiasHabilesSinContarFinDeSemana() {
        TipoTermino tipo = tipo(ModoPlazoTermino.CALCULADO, 2, "HABILES");
        assertEquals(LocalDate.of(2026, 9, 28), calculadora.resolverVencimiento(
                tipo, LocalDate.of(2026, 9, 24), null));
        assertEquals(2, calculadora.diasEntre(
                LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 28), "HABILES"));
    }

    @Test
    void plazoCapturadoValidaLaSecuenciaDeFechas() {
        TipoTermino tipo = tipo(ModoPlazoTermino.CAPTURADO, null, "NATURALES");
        assertThrows(BadRequestException.class, () -> calculadora.resolverVencimiento(
                tipo, LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 9)));
        assertEquals(LocalDate.of(2027, 1, 10), calculadora.resolverVencimiento(
                tipo, LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 10)));
    }

    @Test
    void tipoSinVencimientoNoFabricaUnaFecha() {
        assertNull(calculadora.resolverVencimiento(
                tipo(ModoPlazoTermino.SIN_VENCIMIENTO, null, "NATURALES"),
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1)));
    }

    @Test
    void calculaConSnapshotVersionadoSinContarSuspension() {
        CalendarioInstitucionalService calendario = mock(CalendarioInstitucionalService.class);
        Set<LocalDate> suspension = Set.of(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 8));
        when(calendario.snapshotActual()).thenReturn(new CalendarioSnapshot(4, suspension));
        when(calendario.serializar(suspension)).thenReturn("2026-10-07,2026-10-08");
        CalculadoraPlazoService servicio = new CalculadoraPlazoService(calendario);

        ResultadoCalculoPlazo resultado = servicio.calcularResultado(
                tipo(ModoPlazoTermino.CALCULADO, 3, "HABILES"),
                LocalDate.of(2026, 10, 5), null);

        assertEquals(LocalDate.of(2026, 10, 12), resultado.fechaAdoptada());
        assertEquals(4, resultado.calendarioVersion());
        assertEquals("2026-10-07,2026-10-08", resultado.calendarioFechas());
        assertTrue(resultado.explicacion().contains("calendario v4"));
    }

    @Test
    void ajustaVencimientoNaturalAlSiguienteHabilConservandoAmbasFechas() {
        TipoTermino tipo = tipo(ModoPlazoTermino.CALCULADO, 2, "NATURALES");
        tipo.setAjustaAlSiguienteHabil(true);
        ResultadoCalculoPlazo resultado = calculadora.calcularResultado(
                tipo, LocalDate.of(2026, 10, 9), null);

        assertEquals(LocalDate.of(2026, 10, 11), resultado.fechaCalculada());
        assertEquals(LocalDate.of(2026, 10, 12), resultado.fechaAdoptada());
        assertTrue(resultado.explicacion().contains("siguiente día hábil"));
    }

    @Test
    void snapshotAplicadoPermaneceEnElTermino() {
        TipoTermino tipo = tipo(ModoPlazoTermino.CAPTURADO, null, "NATURALES");
        ResultadoCalculoPlazo resultado = calculadora.calcularResultado(
                tipo, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20));
        com.juridico.sistema_juridico.Entity.procesal.Termino termino =
                new com.juridico.sistema_juridico.Entity.procesal.Termino();

        calculadora.aplicarSnapshot(termino, resultado);

        assertEquals(ModoPlazoTermino.CAPTURADO, termino.getPlazoModoSnapshot());
        assertEquals(19L, termino.getPlazoDuracionDetectada());
        assertEquals(LocalDate.of(2026, 10, 20), termino.getPlazoFechaAdoptada());
        assertTrue(termino.getPlazoExplicacion().contains("fecha fue informada"));
    }

    @Test
    void reproduceElResultadoSinConsultarLaConfiguracionActual() {
        com.juridico.sistema_juridico.Entity.procesal.Termino termino =
                new com.juridico.sistema_juridico.Entity.procesal.Termino();
        termino.setFechaIngreso(LocalDate.of(2026, 10, 5));
        termino.setPlazoModoSnapshot(ModoPlazoTermino.CALCULADO);
        termino.setPlazoCantidadSnapshot(3);
        termino.setPlazoTipoDiasSnapshot("HABILES");
        termino.setPlazoCalendarioVersion(9);
        termino.setPlazoCalendarioFechas("2026-10-07,2026-10-08");
        termino.setPlazoReglaComputo("ORIGEN_NO_CUENTA;HABILES");
        termino.setPlazoFechaCalculada(LocalDate.of(2026, 10, 12));

        ResultadoCalculoPlazo reproducido = calculadora.reproducirDesdeSnapshot(termino);

        assertEquals(LocalDate.of(2026, 10, 12), reproducido.fechaCalculada());
        assertEquals(9, reproducido.calendarioVersion());
    }

    private TipoTermino tipo(ModoPlazoTermino modo, Integer dias, String tipoDias) {
        return TipoTermino.builder().modoPlazo(modo).diasPredeterminados(dias).tipoDias(tipoDias).build();
    }
}
