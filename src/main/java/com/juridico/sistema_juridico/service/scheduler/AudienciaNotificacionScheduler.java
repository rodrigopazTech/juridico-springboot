package com.juridico.sistema_juridico.service.scheduler;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class AudienciaNotificacionScheduler {

    @Autowired
    private AudienciaRepository audienciaRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private CatPrioridadRepository catPrioridadRepository;

    @Scheduled(cron = "0 0 8 * * ?") // Se ejecuta todos los días a las 8:00 AM
    @Transactional
    public void notificarAudienciasProximas() {
        LocalDate hoy = LocalDate.now();
        LocalDate manana = hoy.plusDays(1);
        LocalDate enTresDias = hoy.plusDays(3);

        // 1. Obtener todas las audiencias relevantes usando los métodos que YA EXISTEN
        // en tu Repositorio
        List<Audiencia> audienciasParaNotificar = new ArrayList<>();

        audienciasParaNotificar.addAll(
                audienciaRepository.findByFechaAudienciaAndEstatusAudienciaNot(hoy, EstatusAudiencia.CONCLUIDA));
        audienciasParaNotificar.addAll(
                audienciaRepository.findByFechaAudienciaAndEstatusAudienciaNot(manana, EstatusAudiencia.CONCLUIDA));
        audienciasParaNotificar.addAll(
                audienciaRepository.findByFechaAudienciaAndEstatusAudienciaNot(enTresDias, EstatusAudiencia.CONCLUIDA));

        // 2. Enviar notificaciones
        for (Audiencia a : audienciasParaNotificar) {
            Usuario destinatario = obtenerDestinatario(a);

            if (destinatario != null) {
                // Determinar el mensaje según la cercanía de la fecha
                String titulo = "⏰ ¡RECORDATORIO DE AUDIENCIA!";
                if (a.getFechaAudiencia().equals(hoy)) {
                    titulo = "🚨 ¡AUDIENCIA ES HOY!";
                } else if (a.getFechaAudiencia().equals(manana)) {
                    titulo = "⚠️ ¡AUDIENCIA MAÑANA!";
                }

                String mensaje = "Audiencia programada para el " + a.getFechaAudiencia() + " a las "
                        + a.getHoraAudiencia() + ". Sala/Lugar: "
                        + (a.getEsVirtual() ? "Virtual" : a.getSalaLugar());

                notificacionService.crearNotificacion(
                        destinatario,
                        titulo,
                        mensaje,
                        "AUDIENCIA",
                        catPrioridadRepository.findByNombreIgnoreCase("ALTA").orElse(null),
                        a.getId().toString());
            }
        }
    }

    private Usuario obtenerDestinatario(Audiencia a) {
        if (a.getAbogadoComparece() != null) {
            return a.getAbogadoComparece();
        }
        if (a.getExpediente() != null && a.getExpediente().getAbogadoResponsable() != null) {
            return a.getExpediente().getAbogadoResponsable();
        }
        return null;
    }
}