package com.juridico.sistema_juridico.service.scheduler;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.service.TareaService;

@Component
public class RecordatorioNotificacionScheduler {

    @Autowired
    private RecordatorioRepository recordatorioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    // Única regla de tareas: se ejecuta cada minuto y genera el aviso desde
    // 15 minutos antes. Si la tarea se crea dentro de esa ventana, se avisará
    // en el siguiente minuto disponible.
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void notificarRecordatoriosIntraday() {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        List<Recordatorio> recordatoriosHoy = recordatorioRepository
                .findByFechaRecordatorioAndCompletadoFalse(hoy);

        for (Recordatorio r : recordatoriosHoy) {
            if (r.getUsuario() == null || r.getHoraRecordatorio() == null) {
                continue;
            }

            // Si la hora actual es mayor o igual al límite (15 min antes)
            LocalTime limiteAviso = r.getHoraRecordatorio().minusMinutes(TareaService.MINUTOS_ANTICIPACION);
            if (ahora.isAfter(limiteAviso) || ahora.equals(limiteAviso)) {
                // Verificar si ya fue notificado previamente
                boolean yaNotificado = notificacionRepository.existsByEventoAndEntidadId(
                        NotificacionEvento.TAREA_RECORDATORIO, entidadIdRecordatorio(r));

                if (!yaNotificado) {
                    String tituloNotif = "Recordatorio de tarea: " + r.getTitulo();
                    String mensaje = String.format("Tu tarea '%s' está programada para hoy a las %s. Detalles: %s",
                            r.getTitulo(),
                            r.getHoraRecordatorio().toString(),
                            r.getDetalles() != null ? r.getDetalles() : "");

                    notificacionService.crearNotificacion(
                            r.getUsuario(),
                            null,
                            NotificacionEvento.TAREA_RECORDATORIO,
                            tituloNotif,
                            mensaje,
                            r.getPrioridad(),
                            entidadIdRecordatorio(r),
                            true // Silenciar superiores = true
                    );
                }
            }
        }
    }

    private String entidadIdRecordatorio(Recordatorio recordatorio) {
        int revision = recordatorio.getRecordatorioRevision() == null
                ? 0
                : recordatorio.getRecordatorioRevision();
        return revision == 0
                ? String.valueOf(recordatorio.getId())
                : recordatorio.getId() + ":" + revision;
    }
}
