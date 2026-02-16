package com.juridico.sistema_juridico.service.scheduler;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class AudienciaNotificacionScheduler {

    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private NotificacionRepository notificacionRepository;

    // @Scheduled(fixedRate = 10000) // <--- NUEVA (PARA PROBAR) comenta el otro y
    // descomenta este para pruebas
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void notificarAudienciasProximas() {
        LocalDate fechaObjetivo = LocalDate.now().plusDays(3);

        List<Audiencia> audiencias = audienciaRepository.findByFechaAudienciaAndEstatusAudienciaNot(
                fechaObjetivo, EstatusAudiencia.CONCLUIDA);

        for (Audiencia a : audiencias) {
            Usuario destinatario = obtenerDestinatario(a);
            if (destinatario != null) {
                crearNotificacion(
                        destinatario,
                        "📅 Audiencia en 3 Días",
                        "Expediente: " + a.getExpediente().getNumero() + ". Prepárate para el " + fechaObjetivo,
                        "AUDIENCIA",
                        a.getId().toString(),
                        Prioridad.MEDIA);
            }
        }
        System.out.println("--- Scheduler: " + audiencias.size() + " recordatorios de 3 días enviados.");
    }

    // @Scheduled(fixedRate = 10000) // <--- NUEVA (PARA PROBAR) comenta el otro y
    // descomenta este para pruebas
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void notificarAudienciasManana() {
        LocalDate manana = LocalDate.now().plusDays(1);

        List<Audiencia> audiencias = audienciaRepository.findByFechaAudienciaAndEstatusAudienciaNot(
                manana, EstatusAudiencia.CONCLUIDA);

        for (Audiencia a : audiencias) {
            Usuario destinatario = obtenerDestinatario(a);
            if (destinatario != null) {
                crearNotificacion(
                        destinatario,
                        "⚠️ ¡Audiencia MAÑANA!",
                        "Expediente: " + a.getExpediente().getNumero() + ". Prepárate, es mañana a las "
                                + a.getHoraAudiencia(),
                        "AUDIENCIA",
                        a.getId().toString(),
                        Prioridad.ALTA);
            }
        }
        System.out.println("--- Scheduler: " + audiencias.size() + " recordatorios de mañana enviados.");
    }

    // @Scheduled(fixedRate = 10000) // <--- NUEVA (PARA PROBAR) comenta el otro y
    // descomenta este para pruebas
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void notificarAudienciasHoy() {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        LocalTime enDosHoras = ahora.plusHours(2);

        List<Audiencia> audiencias = audienciaRepository
                .findByFechaAudienciaAndHoraAudienciaBetweenAndEstatusAudienciaNot(
                        hoy, ahora, enDosHoras, EstatusAudiencia.CONCLUIDA);

        for (Audiencia a : audiencias) {
            Usuario destinatario = obtenerDestinatario(a);
            if (destinatario != null) {
                crearNotificacion(
                        destinatario,
                        "⏰ ¡AUDIENCIA INMINENTE!",
                        "Es hoy a las " + a.getHoraAudiencia() + ". Sala/Link: "
                                + (a.getEsVirtual() ? "Virtual" : a.getSalaLugar()),
                        "AUDIENCIA",
                        a.getId().toString(),
                        Prioridad.ALTA);
            }
        }
    }

    private Usuario obtenerDestinatario(Audiencia a) {
        if (a.getAbogadoComparece() != null)
            return a.getAbogadoComparece();
        if (a.getExpediente().getAbogadoResponsable() != null)
            return a.getExpediente().getAbogadoResponsable();
        return null;
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