package com.juridico.sistema_juridico.service.scheduler;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class TerminoNotificacionScheduler {

    @Autowired
    private TerminoRepository terminoRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    // Ejecutar todos los días a las 08:30 AM
    @Scheduled(cron = "0 30 8 * * ?")
    @Transactional
    public void notificarTerminosProximos() {
        notificarPorDiasRestantes(3, "📅 Término vence en 3 días", Prioridad.MEDIA);
        notificarPorDiasRestantes(1, "⚠️ ¡Término vence MAÑANA!", Prioridad.ALTA);
        notificarPorDiasRestantes(0, "🔥 ¡Término vence HOY!", Prioridad.ALTA);
    }

    private void notificarPorDiasRestantes(int dias, String tituloBase, Prioridad prioridad) {
        LocalDate fechaObjetivo = LocalDate.now().plusDays(dias);

        // Buscar términos que vencen en la fecha objetivo y NO están concluidos
        List<Termino> terminos = terminoRepository.findByFechaVencimientoAndEstatusTerminoNot(
                fechaObjetivo, EstatusTermino.CONCLUIDO); // Asumiendo que CONCLUIDO es el estatus final

        for (Termino t : terminos) {
            // Validar que tampoco esté PRESENTADO si esa es otra condición de "finalizado"
            if (t.getEstatusTermino() == EstatusTermino.PRESENTADO) {
                continue;
            }

            Usuario destinatario = t.getAbogadoResponsable();
            if (destinatario != null) {
                String mensaje = "Expediente: " + t.getExpediente().getNumero() +
                        ". Actuación: " + t.getActuacion() +
                        ". Vence el " + t.getFechaVencimiento();

                crearNotificacion(
                        destinatario,
                        tituloBase,
                        mensaje,
                        "TERMINO",
                        t.getId().toString(),
                        prioridad);
            }
        }

        if (!terminos.isEmpty()) {
            System.out.println("--- Scheduler Términos: " + terminos.size()
                    + " notificaciones enviadas para vencimiento en " + dias + " días.");
        }
    }

    private void crearNotificacion(Usuario user, String titulo, String mensaje, String entidadTipo, String entidadId,
            Prioridad prioridad) {
        Notificacion n = new Notificacion();
        n.setUsuario(user);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setTipo("SISTEMA");
        n.setEntidadTipo(entidadTipo);
        n.setEntidadId(entidadId);
        n.setPrioridad(prioridad);
        n.setNotificarEn(LocalDateTime.now());
        n.setLeida(false);
        n.setCreatedAt(LocalDateTime.now());

        notificacionRepository.save(n);
    }
}
