package com.juridico.sistema_juridico.service;

public interface PasswordResetMailer {

    boolean sendResetEmail(String to, String resetUrl, int expirationMinutes);

    boolean sendActivationEmail(String to, String username, String activationUrl, int expirationMinutes);

    boolean sendUsernameReminderEmail(String to, String username, String employeeNumber);

    boolean sendPasswordChangedEmail(String to);

    default String getLastFailureSummary() {
        return null;
    }
}
