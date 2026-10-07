package com.juridico.sistema_juridico.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;

/**
 * Estrategia única y reutilizable para normalizar y generar códigos de catálogos
 * que garantizan cumplir con la restricción VARCHAR(50) de la base de datos,
 * asegurando unicidad mediante sufijos estables (ID o hash).
 */
public final class CodigoNormalizer {

    public static final int MAX_LENGTH = 50;
    private static final int HASH_LENGTH = 8;

    private CodigoNormalizer() {
    }

    /**
     * Normaliza un código o nombre sin sufijo de ID.
     * Si excede los 50 caracteres, trunca la base y añade un hash corto del texto original
     * para evitar colisiones entre nombres largos con el mismo prefijo.
     */
    public static String normalizar(String texto) {
        return normalizar(texto, null);
    }

    /**
     * Normaliza un código o nombre aplicando un sufijo estable (como un ID numérico).
     * Garantiza que la longitud total sea estrictamente <= 50 caracteres.
     */
    public static String normalizar(String texto, Object sufijoId) {
        if (texto == null || texto.isBlank()) {
            texto = "CODIGO";
        }

        // 1. Eliminar acentos y caracteres especiales diacríticos
        String normalizado = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalizado.isBlank()) {
            normalizado = "CODIGO";
        }

        // 2. Si se especifica un sufijo (ej. ID de base de datos)
        if (sufijoId != null && !sufijoId.toString().isBlank()) {
            String sufijo = sufijoId.toString().trim();
            int maxBaseLen = MAX_LENGTH - 1 - sufijo.length(); // 50 - '_' - longitud sufijo
            if (maxBaseLen < 1) {
                return normalizado.substring(0, Math.min(MAX_LENGTH, normalizado.length()));
            }
            String base = normalizado.length() > maxBaseLen
                    ? normalizado.substring(0, maxBaseLen).replaceAll("_+$", "")
                    : normalizado;
            return base + "_" + sufijo;
        }

        // 3. Si no tiene sufijo y cabe exactamente en el límite
        if (normalizado.length() <= MAX_LENGTH) {
            return normalizado;
        }

        // 4. Si no cabe en 50 caracteres y no tiene sufijo, generamos hash SHA-256 corto (8 chars hex)
        String hash = generarHashCorto(texto);
        int maxBaseLen = MAX_LENGTH - 1 - HASH_LENGTH; // 50 - 1 - 8 = 41
        String base = normalizado.substring(0, maxBaseLen).replaceAll("_+$", "");
        return base + "_" + hash;
    }

    private static String generarHashCorto(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02X", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.format("%08X", Math.abs(input.hashCode()));
        }
    }
}
