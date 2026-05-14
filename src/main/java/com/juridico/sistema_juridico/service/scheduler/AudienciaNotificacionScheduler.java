package com.juridico.sistema_juridico.service.scheduler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.service.NotificacionService;

@Component
public class AudienciaNotificacionScheduler {

    @Autowired
    private AudienciaRepository audienciaRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private CatPrioridadRepository catPrioridadRepository;

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void notificarAudienciasProximas() {
        LocalDate hoy = LocalDate.now();
        LocalDate manana = hoy.plusDays(1);
        LocalDate enTresDias = hoy.plusDays(3);

        List<Audiencia> audienciasParaNotificar = new ArrayList<>();
        audienciasParaNotificar.addAll(audienciaRepository.findByFechaAudienciaAndEstatusAudienciaNot(hoy, EstatusAudiencia.CONCLUIDA));
        audienciasParaNotificar.addAll(audienciaRepository.findByFechaAudienciaAndEstatusAudienciaNot(manana, EstatusAudiencia.CONCLUIDA));
        audienciasParaNotificar.addAll(audienciaRepository.findByFechaAudienciaAndEstatusAudienciaNot(enTresDias, EstatusAudiencia.CONCLUIDA));

        for (Audiencia aud : audienciasParaNotificar) {
            Usuario destinatario = obtenerDestinatario(aud);

            if (destinatario != null) {
                String titulo = "⏰ ¡RECORDATORIO DE AUDIENCIA!";
                if (aud.getFechaAudiencia().equals(hoy)) {
                    titulo = "🚨 ¡AUDIENCIA ES HOY!";
                } else if (aud.getFechaAudiencia().equals(manana)) {
                    titulo = "⚠️ ¡AUDIENCIA MAÑANA!";
                }

                String lugar = (aud.getEsVirtual() != null && aud.getEsVirtual()) ? "Virtual" : aud.getSalaLugar();
                String mensaje = String.format("Audiencia programada para el %s a las %s. Sala/Lugar: %s",
                        aud.getFechaAudiencia(), aud.getHoraAudiencia(), lugar);

                notificacionService.crearNotificacion(
                        destinatario,
                        null,
                        titulo,
                        mensaje,
                        "AUDIENCIA",
                        catPrioridadRepository.findByNombreIgnoreCase("ALTA").orElse(null),
                        String.valueOf(aud.getId()),
                        false
                );
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