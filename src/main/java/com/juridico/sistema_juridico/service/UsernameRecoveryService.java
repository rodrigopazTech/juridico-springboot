package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsernameRecoveryService {

    public enum UsernameRecoveryResult {
        SENT,
        GENERIC,
        RATE_LIMITED,
        EMAIL_ERROR
    }

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetRateLimitService rateLimitService;
    private final PasswordResetMailer emailService;
    private final AuditoriaTecnicaService auditoriaTecnicaService;

    public UsernameRecoveryResult requestUsernameReminder(String rawEmail, HttpServletRequest request) {
        String email = normalizeEmail(rawEmail);
        String ip = resolveIp(request);
        if (!rateLimitService.isAllowed("username:" + email, ip)) {
            auditoriaTecnicaService.registrarEventoSeguridad(null, email, request,
                    "USERNAME_RECOVERY_RATE_LIMIT", "FALLIDO", 429,
                    "Demasiadas solicitudes para recordar usuario.", "USUARIO", null);
            return UsernameRecoveryResult.RATE_LIMITED;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            auditoriaTecnicaService.registrarEventoSeguridad(null, email, request,
                    "USERNAME_RECOVERY_SOLICITADO", "GENERICO", 202,
                    "Solicitud de recordatorio de usuario para correo no localizado.", "USUARIO", null);
            return UsernameRecoveryResult.GENERIC;
        }

        Usuario usuario = usuarioOpt.get();
        boolean sent = emailService.sendUsernameReminderEmail(usuario.getEmail(), usuario.getUsername(), usuario.getNumeroEmpleado());
        auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                sent ? "USERNAME_RECOVERY_ENVIADO" : "USERNAME_RECOVERY_EMAIL_ERROR",
                sent ? "EXITOSO" : "ERROR", sent ? 202 : 500,
                sent ? "Se envio recordatorio de usuario." : "No fue posible enviar recordatorio de usuario.",
                "USUARIO", String.valueOf(usuario.getId()));
        return sent ? UsernameRecoveryResult.SENT : UsernameRecoveryResult.EMAIL_ERROR;
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
