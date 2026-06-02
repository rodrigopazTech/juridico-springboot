package com.juridico.sistema_juridico.service.scheduler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.service.configuracion.ConfiguracionAlertaService;

@Component
public class AudienciaNotificacionScheduler {

    @Autowired
    private AudienciaRepository audienciaRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private CatPrioridadRepository catPrioridadRepository;

    @Autowired
    private ConfiguracionAlertaService configuracionAlertaService;

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void notificarAudienciasProximas() {
        LocalDate hoy = LocalDate.now();
        List<Audiencia> audienciasParaNotificar = new ArrayList<>();

        for (Integer diasAnticipacion : configuracionAlertaService.obtenerDiasCandidatos(TipoEventoAlerta.AUDIENCIA)) {
            audienciasParaNotificar.addAll(audienciaRepository.findByFechaAudienciaAndEstatusAudienciaNot(
                    hoy.plusDays(diasAnticipacion), EstatusAudiencia.CONCLUIDA));
        }

        for (Audiencia audiencia : audienciasParaNotificar) {
            Usuario destinatario = obtenerDestinatario(audiencia);
            int diasAnticipacion = (int) ChronoUnit.DAYS.between(hoy, audiencia.getFechaAudiencia());

            if (destinatario == null || !configuracionAlertaService.debeNotificar(
                    TipoEventoAlerta.AUDIENCIA, diasAnticipacion, destinatario,
                    "AUDIENCIA", audiencia.getId().toString())) {
                continue;
            }

            String lugar = Boolean.TRUE.equals(audiencia.getEsVirtual()) ? "Virtual" : audiencia.getSalaLugar();
            String mensaje = String.format("Audiencia programada para el %s a las %s. Sala/Lugar: %s",
                    audiencia.getFechaAudiencia(), audiencia.getHoraAudiencia(), lugar);

            notificacionService.crearNotificacion(
                    destinatario,
                    null,
                    crearTitulo(diasAnticipacion),
                    mensaje,
                    "AUDIENCIA",
                    catPrioridadRepository.findByNombreIgnoreCase("ALTA").orElse(null),
                    String.valueOf(audiencia.getId()),
                    false);
        }
    }

    private String crearTitulo(int diasAnticipacion) {
        if (diasAnticipacion == 0) {
            return "¡AUDIENCIA ES HOY!";
        }
        if (diasAnticipacion == 1) {
            return "¡AUDIENCIA MAÑANA!";
        }
        return "Audiencia en " + diasAnticipacion + " días";
    }

    private Usuario obtenerDestinatario(Audiencia audiencia) {
        if (audiencia.getAbogadoComparece() != null) {
            return audiencia.getAbogadoComparece();
        }
        if (audiencia.getExpediente() != null && audiencia.getExpediente().getAbogadoResponsable() != null) {
            return audiencia.getExpediente().getAbogadoResponsable();
        }
        return null;
    }
}
