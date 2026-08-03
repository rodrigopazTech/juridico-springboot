package com.juridico.sistema_juridico.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetRateLimitServiceTest {

    @Test
    void blocksRequestsWhenUserLimitIsReached() {
        PasswordResetRateLimitService service = new PasswordResetRateLimitService(2, 60, 10, 60);

        assertTrue(service.isAllowed("USUARIO@FINABIEN.GOB.MX", "10.0.0.1"));
        assertTrue(service.isAllowed("usuario@finabien.gob.mx", "10.0.0.1"));
        assertFalse(service.isAllowed(" usuario@finabien.gob.mx ", "10.0.0.2"));
    }

    @Test
    void blocksRequestsWhenIpLimitIsReached() {
        PasswordResetRateLimitService service = new PasswordResetRateLimitService(10, 60, 2, 60);

        assertTrue(service.isAllowed("uno@finabien.gob.mx", "10.0.0.1"));
        assertTrue(service.isAllowed("dos@finabien.gob.mx", "10.0.0.1"));
        assertFalse(service.isAllowed("tres@finabien.gob.mx", "10.0.0.1"));
    }
}
