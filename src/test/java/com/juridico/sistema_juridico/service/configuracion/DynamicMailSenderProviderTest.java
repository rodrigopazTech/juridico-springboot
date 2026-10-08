package com.juridico.sistema_juridico.service.configuracion;

import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionCorreoSmtp;
import com.juridico.sistema_juridico.Entity.enums.EstadoConfiguracionCorreo;
import com.juridico.sistema_juridico.repository.configuracion.ConfiguracionCorreoSmtpRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicMailSenderProviderTest {

    @Test
    void rebuildsSenderWhenActiveCredentialVersionChanges() {
        ConfiguracionCorreoSmtpRepository repository = mock(ConfiguracionCorreoSmtpRepository.class);
        MailCredentialCipher cipher = mock(MailCredentialCipher.class);
        SmtpPasswordMailSenderFactory factory = mock(SmtpPasswordMailSenderFactory.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> fallback = mock(ObjectProvider.class);
        JavaMailSender firstSender = mock(JavaMailSender.class);
        JavaMailSender secondSender = mock(JavaMailSender.class);
        ConfiguracionCorreoSmtp first = active(1L, 0L, "cipher-1");
        ConfiguracionCorreoSmtp second = active(2L, 0L, "cipher-2");

        when(cipher.isConfigured()).thenReturn(true);
        when(repository.findFirstByEstadoOrderByFechaActivacionDesc(EstadoConfiguracionCorreo.ACTIVA))
                .thenReturn(Optional.of(first), Optional.of(second));
        when(cipher.decrypt("cipher-1")).thenReturn("password-1");
        when(cipher.decrypt("cipher-2")).thenReturn("password-2");
        when(factory.create("password-1")).thenReturn(firstSender);
        when(factory.create("password-2")).thenReturn(secondSender);

        DynamicMailSenderProvider provider = new DynamicMailSenderProvider(
                repository, cipher, factory, fallback, 1000L);

        assertSame(firstSender, provider.getMailSender());
        provider.invalidate();
        assertSame(secondSender, provider.getMailSender());
        verify(factory).create("password-1");
        verify(factory).create("password-2");
    }

    private ConfiguracionCorreoSmtp active(Long id, Long version, String ciphertext) {
        return ConfiguracionCorreoSmtp.builder()
                .id(id)
                .version(version)
                .estado(EstadoConfiguracionCorreo.ACTIVA)
                .passwordCifrado(ciphertext)
                .build();
    }
}
