package com.juridico.sistema_juridico.service.configuracion;

import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionCorreoSmtp;
import com.juridico.sistema_juridico.Entity.enums.EstadoConfiguracionCorreo;
import com.juridico.sistema_juridico.repository.configuracion.ConfiguracionCorreoSmtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class DynamicMailSenderProvider {

    private final ConfiguracionCorreoSmtpRepository repository;
    private final MailCredentialCipher cipher;
    private final SmtpPasswordMailSenderFactory senderFactory;
    private final ObjectProvider<JavaMailSender> fallbackProvider;
    private final long refreshMillis;

    private volatile CachedSender cachedSender;
    private volatile long lastCheckMillis;

    public DynamicMailSenderProvider(
            ConfiguracionCorreoSmtpRepository repository,
            MailCredentialCipher cipher,
            SmtpPasswordMailSenderFactory senderFactory,
            ObjectProvider<JavaMailSender> fallbackProvider,
            @Value("${app.email.dynamic.refresh-ms:${MAIL_DYNAMIC_REFRESH_MS:10000}}") long refreshMillis) {
        this.repository = repository;
        this.cipher = cipher;
        this.senderFactory = senderFactory;
        this.fallbackProvider = fallbackProvider;
        this.refreshMillis = Math.max(refreshMillis, 1000L);
    }

    public JavaMailSender getMailSender() {
        long now = System.currentTimeMillis();
        CachedSender current = cachedSender;
        if (now - lastCheckMillis < refreshMillis) {
            return current != null ? current.sender() : fallbackProvider.getIfAvailable();
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - lastCheckMillis < refreshMillis) {
                current = cachedSender;
                return current != null ? current.sender() : fallbackProvider.getIfAvailable();
            }
            lastCheckMillis = now;
            try {
                Optional<ConfiguracionCorreoSmtp> active = repository
                        .findFirstByEstadoOrderByFechaActivacionDesc(EstadoConfiguracionCorreo.ACTIVA);
                if (active.isEmpty() || !cipher.isConfigured()) {
                    cachedSender = null;
                    return fallbackProvider.getIfAvailable();
                }
                ConfiguracionCorreoSmtp configuration = active.get();
                current = cachedSender;
                if (current == null || !current.matches(configuration)) {
                    String password = cipher.decrypt(configuration.getPasswordCifrado());
                    JavaMailSender refreshed = senderFactory.create(password);
                    cachedSender = new CachedSender(configuration.getId(), configuration.getVersion(), refreshed);
                    log.info("Configuracion SMTP dinamica actualizada a la version {}.", configuration.getId());
                }
                return cachedSender.sender();
            } catch (Exception ex) {
                log.error("No fue posible actualizar la configuracion SMTP dinamica; se conserva la ultima valida.", ex);
                current = cachedSender;
                return current != null ? current.sender() : fallbackProvider.getIfAvailable();
            }
        }
    }

    public void invalidate() {
        lastCheckMillis = 0L;
    }

    private record CachedSender(Long id, Long version, JavaMailSender sender) {
        private boolean matches(ConfiguracionCorreoSmtp configuration) {
            return id.equals(configuration.getId()) && version.equals(configuration.getVersion());
        }
    }
}
