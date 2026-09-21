package com.juridico.sistema_juridico.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class SmtpOAuth2MailSenderFactory {

    private final SmtpOAuth2AccessTokenProvider accessTokenProvider;

    @Value("${spring.mail.host:smtp.office365.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:10000}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:10000}")
    private int readTimeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:10000}")
    private int writeTimeout;

    public SmtpOAuth2MailSenderFactory(SmtpOAuth2AccessTokenProvider accessTokenProvider) {
        this.accessTokenProvider = accessTokenProvider;
    }

    public JavaMailSender create() {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Falta configurar MAIL_USERNAME para SMTP OAuth2.");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username.trim());
        sender.setPassword(accessTokenProvider.getAccessToken());

        Properties mail = sender.getJavaMailProperties();
        mail.put("mail.smtp.auth", "true");
        mail.put("mail.smtp.starttls.enable", "true");
        mail.put("mail.smtp.starttls.required", "true");
        mail.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        mail.put("mail.smtp.auth.xoauth2.disable", "false");
        mail.put("mail.smtp.auth.login.disable", "true");
        mail.put("mail.smtp.auth.plain.disable", "true");
        mail.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        mail.put("mail.smtp.timeout", String.valueOf(readTimeout));
        mail.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));
        return sender;
    }
}
