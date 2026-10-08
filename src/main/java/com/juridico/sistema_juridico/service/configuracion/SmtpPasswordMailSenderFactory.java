package com.juridico.sistema_juridico.service.configuracion;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Properties;

@Component
public class SmtpPasswordMailSenderFactory {

    private final String host;
    private final int port;
    private final String username;
    private final String from;
    private final boolean auth;
    private final boolean startTls;
    private final boolean startTlsRequired;
    private final int connectionTimeout;
    private final int readTimeout;
    private final int writeTimeout;

    public SmtpPasswordMailSenderFactory(
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${app.email.from:${spring.mail.username:}}") String from,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean auth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean startTls,
            @Value("${spring.mail.properties.mail.smtp.starttls.required:false}") boolean startTlsRequired,
            @Value("${spring.mail.properties.mail.smtp.connectiontimeout:10000}") int connectionTimeout,
            @Value("${spring.mail.properties.mail.smtp.timeout:10000}") int readTimeout,
            @Value("${spring.mail.properties.mail.smtp.writetimeout:10000}") int writeTimeout) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.from = from;
        this.auth = auth;
        this.startTls = startTls;
        this.startTlsRequired = startTlsRequired;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
    }

    public JavaMailSender create(String password) {
        validateBaseConfiguration();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host.trim());
        sender.setPort(port);
        sender.setUsername(username.trim());
        sender.setPassword(password);

        Properties mail = sender.getJavaMailProperties();
        mail.put("mail.smtp.auth", String.valueOf(auth));
        mail.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        mail.put("mail.smtp.starttls.required", String.valueOf(startTlsRequired));
        mail.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        mail.put("mail.smtp.timeout", String.valueOf(readTimeout));
        mail.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));
        return sender;
    }

    public void sendTest(JavaMailSender sender, String recipient) throws Exception {
        InternetAddress address = new InternetAddress(recipient, true);
        address.validate();
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        helper.setTo(address.getAddress());
        helper.setSubject("SIAJ - Prueba de configuracion SMTP");
        helper.setText("La credencial SMTP fue validada correctamente el " + LocalDateTime.now()
                + ". Aun debe activarse desde el panel de administracion.");
        sender.send(message);
    }

    public String connectionSummary() {
        String safeHost = host == null || host.isBlank() ? "Sin configurar" : host.trim();
        String safeUser = username == null || username.isBlank() ? "Sin configurar" : maskEmail(username.trim());
        return safeHost + ":" + port + " / " + safeUser;
    }

    private void validateBaseConfiguration() {
        if (host == null || host.isBlank() || username == null || username.isBlank()
                || from == null || from.isBlank()) {
            throw new IllegalStateException("Faltan host, usuario o remitente en la configuracion SMTP base.");
        }
    }

    private String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? value.substring(at) : "");
        }
        return value.substring(0, 1) + "***" + value.substring(at);
    }
}
