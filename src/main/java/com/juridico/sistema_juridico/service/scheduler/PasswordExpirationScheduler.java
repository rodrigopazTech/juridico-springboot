package com.juridico.sistema_juridico.service.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@Component
public class PasswordExpirationScheduler {

    private static final int DIAS_VIGENCIA = 92;
    private static final int DIAS_AVISO_PREVIO = 7;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional
    public void verificarContrasenasPorVencer() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();

        List<Usuario> usuarios = usuarioRepository.findAll();

        for (Usuario usuario : usuarios) {
            if (!Boolean.TRUE.equals(usuario.getActivo())) {
                continue;
            }

            LocalDateTime referencia = usuario.getPasswordUpdatedAt() != null
                    ? usuario.getPasswordUpdatedAt()
                    : usuario.getCreatedAt();

            if (referencia == null) {
                continue;
            }

            LocalDate fechaVencimiento = referencia.toLocalDate().plusDays(DIAS_VIGENCIA);
            long diasRestantes = ChronoUnit.DAYS.between(hoy, fechaVencimiento);

            if (diasRestantes < 1 || diasRestantes > DIAS_AVISO_PREVIO) {
                continue;
            }

            boolean yaNotificadoHoy = notificacionRepository
                    .findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(usuario.getId())
                    .stream()
                    .anyMatch(n -> n.getEvento() == NotificacionEvento.SEGURIDAD_PASSWORD_POR_VENCER
                            && n.getCreatedAt() != null
                            && n.getCreatedAt().toLocalDate().equals(hoy));

            if (yaNotificadoHoy) {
                continue;
            }

            String titulo;
            String mensaje;

            if (diasRestantes == 1) {
                titulo = "Tu contrasena vence manana";
                mensaje = "Tu contrasena expira manana (" + fechaVencimiento + "). "
                        + "Si no la cambias hoy, manana no podras acceder al sistema. "
                        + "Haz clic aqui para actualizarla ahora.";
            } else {
                titulo = "Tu contrasena vence en " + diasRestantes + " dias";
                mensaje = "Tu contrasena expirara el " + fechaVencimiento + " (" + diasRestantes + " dias restantes). "
                        + "Actualizala antes de esa fecha para no perder el acceso. "
                        + "Haz clic aqui para ir a Seguridad.";
            }

            Notificacion notif = Notificacion.builder()
                    .usuario(usuario)
                    .titulo(titulo)
                    .mensaje(mensaje)
                    .tipo("SISTEMA")
                    .evento(NotificacionEvento.SEGURIDAD_PASSWORD_POR_VENCER)
                    .prioridad(null)
                    .entidadTipo("SEGURIDAD")
                    .entidadId("perfil-seguridad")
                    .notificarEn(ahora)
                    .leida(false)
                    .createdAt(ahora)
                    .build();

            notificacionRepository.save(notif);
        }
    }
}
