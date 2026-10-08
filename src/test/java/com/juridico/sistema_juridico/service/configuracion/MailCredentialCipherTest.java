package com.juridico.sistema_juridico.service.configuracion;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailCredentialCipherTest {

    @Test
    void encryptsAndDecryptsWithoutPersistingPlaintext() {
        MailCredentialCipher cipher = new MailCredentialCipher(randomKey());

        String encrypted = cipher.encrypt("smtp-secret-30-days");

        assertNotEquals("smtp-secret-30-days", encrypted);
        assertEquals("smtp-secret-30-days", cipher.decrypt(encrypted));
    }

    @Test
    void rejectsDecryptingWithAnotherKey() {
        MailCredentialCipher first = new MailCredentialCipher(randomKey());
        MailCredentialCipher second = new MailCredentialCipher(randomKey());

        String encrypted = first.encrypt("smtp-secret");

        assertThrows(IllegalStateException.class, () -> second.decrypt(encrypted));
    }

    @Test
    void requiresConfiguredKeyBeforeEncrypting() {
        MailCredentialCipher cipher = new MailCredentialCipher("");

        assertThrows(IllegalStateException.class, () -> cipher.encrypt("smtp-secret"));
    }

    private String randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
