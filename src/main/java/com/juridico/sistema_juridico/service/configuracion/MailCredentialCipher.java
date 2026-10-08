package com.juridico.sistema_juridico.service.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class MailCredentialCipher {

    private static final String PREFIX = "v1:";
    private static final byte[] AAD = "SIAJ_SMTP_PASSWORD_V1".getBytes(StandardCharsets.UTF_8);
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec key;

    public MailCredentialCipher(
            @Value("${app.email.credential-encryption-key:${MAIL_CREDENTIAL_ENCRYPTION_KEY:}}") String encodedKey) {
        this.key = parseKey(encodedKey);
    }

    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(String plaintext) {
        requireConfigured();
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("La contrasena SMTP no puede estar vacia.");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(AAD);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No fue posible cifrar la credencial SMTP.", ex);
        }
    }

    public String decrypt(String encrypted) {
        requireConfigured();
        if (encrypted == null || !encrypted.startsWith(PREFIX)) {
            throw new IllegalStateException("La credencial SMTP tiene un formato de cifrado no soportado.");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
            if (payload.length <= IV_LENGTH) {
                throw new IllegalStateException("La credencial SMTP cifrada esta incompleta.");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new IllegalStateException("No fue posible descifrar la credencial SMTP.", ex);
        }
    }

    private SecretKeySpec parseKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encodedKey.trim());
            if (bytes.length != 32) {
                throw new IllegalStateException(
                        "MAIL_CREDENTIAL_ENCRYPTION_KEY debe ser una llave de 32 bytes codificada en Base64.");
            }
            return new SecretKeySpec(bytes, "AES");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("MAIL_CREDENTIAL_ENCRYPTION_KEY no contiene Base64 valido.", ex);
        }
    }

    private void requireConfigured() {
        if (key == null) {
            throw new IllegalStateException(
                    "Configure MAIL_CREDENTIAL_ENCRYPTION_KEY antes de administrar credenciales SMTP.");
        }
    }
}
