package com.juridico.sistema_juridico.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodigoNormalizerTest {

    @Test
    @DisplayName("Debe normalizar un nombre de 100 caracteres y limitar su longitud a máximo 50")
    void testNombreDe100Caracteres() {
        // Nombre con más de 100 caracteres
        String nombre100 = "AUDIENCIA_DE_CONCILIACION_Y_DEPURACION_PROCESAL_EN_MATERIA_CIVIL_Y_MERCANTIL_EXTRAORDINARIA_NUMERO_100_";
        assertTrue(nombre100.length() >= 100);

        String codigoGenerado = CodigoNormalizer.normalizar(nombre100);

        assertNotNull(codigoGenerado);
        assertTrue(codigoGenerado.length() <= 50, "El código generado debe tener 50 caracteres o menos, longitud: " + codigoGenerado.length());
        assertTrue(codigoGenerado.matches("^[A-Z0-9_]+$"), "El código debe contener únicamente caracteres válidos A-Z, 0-9 y _");
    }

    @Test
    @DisplayName("Dos nombres largos con el mismo prefijo deben generar códigos diferentes y únicos")
    void testDosNombresLargosConMismoPrefijo() {
        String baseComun = "AUDIENCIA_DE_CONCILIACION_Y_DEPURACION_PROCESAL_EN_MATERIA_CIVIL_";
        String nombreA = baseComun + "CASO_ALPHA_1234567890_DIFERENCIADOR_A";
        String nombreB = baseComun + "CASO_BETA__1234567890_DIFERENCIADOR_B";

        String codigoA = CodigoNormalizer.normalizar(nombreA);
        String codigoB = CodigoNormalizer.normalizar(nombreB);

        assertTrue(codigoA.length() <= 50, "codigoA debe tener <= 50 caracteres");
        assertTrue(codigoB.length() <= 50, "codigoB debe tener <= 50 caracteres");
        assertNotEquals(codigoA, codigoB, "Dos nombres diferentes con el mismo prefijo deben generar códigos distintos");
    }

    @Test
    @DisplayName("Debe generar formato conceptual esperado con sufijo ID")
    void testConSufijoId() {
        String nombre = "Aceptación y Protesta de Cargo";
        Integer id = 387;

        String codigo = CodigoNormalizer.normalizar(nombre, id);

        assertEquals("ACEPTACION_Y_PROTESTA_DE_CARGO_387", codigo);
        assertTrue(codigo.length() <= 50);
    }

    @Test
    @DisplayName("Nombre extremadamente largo con sufijo ID debe truncar la base y conservar el ID intacto")
    void testNombreLargoConSufijoId() {
        String nombreMuyLargo = "NOTIFICACION_PERSONAL_DE_SENTENCIA_DEFINITIVA_EN_JUICIO_ORDINARIO_MERCANTIL_CON_EMBARGO_PRECAUTORIO";
        Integer id = 99999;

        String codigo = CodigoNormalizer.normalizar(nombreMuyLargo, id);

        assertTrue(codigo.length() <= 50, "Longitud debe ser <= 50, actual: " + codigo.length());
        assertTrue(codigo.endsWith("_99999"), "Debe terminar con el ID especificado");
    }

    @Test
    @DisplayName("Debe normalizar caracteres con tildes, ñ y caracteres especiales")
    void testCaracteresEspeciales() {
        String nombre = "Término (10 días hábiles) - Año 2026 / Audiencia #1";
        String codigo = CodigoNormalizer.normalizar(nombre);

        assertEquals("TERMINO_10_DIAS_HABILES_ANO_2026_AUDIENCIA_1", codigo);
    }

    @Test
    @DisplayName("Texto nulo o vacío debe producir un código por defecto válido")
    void testNuloOVacio() {
        String codigoNull = CodigoNormalizer.normalizar(null);
        String codigoBlank = CodigoNormalizer.normalizar("   ");

        assertEquals("CODIGO", codigoNull);
        assertEquals("CODIGO", codigoBlank);
    }
}
