package com.juridico.sistema_juridico.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;

import org.springframework.stereotype.Service;

@Service
public class OutlookTokenCipher {

    private final MicrosoftAuthProperties properties;

    public OutlookTokenCipher(MicrosoftAuthProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible cifrar el token de Outlook.", ex);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key());
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible descifrar el token de Outlook.", ex);
        }
    }

    private SecretKeySpec key() throws Exception {
        String secret = properties.getTokenEncryptionSecret();
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException("Configura app.microsoft.token-encryption-secret con al menos 16 caracteres.");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(digest, 16), "AES");
    }
}
