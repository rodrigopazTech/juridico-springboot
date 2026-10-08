package com.juridico.sistema_juridico.service.configuracion;

import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionCorreoSmtp;
import com.juridico.sistema_juridico.Entity.enums.EstadoConfiguracionCorreo;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.repository.configuracion.ConfiguracionCorreoSmtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;

class ConfiguracionCorreoSmtpServiceTest {

    private ConfiguracionCorreoSmtpRepository repository;
    private DynamicMailSenderProvider provider;
    private MailCredentialCipher cipher;
    private SmtpPasswordMailSenderFactory factory;
    private ConfiguracionCorreoSmtpService service;

    @BeforeEach
    void setUp() {
        repository = mock(ConfiguracionCorreoSmtpRepository.class);
        provider = mock(DynamicMailSenderProvider.class);
        cipher = new MailCredentialCipher(randomKey());
        factory = mock(SmtpPasswordMailSenderFactory.class);
        service = new ConfiguracionCorreoSmtpService(repository, cipher, factory, provider);
    }

    @Test
    void activatesOnlyAfterSuccessfulTestAndRetiresCurrentCredentialFirst() {
        ConfiguracionCorreoSmtp current = configuration(1L, EstadoConfiguracionCorreo.ACTIVA, true);
        ConfiguracionCorreoSmtp candidate = configuration(2L, EstadoConfiguracionCorreo.PROBADA, true);
        when(repository.findAllForUpdate()).thenReturn(List.of(current, candidate));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.activate(2L, "admin@siaj.local");

        assertEquals(EstadoConfiguracionCorreo.ANTERIOR, current.getEstado());
        assertEquals(EstadoConfiguracionCorreo.ACTIVA, candidate.getEstado());
        var order = inOrder(repository);
        order.verify(repository).saveAll(List.of(current));
        order.verify(repository).flush();
        order.verify(repository).save(candidate);
        order.verify(repository).flush();
        verify(provider).invalidate();
    }

    @Test
    void rejectsActivationWithoutSuccessfulTest() {
        ConfiguracionCorreoSmtp candidate = configuration(2L, EstadoConfiguracionCorreo.PENDIENTE, false);
        when(repository.findAllForUpdate()).thenReturn(List.of(candidate));

        assertThrows(BadRequestException.class, () -> service.activate(2L, "admin"));
    }

    @Test
    void failedMicrosoftAuthenticationIsPersistedWithFriendlyMessage() {
        ConfiguracionCorreoSmtp candidate = configuration(7L, EstadoConfiguracionCorreo.PENDIENTE, false);
        candidate.setPasswordCifrado(cipher.encrypt("smtp-secret"));
        when(repository.findById(7L)).thenReturn(Optional.of(candidate));
        when(factory.create("smtp-secret"))
                .thenThrow(new MailAuthenticationException("535 5.7.3 Authentication unsuccessful"));

        ConfiguracionCorreoSmtpService.TestResult result = service.testCandidate(7L, "admin");

        assertEquals(false, result.success());
        assertEquals(false, candidate.getPruebaExitosa());
        assertEquals(EstadoConfiguracionCorreo.PENDIENTE, candidate.getEstado());
        org.junit.jupiter.api.Assertions.assertTrue(result.message().contains("Microsoft 365"));
        org.junit.jupiter.api.Assertions.assertTrue(result.message().contains("SMTP AUTH"));
        verify(repository).save(candidate);
    }

    @Test
    void successfulTestAutomaticallyActivatesCandidateAndRetiresCurrentCredential() throws Exception {
        ConfiguracionCorreoSmtp current = configuration(1L, EstadoConfiguracionCorreo.ACTIVA, true);
        ConfiguracionCorreoSmtp candidate = configuration(2L, EstadoConfiguracionCorreo.PENDIENTE, false);
        candidate.setPasswordCifrado(cipher.encrypt("smtp-secret"));
        when(repository.findById(2L)).thenReturn(Optional.of(candidate));
        when(repository.findAllForUpdate()).thenReturn(List.of(current, candidate));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConfiguracionCorreoSmtpService.TestResult result = service.testCandidate(2L, "admin@siaj.local");

        assertTrue(result.success());
        assertTrue(result.message().contains("quedo activa"));
        assertEquals(EstadoConfiguracionCorreo.ANTERIOR, current.getEstado());
        assertEquals(EstadoConfiguracionCorreo.ACTIVA, candidate.getEstado());
        assertEquals("admin@siaj.local", candidate.getActivadaPor());
        verify(provider).invalidate();
    }

    private ConfiguracionCorreoSmtp configuration(Long id, EstadoConfiguracionCorreo status, boolean tested) {
        return ConfiguracionCorreoSmtp.builder()
                .id(id)
                .estado(status)
                .pruebaExitosa(tested)
                .passwordCifrado("v1:not-used")
                .correoPrueba("admin@siaj.local")
                .proximaFechaCambio(LocalDate.now().plusDays(30))
                .creadaPor("admin")
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .version(0L)
                .build();
    }

    private String randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
