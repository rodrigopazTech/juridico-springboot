package com.juridico.sistema_juridico.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.juridico.sistema_juridico.service.configuracion.DynamicMailSenderProvider;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.email.maintenance", havingValue = "false", matchIfMissing = true)
public class PasswordResetEmailService implements PasswordResetMailer {

    private final DynamicMailSenderProvider dynamicMailSenderProvider;
    private final ThreadLocal<String> lastFailureSummary = new ThreadLocal<>();

    @Value("${app.email.from:siaj@finabien.gob.mx}")
    private String from;

    @Value("${app.email.app-name:SIAJ}")
    private String appName;

    public boolean sendResetEmail(String to, String resetUrl, int expirationMinutes) {
        String subject = appName + " - Recuperacion de contrasena";
        String text = "Recupera tu contrasena de " + appName + ".\n\n"
                + resetUrl + "\n\n"
                + "Vence en " + expirationMinutes + " minutos.";
        String html = renderEmail(
                "Recuperacion de contrasena",
                "<p style=\"margin:0 0 14px;color:#3f3f46;font-size:15px;line-height:1.5;\">Usa este enlace para crear una nueva contrasena.</p>",
                renderInfoBox("Vence en", expirationMinutes + " minutos"),
                "Crear nueva contrasena",
                resetUrl,
                "Si no lo solicitaste, ignora este correo.");

        return sendMessage(to, subject, text, html, "recuperacion");
    }

    public boolean sendActivationEmail(String to, String username, String activationUrl, int expirationMinutes) {
        String subject = appName + " - Activacion de cuenta";
        String text = "Tu cuenta de " + appName + " esta lista.\n\n"
                + "Usuario: " + username + "\n"
                + "Vence en: " + expirationMinutes + " minutos\n\n"
                + activationUrl;
        String html = renderEmail(
                "Activa tu cuenta",
                "<p style=\"margin:0 0 14px;color:#3f3f46;font-size:15px;line-height:1.5;\">Tu cuenta esta lista. Establece tu contrasena para ingresar.</p>",
                renderDetailBox(new String[][] {
                        {"Usuario", username},
                        {"Vence en", expirationMinutes + " minutos"}
                }),
                "Establecer contrasena",
                activationUrl,
                null);

        return sendMessage(to, subject, text, html, "activacion");
    }

    public boolean sendUsernameReminderEmail(String to, String username, String employeeNumber) {
        String employeeLine = employeeNumber != null && !employeeNumber.isBlank()
                ? "Numero de empleado: " + employeeNumber + "\n"
                : "";
        String subject = appName + " - Recordatorio de usuario";
        String text = "Este es tu usuario de acceso a " + appName + ".\n\n"
                + "Usuario: " + username + "\n"
                + employeeLine;
        String html = renderEmail(
                "Tu usuario de acceso",
                "<p style=\"margin:0 0 14px;color:#3f3f46;font-size:15px;line-height:1.5;\">Este es el usuario registrado para tu cuenta.</p>",
                renderDetailBox(employeeNumber != null && !employeeNumber.isBlank()
                        ? new String[][] {{"Usuario", username}, {"Numero de empleado", employeeNumber}}
                        : new String[][] {{"Usuario", username}}),
                null,
                null,
                null);

        return sendMessage(to, subject, text, html, "recordatorio de usuario");
    }

    public boolean sendPasswordChangedEmail(String to) {
        String subject = appName + " - Contrasena actualizada";
        String text = "Tu contrasena de " + appName + " fue actualizada correctamente.";
        String html = renderEmail(
                "Contrasena actualizada",
                "<p style=\"margin:0 0 14px;color:#3f3f46;font-size:15px;line-height:1.5;\">El cambio se realizo correctamente.</p>",
                "",
                null,
                null,
                "Si no reconoces este cambio, contacta al administrador.");

        return sendMessage(to, subject, text, html, "confirmacion de cambio de contrasena");
    }

    private boolean sendMessage(String to, String subject, String text, String html, String emailType) {
        lastFailureSummary.remove();
        JavaMailSender mailSender = dynamicMailSenderProvider.getMailSender();
        if (mailSender == null) {
            String failure = "No hay servidor de correo configurado para enviar mensajes.";
            lastFailureSummary.set(failure);
            log.warn("{} No se envio correo de {} a {}", failure, emailType, to);
            return false;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception htmlError) {
            log.warn("Fallo envio HTML de {} a {}. Intentando texto plano: {}", emailType, to, htmlError.getMessage());
            try {
                SimpleMailMessage fallback = new SimpleMailMessage();
                fallback.setFrom(from);
                fallback.setTo(to);
                fallback.setSubject(subject);
                fallback.setText(text);
                mailSender.send(fallback);
                return true;
            } catch (Exception textError) {
                String failure = summarizeMailFailure(textError);
                lastFailureSummary.set(failure);
                log.error("Fallo envio de {} a {}: {}", emailType, to, failure);
                return false;
            }
        }
    }

    @Override
    public String getLastFailureSummary() {
        return lastFailureSummary.get();
    }

    private String renderEmail(String title, String introHtml, String detailHtml,
            String actionText, String actionUrl, String note) {
        String escapedUrl = actionUrl != null ? escapeHtml(actionUrl) : "";
        String actionHtml = actionText != null && actionUrl != null
                ? "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:20px 0 10px;\"><tr><td>"
                        + "<a href=\"" + escapedUrl + "\" style=\"display:inline-block;background:#691228;color:#ffffff;text-decoration:none;"
                        + "font-size:14px;font-weight:700;padding:13px 20px;border-radius:6px;\">"
                        + escapeHtml(actionText) + "</a></td></tr></table>"
                        + "<p style=\"margin:10px 0 0;color:#6b7280;font-size:11px;line-height:1.45;\">Enlace alterno:<br>"
                        + "<span style=\"word-break:break-all;color:#691228;\">" + escapedUrl + "</span></p>"
                : "";
        String noteHtml = note != null && !note.isBlank()
                ? "<p style=\"margin:18px 0 0;color:#6b7280;font-size:12px;line-height:1.45;\">" + escapeHtml(note) + "</p>"
                : "";

        return "<!doctype html><html lang=\"es\"><head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f5f5f5;font-family:Arial,Helvetica,sans-serif;color:#2f2f2f;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f5f5f5;padding:24px 12px;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:620px;background:#ffffff;"
                + "border:1px solid #e6e1dc;border-radius:10px;overflow:hidden;\">"
                + "<tr><td style=\"background:#691228;padding:18px 28px;border-bottom:4px solid #D4C19C;\">"
                + "<div style=\"font-size:22px;color:#ffffff;font-weight:700;line-height:1.2;\">" + escapeHtml(appName) + "</div>"
                + "<div style=\"font-size:12px;color:#D4C19C;margin-top:4px;\">Sistema Integral de Asuntos Juridicos</div>"
                + "</td></tr>"
                + "<tr><td style=\"padding:26px 28px;\">"
                + "<h1 style=\"margin:0 0 12px;color:#4e0c1d;font-size:22px;line-height:1.25;font-weight:700;\">" + escapeHtml(title) + "</h1>"
                + introHtml
                + detailHtml
                + actionHtml
                + noteHtml
                + "</td></tr>"
                + "<tr><td style=\"background:#fafafa;border-top:1px solid #ececec;padding:14px 28px;color:#777777;font-size:12px;line-height:1.4;\">"
                + "Correo automatico de SIAJ."
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String renderInfoBox(String label, String value) {
        return renderDetailBox(new String[][] {{label, value}});
    }

    private String renderDetailBox(String[][] rows) {
        StringBuilder html = new StringBuilder("<div style=\"margin:16px 0;padding:14px 16px;background:#fffaf4;border:1px solid #ece6df;"
                + "border-left:4px solid #D4C19C;border-radius:6px;\">");
        for (String[] row : rows) {
            html.append("<p style=\"margin:0 0 6px;color:#3f3f46;font-size:14px;line-height:1.45;\">")
                    .append("<strong style=\"color:#4e0c1d;\">").append(escapeHtml(row[0])).append(":</strong> ")
                    .append(escapeHtml(row[1])).append("</p>");
        }
        html.append("</div>");
        return html.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String summarizeMailFailure(Exception error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getMessage();
        }
        if (message == null || message.isBlank()) {
            return root.getClass().getSimpleName();
        }
        return root.getClass().getSimpleName() + ": " + message.replaceAll("\\s+", " ").trim();
    }
}
