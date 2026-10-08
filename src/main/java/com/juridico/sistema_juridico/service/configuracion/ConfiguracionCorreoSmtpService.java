package com.juridico.sistema_juridico.service.configuracion;

import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionCorreoSmtp;
import com.juridico.sistema_juridico.Entity.enums.EstadoConfiguracionCorreo;
import com.juridico.sistema_juridico.dto.response.configuracion.ConfiguracionCorreoSmtpView;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.configuracion.ConfiguracionCorreoSmtpRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConfiguracionCorreoSmtpService {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionCorreoSmtpService.class);

    private final ConfiguracionCorreoSmtpRepository repository;
    private final MailCredentialCipher cipher;
    private final SmtpPasswordMailSenderFactory senderFactory;
    private final DynamicMailSenderProvider dynamicMailSenderProvider;

    public ConfiguracionCorreoSmtpService(
            ConfiguracionCorreoSmtpRepository repository,
            MailCredentialCipher cipher,
            SmtpPasswordMailSenderFactory senderFactory,
            DynamicMailSenderProvider dynamicMailSenderProvider) {
        this.repository = repository;
        this.cipher = cipher;
        this.senderFactory = senderFactory;
        this.dynamicMailSenderProvider = dynamicMailSenderProvider;
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionCorreoSmtpView> history() {
        return repository.findAllByOrderByFechaCreacionDesc().stream()
                .map(ConfiguracionCorreoSmtpView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfiguracionCorreoSmtpView active() {
        return repository.findFirstByEstadoOrderByFechaActivacionDesc(EstadoConfiguracionCorreo.ACTIVA)
                .map(ConfiguracionCorreoSmtpView::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ConfiguracionCorreoSmtpView rollbackCandidate() {
        return repository.findFirstByEstadoOrderByFechaActivacionDesc(EstadoConfiguracionCorreo.ANTERIOR)
                .map(ConfiguracionCorreoSmtpView::from)
                .orElse(null);
    }

    public boolean encryptionReady() {
        return cipher.isConfigured();
    }

    public String connectionSummary() {
        return senderFactory.connectionSummary();
    }

    @Transactional
    public ConfiguracionCorreoSmtpView createCandidate(
            String password, String testEmail, LocalDate nextChangeDate, String actor) {
        validateCandidate(password, testEmail, nextChangeDate);
        ConfiguracionCorreoSmtp created = ConfiguracionCorreoSmtp.builder()
                .passwordCifrado(cipher.encrypt(password))
                .correoPrueba(testEmail.trim().toLowerCase())
                .proximaFechaCambio(nextChangeDate)
                .estado(EstadoConfiguracionCorreo.PENDIENTE)
                .pruebaExitosa(null)
                .creadaPor(safeActor(actor))
                .build();
        return ConfiguracionCorreoSmtpView.from(repository.save(created));
    }

    @Transactional
    public TestResult testCandidate(Long id, String actor) {
        ConfiguracionCorreoSmtp configuration = find(id);
        if (configuration.getEstado() == EstadoConfiguracionCorreo.ACTIVA
                || configuration.getEstado() == EstadoConfiguracionCorreo.ANTERIOR) {
            throw new BadRequestException("Solo se pueden probar credenciales pendientes o ya probadas.");
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            String password = cipher.decrypt(configuration.getPasswordCifrado());
            JavaMailSender sender = senderFactory.create(password);
            senderFactory.sendTest(sender, configuration.getCorreoPrueba());
            configuration.setEstado(EstadoConfiguracionCorreo.PROBADA);
            configuration.setPruebaExitosa(true);
            configuration.setFechaPrueba(now);
            configuration.setProbadaPor(safeActor(actor));
            configuration.setUltimoResultadoPrueba("Prueba enviada correctamente.");
            repository.save(configuration);
            repository.flush();
            activateTestedCandidate(id, actor);
            return new TestResult(true,
                    "Correo de prueba enviado correctamente. La credencial quedo activa sin reiniciar la aplicacion.");
        } catch (Exception ex) {
            configuration.setEstado(EstadoConfiguracionCorreo.PENDIENTE);
            configuration.setPruebaExitosa(false);
            configuration.setFechaPrueba(now);
            configuration.setProbadaPor(safeActor(actor));
            String message = "Fallo la prueba: " + safeFailure(ex);
            configuration.setUltimoResultadoPrueba(message);
            repository.save(configuration);
            log.warn("Prueba SMTP rechazada para la configuracion {}: {}", configuration.getId(), safeFailure(ex));
            return new TestResult(false, message);
        }
    }

    @Transactional
    public void activate(Long id, String actor) {
        activateTestedCandidate(id, actor);
    }

    private void activateTestedCandidate(Long id, String actor) {
        List<ConfiguracionCorreoSmtp> configurations = repository.findAllForUpdate();
        ConfiguracionCorreoSmtp candidate = configurations.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Configuracion SMTP no encontrada."));
        if (candidate.getEstado() != EstadoConfiguracionCorreo.PROBADA
                || !Boolean.TRUE.equals(candidate.getPruebaExitosa())) {
            throw new BadRequestException("La credencial debe superar una prueba antes de activarse.");
        }
        LocalDateTime now = LocalDateTime.now();
        List<ConfiguracionCorreoSmtp> activeConfigurations = configurations.stream()
                .filter(item -> item.getEstado() == EstadoConfiguracionCorreo.ACTIVA)
                .toList();
        activeConfigurations.forEach(item -> {
                    item.setEstado(EstadoConfiguracionCorreo.ANTERIOR);
                    item.setFechaRetiro(now);
                });
        repository.saveAll(activeConfigurations);
        repository.flush();
        candidate.setEstado(EstadoConfiguracionCorreo.ACTIVA);
        candidate.setFechaActivacion(now);
        candidate.setActivadaPor(safeActor(actor));
        candidate.setFechaRetiro(null);
        repository.save(candidate);
        repository.flush();
        dynamicMailSenderProvider.invalidate();
    }

    @Transactional
    public void rollback(String actor) {
        List<ConfiguracionCorreoSmtp> configurations = repository.findAllForUpdate();
        ConfiguracionCorreoSmtp previous = configurations.stream()
                .filter(item -> item.getEstado() == EstadoConfiguracionCorreo.ANTERIOR)
                .filter(item -> Boolean.TRUE.equals(item.getPruebaExitosa()))
                .max((left, right) -> compareNullable(left.getFechaActivacion(), right.getFechaActivacion()))
                .orElseThrow(() -> new BadRequestException("No existe una credencial anterior valida para restaurar."));
        try {
            JavaMailSender previousSender = senderFactory.create(cipher.decrypt(previous.getPasswordCifrado()));
            senderFactory.sendTest(previousSender, previous.getCorreoPrueba());
        } catch (Exception ex) {
            throw new BadRequestException(
                    "La credencial anterior no supero la prueba y no fue restaurada: " + safeFailure(ex));
        }
        LocalDateTime now = LocalDateTime.now();
        List<ConfiguracionCorreoSmtp> activeConfigurations = configurations.stream()
                .filter(item -> item.getEstado() == EstadoConfiguracionCorreo.ACTIVA)
                .toList();
        activeConfigurations.forEach(item -> {
                    item.setEstado(EstadoConfiguracionCorreo.ANTERIOR);
                    item.setFechaRetiro(now);
                });
        repository.saveAll(activeConfigurations);
        repository.flush();
        previous.setEstado(EstadoConfiguracionCorreo.ACTIVA);
        previous.setFechaActivacion(now);
        previous.setActivadaPor(safeActor(actor) + " (rollback)");
        previous.setFechaRetiro(null);
        repository.save(previous);
        repository.flush();
        dynamicMailSenderProvider.invalidate();
    }

    private ConfiguracionCorreoSmtp find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuracion SMTP no encontrada."));
    }

    private void validateCandidate(String password, String testEmail, LocalDate nextChangeDate) {
        if (!cipher.isConfigured()) {
            throw new BadRequestException(
                    "La administracion de credenciales no esta habilitada: falta MAIL_CREDENTIAL_ENCRYPTION_KEY.");
        }
        if (password == null || password.isBlank() || password.length() > 1024) {
            throw new BadRequestException("La contrasena es obligatoria y no puede exceder 1024 caracteres.");
        }
        if (testEmail == null || testEmail.isBlank() || testEmail.length() > 254 || !testEmail.contains("@")) {
            throw new BadRequestException("Indique un correo de prueba valido.");
        }
        if (nextChangeDate == null || nextChangeDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("La proxima fecha de cambio no puede estar en el pasado.");
        }
    }

    private String safeActor(String actor) {
        String value = actor == null || actor.isBlank() ? "administrador" : actor.trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private String safeFailure(Exception error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = root.getClass().getSimpleName();
        }
        if (message.contains("535") || message.toLowerCase().contains("authentication unsuccessful")) {
            return "Microsoft 365 rechazo la autenticacion SMTP (535). Verifique el usuario, la contrasena y que SMTP AUTH este habilitado para el buzon.";
        }
        message = message.replaceAll("(?i)(password|contrasena|credential)=[^\\s,;]+", "$1=***")
                .replaceAll("[\\r\\n\\t]+", " ").trim();
        return message.length() > 470 ? message.substring(0, 470) : message;
    }

    private int compareNullable(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    public record TestResult(boolean success, String message) {
    }
}
