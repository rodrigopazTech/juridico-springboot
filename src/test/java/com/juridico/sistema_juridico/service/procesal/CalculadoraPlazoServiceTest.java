package com.juridico.sistema_juridico.service.procesal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.exception.BadRequestException;

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
    }

    @Test
    void tipoSinVencimientoNoFabricaUnaFecha() {
        assertNull(calculadora.resolverVencimiento(
                tipo(ModoPlazoTermino.SIN_VENCIMIENTO, null, "NATURALES"),
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1)));
    }

    private TipoTermino tipo(ModoPlazoTermino modo, Integer dias, String tipoDias) {
        return TipoTermino.builder().modoPlazo(modo).diasPredeterminados(dias).tipoDias(tipoDias).build();
    }
}
