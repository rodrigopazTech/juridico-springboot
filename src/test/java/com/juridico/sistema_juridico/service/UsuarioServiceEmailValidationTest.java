package com.juridico.sistema_juridico.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsuarioServiceEmailValidationTest {

    private final UsuarioService usuarioService = new UsuarioService();

    @Test
    void acceptsUserWithoutInstitutionalEmail() {
        String email = ReflectionTestUtils.invokeMethod(usuarioService, "validateOptionalEmail", "  ");

        assertNull(email);
    }

    @Test
    void rejectsMalformedEmailBeforeSaving() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(usuarioService,
                        "validateOptionalEmail", "correo-invalido"));

        assertEquals("Captura un correo electrónico válido.", error.getMessage());
    }
}
