package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.PasswordResetToken;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.request.auth.PasswordResetConfirmRequest;
import com.juridico.sistema_juridico.repository.Usuarios.PasswordResetTokenRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    public enum ResetRequestResult {
        SENT,
        GENERIC,
        RATE_LIMITED,
        EMAIL_ERROR
    }

    public enum ConfirmResetResult {
        SUCCESS,
        INVALID_TOKEN,
        PASSWORD_MISMATCH,
        WEAK_PASSWORD
    }

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetRateLimitService rateLimitService;
    private final PasswordResetMailer emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaTecnicaService auditoriaTecnicaService;
    private final PasswordPolicyService passwordPolicyService;
    private static final SecureRandom TEMP_PASSWORD_RANDOM = new SecureRandom();
    private static final char[] TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();

    @Value("${app.password-reset.expiration-minutes:30}")
    private int expirationMinutes;

    @Value("${app.password-reset.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public ResetRequestResult requestPasswordReset(String rawIdentifier, HttpServletRequest request) {
        String identifier = normalizeIdentifier(rawIdentifier);
        String ip = resolveIp(request);

        if (!rateLimitService.isAllowed(identifier, ip)) {
            auditoriaTecnicaService.registrarEventoSeguridad(null, identifier, request,
                    "PASSWORD_RESET_RATE_LIMIT", "DENEGADO", 429,
                    "Solicitud de recuperacion bloqueada por rate limit.", "USUARIO", identifier);
            return ResetRequestResult.RATE_LIMITED;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByLoginIdentifier(identifier);
        if (usuarioOpt.isEmpty() || !Boolean.TRUE.equals(usuarioOpt.get().getActivo())) {
            auditoriaTecnicaService.registrarEventoSeguridad(null, identifier, request,
                    "PASSWORD_RESET_SOLICITADO", "EXITOSO", 202,
                    "Solicitud generica de recuperacion para usuario no encontrado o inactivo.", "USUARIO", identifier);
            return ResetRequestResult.GENERIC;
        }

        Usuario usuario = usuarioOpt.get();
        PasswordResetToken token = createToken(usuario);

        boolean sent = emailService.sendResetEmail(usuario.getEmail(), buildResetUrl(token.getToken()), expirationMinutes);
        if (!sent) {
            token.setActivo(false);
            tokenRepository.save(token);
            String detalle = mailFailureDetail("No fue posible enviar el correo de recuperacion.");
            auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                    "PASSWORD_RESET_EMAIL_ERROR", "ERROR", 500,
                    detalle, "USUARIO", String.valueOf(usuario.getId()));
            return ResetRequestResult.EMAIL_ERROR;
        }

        auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                "PASSWORD_RESET_SOLICITADO", "EXITOSO", 202,
                "Se envio correo de recuperacion de contrasena.", "USUARIO", String.valueOf(usuario.getId()));
        return ResetRequestResult.SENT;
    }

    @Transactional
    public boolean sendActivationLink(Usuario usuario, HttpServletRequest request) {
        PasswordResetToken token = createToken(usuario);
        boolean sent = emailService.sendActivationEmail(usuario.getEmail(), usuario.getUsername(),
                buildActivationUrl(token.getToken()), expirationMinutes);
        auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                sent ? "CUENTA_ACTIVACION_ENVIADA" : "CUENTA_ACTIVACION_EMAIL_ERROR",
                sent ? "EXITOSO" : "ERROR", sent ? 202 : 500,
                sent ? "Se envio enlace de activacion de cuenta."
                        : mailFailureDetail("No fue posible enviar enlace de activacion."),
                "USUARIO", String.valueOf(usuario.getId()));
        if (!sent) {
            token.setActivo(false);
            tokenRepository.save(token);
        }
        return sent;
    }

    @Transactional
    public boolean sendResetLinkForUser(Usuario usuario, HttpServletRequest request) {
        PasswordResetToken token = createToken(usuario);
        boolean sent = emailService.sendResetEmail(usuario.getEmail(), buildResetUrl(token.getToken()), expirationMinutes);
        auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                sent ? "PASSWORD_RESET_ADMIN_ENVIADO" : "PASSWORD_RESET_ADMIN_EMAIL_ERROR",
                sent ? "EXITOSO" : "ERROR", sent ? 202 : 500,
                sent ? "Se envio enlace de restablecimiento por administrador."
                        : mailFailureDetail("No fue posible enviar enlace de restablecimiento por administrador."),
                "USUARIO", String.valueOf(usuario.getId()));
        if (!sent) {
            token.setActivo(false);
            tokenRepository.save(token);
        }
        return sent;
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        return tokenRepository.findFirstByTokenAndActivoTrue(token)
                .filter(t -> !Boolean.TRUE.equals(t.getUsado()))
                .filter(t -> t.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> findUserForValidToken(String token) {
        return tokenRepository.findFirstByTokenAndActivoTrue(token)
                .filter(t -> !Boolean.TRUE.equals(t.getUsado()))
                .filter(t -> t.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .map(PasswordResetToken::getUsuario);
    }

    @Transactional
    public String createTemporaryPassword(Usuario usuario, String requestedPassword, HttpServletRequest request) {
        String temporaryPassword = requestedPassword == null || requestedPassword.isBlank()
                ? generateTemporaryPassword()
                : requestedPassword.trim();

        if (temporaryPassword.length() < 8) {
            throw new IllegalArgumentException("La contrasena temporal debe tener al menos 8 caracteres.");
        }
        if (!temporaryPassword.matches("[A-Za-z0-9!@#$%._-]+")) {
            throw new IllegalArgumentException("La contrasena temporal solo puede usar letras, numeros y ! @ # $ % . _ -");
        }

        tokenRepository.invalidateActiveTokensByUsuarioId(usuario.getId());
        usuario.setPassword(passwordEncoder.encode(temporaryPassword));
        usuario.setForcePasswordChange(true);
        usuario.setPasswordUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);

        auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                "PASSWORD_TEMPORAL_ADMIN_GENERADA", "EXITOSO", 200,
                "Se genero una contrasena temporal administrativa y se forzo cambio al iniciar sesion.",
                "USUARIO", String.valueOf(usuario.getId()));
        return temporaryPassword;
    }

    @Transactional
    public ConfirmResetResult confirmReset(PasswordResetConfirmRequest resetRequest, HttpServletRequest request) {
        if (!resetRequest.getNuevaContrasena().equals(resetRequest.getConfirmacionContrasena())) {
            auditoriaTecnicaService.registrarEventoSeguridad(null, null, request,
                    "PASSWORD_RESET_VALIDACION_FALLIDA", "FALLIDO", 400,
                    "La confirmacion de contrasena no coincide.", "PASSWORD_RESET", null);
            return ConfirmResetResult.PASSWORD_MISMATCH;
        }
        if (!passwordPolicyService.isValid(resetRequest.getNuevaContrasena())) {
            auditoriaTecnicaService.registrarEventoSeguridad(null, null, request,
                    "PASSWORD_RESET_VALIDACION_FALLIDA", "FALLIDO", 400,
                    "La nueva contrasena no cumple la politica de seguridad.", "PASSWORD_RESET", null);
            return ConfirmResetResult.WEAK_PASSWORD;
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findFirstByTokenAndActivoTrue(resetRequest.getToken());
        if (tokenOpt.isEmpty()) {
            auditoriaTecnicaService.registrarEventoSeguridad(null, null, request,
                    "PASSWORD_RESET_TOKEN_INVALIDO", "FALLIDO", 400,
                    "Token de recuperacion inexistente o inactivo.", "PASSWORD_RESET", null);
            return ConfirmResetResult.INVALID_TOKEN;
        }

        PasswordResetToken token = tokenOpt.get();
        if (Boolean.TRUE.equals(token.getUsado()) || token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            token.setActivo(false);
            tokenRepository.save(token);
            auditoriaTecnicaService.registrarEventoSeguridad(token.getUsuario(), token.getEmail(), request,
                    "PASSWORD_RESET_TOKEN_INVALIDO", "FALLIDO", 400,
                    "Token de recuperacion usado o expirado.", "PASSWORD_RESET", String.valueOf(token.getId()));
            return ConfirmResetResult.INVALID_TOKEN;
        }

        Usuario usuario = token.getUsuario();
        usuario.setPassword(passwordEncoder.encode(resetRequest.getNuevaContrasena()));
        usuario.setForcePasswordChange(false);
        usuario.setPasswordUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);

        token.setUsado(true);
        token.setActivo(false);
        token.setFechaUso(LocalDateTime.now());
        tokenRepository.save(token);

        auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                "PASSWORD_RESET_CONFIRMADO", "EXITOSO", 200,
                "Contrasena actualizada por recuperacion con token.", "USUARIO", String.valueOf(usuario.getId()));
        emailService.sendPasswordChangedEmail(usuario.getEmail());
        return ConfirmResetResult.SUCCESS;
    }

    private String buildResetUrl(String token) {
        String cleanBase = baseUrl != null ? baseUrl.trim() : "";
        if (cleanBase.endsWith("/")) {
            cleanBase = cleanBase.substring(0, cleanBase.length() - 1);
        }
        return cleanBase + "/auth/reset-password?token=" + token;
    }

    private String buildActivationUrl(String token) {
        String cleanBase = baseUrl != null ? baseUrl.trim() : "";
        if (cleanBase.endsWith("/")) {
            cleanBase = cleanBase.substring(0, cleanBase.length() - 1);
        }
        return cleanBase + "/auth/activar?token=" + token;
    }

    private PasswordResetToken createToken(Usuario usuario) {
        tokenRepository.invalidateActiveTokensByUsuarioId(usuario.getId());
        PasswordResetToken token = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .usuario(usuario)
                .email(usuario.getEmail())
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(expirationMinutes))
                .usado(false)
                .activo(true)
                .build();
        return tokenRepository.save(token);
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(TEMP_PASSWORD_CHARS[TEMP_PASSWORD_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length)]);
        }
        return password.toString();
    }

    private String normalizeIdentifier(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String mailFailureDetail(String baseMessage) {
        String failure = emailService.getLastFailureSummary();
        if (failure == null || failure.isBlank()) {
            return baseMessage;
        }
        return baseMessage + " Causa tecnica: " + failure;
    }
}
