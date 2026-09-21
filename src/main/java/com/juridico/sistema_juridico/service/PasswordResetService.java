package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.PasswordResetToken;
import com.juridico.sistema_juridico.Entity.usuario.ManualRecoveryCode;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.request.auth.PasswordResetConfirmRequest;
import com.juridico.sistema_juridico.repository.Usuarios.PasswordResetTokenRepository;
import com.juridico.sistema_juridico.repository.Usuarios.ManualRecoveryCodeRepository;
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

    public enum ManualRecoveryResult {
        SUCCESS,
        INVALID_CODE,
        INACTIVE_USER,
        PASSWORD_MISMATCH,
        WEAK_PASSWORD
    }

    public record ManualRecoveryCodeResult(String code, LocalDateTime expiresAt) {
    }

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final ManualRecoveryCodeRepository manualRecoveryCodeRepository;
    private final PasswordResetRateLimitService rateLimitService;
    private final PasswordResetMailer emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaTecnicaService auditoriaTecnicaService;
    private final PasswordPolicyService passwordPolicyService;
    private final SessionRevocationService sessionRevocationService;
    private static final SecureRandom TEMP_PASSWORD_RANDOM = new SecureRandom();
    private static final char[] MANUAL_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

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
    public ManualRecoveryCodeResult createManualRecoveryCode(Usuario usuario, Usuario actor, String motivo,
            String origen, HttpServletRequest request) {
        String normalizedReason = motivo == null ? "" : motivo.trim();
        if (normalizedReason.length() < 10) {
            throw new IllegalArgumentException("Indica un motivo de al menos 10 caracteres para la recuperacion manual.");
        }

        manualRecoveryCodeRepository.invalidateActiveCodesByUsuarioId(usuario.getId());
        tokenRepository.invalidateActiveTokensByUsuarioId(usuario.getId());
        String code = generateManualCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        manualRecoveryCodeRepository.save(ManualRecoveryCode.builder()
                .usuario(usuario)
                .emitidoPor(actor)
                .codigoHash(passwordEncoder.encode(code))
                .motivo(normalizedReason)
                .origen(origen)
                .fechaExpiracion(expiresAt)
                .usado(false)
                .activo(true)
                .build());

        auditoriaTecnicaService.registrarRecuperacionManual(actor, usuario, request,
                "MANUAL_RECOVERY_CODE_ISSUED", "EXITOSO", 201,
                "Se emitio un codigo manual de un uso. Origen: " + origen + ". Motivo: " + normalizedReason);
        return new ManualRecoveryCodeResult(code, expiresAt);
    }

    @Transactional
    public ManualRecoveryResult confirmManualRecovery(String rawIdentifier, String code, String newPassword,
            String confirmation, HttpServletRequest request) {
        if (newPassword == null || !newPassword.equals(confirmation)) {
            return ManualRecoveryResult.PASSWORD_MISMATCH;
        }
        if (!passwordPolicyService.isValid(newPassword)) {
            return ManualRecoveryResult.WEAK_PASSWORD;
        }

        Optional<Usuario> userOpt = usuarioRepository.findByLoginIdentifier(normalizeIdentifier(rawIdentifier));
        if (userOpt.isEmpty()) {
            auditoriaTecnicaService.registrarEventoSeguridad(null, rawIdentifier, request,
                    "MANUAL_RECOVERY_CODE_INVALID", "FALLIDO", 400,
                    "Intento de recuperacion manual para una cuenta inexistente.", "USUARIO", rawIdentifier);
            return ManualRecoveryResult.INVALID_CODE;
        }

        Usuario usuario = userOpt.get();
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                    "MANUAL_RECOVERY_INACTIVE_USER", "DENEGADO", 409,
                    "Se rechazo la recuperacion manual porque la cuenta esta inactiva.",
                    "USUARIO", String.valueOf(usuario.getId()));
            return ManualRecoveryResult.INACTIVE_USER;
        }
        Optional<ManualRecoveryCode> codeOpt = manualRecoveryCodeRepository
                .findFirstByUsuarioIdAndActivoTrueAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId());
        if (codeOpt.isEmpty() || codeOpt.get().getFechaExpiracion().isBefore(LocalDateTime.now())
                || code == null || !passwordEncoder.matches(code.trim(), codeOpt.get().getCodigoHash())) {
            codeOpt.ifPresent(saved -> {
                if (saved.getFechaExpiracion().isBefore(LocalDateTime.now())) {
                    saved.setActivo(false);
                    manualRecoveryCodeRepository.save(saved);
                }
            });
            auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                    "MANUAL_RECOVERY_CODE_INVALID", "FALLIDO", 400,
                    "Se intento usar un codigo manual invalido, vencido o ya utilizado.", "USUARIO", String.valueOf(usuario.getId()));
            return ManualRecoveryResult.INVALID_CODE;
        }

        ManualRecoveryCode recoveryCode = codeOpt.get();
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuario.setForcePasswordChange(false);
        usuario.setPasswordUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
        recoveryCode.setUsado(true);
        recoveryCode.setActivo(false);
        recoveryCode.setFechaUso(LocalDateTime.now());
        manualRecoveryCodeRepository.save(recoveryCode);
        tokenRepository.invalidateActiveTokensByUsuarioId(usuario.getId());
        sessionRevocationService.revokeAll(usuario, "RECUPERACION_MANUAL");
        auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                "MANUAL_RECOVERY_CODE_USED", "EXITOSO", 200,
                "El usuario establecio una nueva contrasena con un codigo manual de un uso.",
                "USUARIO", String.valueOf(usuario.getId()));
        return ManualRecoveryResult.SUCCESS;
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

        sessionRevocationService.revokeAll(usuario, "PASSWORD_RESET");

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

    private String generateManualCode() {
        StringBuilder code = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            code.append(MANUAL_CODE_CHARS[TEMP_PASSWORD_RANDOM.nextInt(MANUAL_CODE_CHARS.length)]);
        }
        return code.toString();
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
