package com.juridico.sistema_juridico.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Keeps account-management flows available while SMTP credentials are pending.
 */
@Service
@ConditionalOnProperty(name = "app.email.maintenance", havingValue = "true")
public class PasswordResetMaintenanceMailer implements PasswordResetMailer {

    private static final String FAILURE = "El envio de correo esta pendiente de configuracion SMTP.";

    @Override
    public boolean sendResetEmail(String to, String resetUrl, int expirationMinutes) {
        return false;
    }

    @Override
    public boolean sendActivationEmail(String to, String username, String activationUrl, int expirationMinutes) {
        return false;
    }

    @Override
    public boolean sendUsernameReminderEmail(String to, String username, String employeeNumber) {
        return false;
    }

    @Override
    public boolean sendPasswordChangedEmail(String to) {
        return false;
    }

    @Override
    public String getLastFailureSummary() {
        return FAILURE;
    }
}
