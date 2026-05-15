package com.juridico.sistema_juridico.service.scheduler;

import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduler que revisa cada día a las 7:00 AM si algún usuario tiene
 * la contraseña próxima a vencer (dentro de los 7 días anteriores al
 * límite de 92 días) y genera una notificación de aviso.
 *
 * Regla: contraseña vence a los 92 días desde passwordUpdatedAt (o createdAt).
 *   - Si faltan 7 días o menos → notificación de advertencia (una por día).
 *   - El día en que ya venció → el filtro redirige a /cambiar-password.
 */
@Component
public class PasswordExpirationScheduler {

    private static final int DIAS_VIGENCIA     = 92; // 3 meses aprox.
    private static final int DIAS_AVISO_PREVIO = 7;  // últimos 7 días de aviso

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    /** Se ejecuta todos los días a las 07:00 AM */
    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional
    public void verificarContrasenasPorVencer() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();

        List<Usuario> usuarios = usuarioRepository.findAll();

        for (Usuario usuario : usuarios) {
            if (!Boolean.TRUE.equals(usuario.getActivo())) continue;

            LocalDateTime referencia = (usuario.getPasswordUpdatedAt() != null)
                    ? usuario.getPasswordUpdatedAt()
                    : usuario.getCreatedAt();

            if (referencia == null) continue;

            LocalDate fechaVencimiento = referencia.toLocalDate().plusDays(DIAS_VIGENCIA);
            long diasRestantes = ChronoUnit.DAYS.between(hoy, fechaVencimiento);

            // Solo notificar durante la última semana (1 a 7 días restantes)
            if (diasRestantes >= 1 && diasRestantes <= DIAS_AVISO_PREVIO) {

                // Evitar duplicados: no crear si ya existe una notificación de contraseña HOY
                boolean yaNotificadoHoy = notificacionRepository
                        .findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(usuario.getId())
                        .stream()
                        .anyMatch(n -> "SISTEMA".equals(n.getTipo())
                                && n.getTitulo() != null
                                && n.getTitulo().contains("contraseña")
                                && n.getCreatedAt() != null
                                && n.getCreatedAt().toLocalDate().equals(hoy));

                if (yaNotificadoHoy) continue;

                String titulo;
                String mensaje;

                if (diasRestantes == 1) {
                    titulo = "⚠️ ¡Tu contraseña vence MAÑANA!";
                    mensaje = "Tu contraseña expira mañana (" + fechaVencimiento + "). "
                            + "Si no la cambias hoy, mañana no podrás acceder al sistema. "
                            + "Haz clic aquí para actualizarla ahora.";
                } else {
                    titulo = "🔑 Tu contraseña vence en " + diasRestantes + " días";
                    mensaje = "Tu contraseña expirará el " + fechaVencimiento + " (" + diasRestantes + " días restantes). "
                            + "Actualízala antes de esa fecha para no perder el acceso. "
                            + "Haz clic aquí para ir a Seguridad.";
                }

                Notificacion notif = Notificacion.builder()
                        .usuario(usuario)
                        .titulo(titulo)
                        .mensaje(mensaje)
                        .tipo("SISTEMA")
                        .prioridad(null)
                        .entidadTipo("SEGURIDAD")
                        .entidadId("perfil-seguridad") // redirige a /perfil#seguridad
                        .notificarEn(ahora)
                        .leida(false)
                        .createdAt(ahora)
                        .build();

                notificacionRepository.save(notif);
            }
        }
    }
}
