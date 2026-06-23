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

@Component
public class RecordatorioNotificacionScheduler {

    @Autowired
    private RecordatorioRepository recordatorioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    // Se ejecuta todos los días a las 7:30 AM
    @Scheduled(cron = "0 30 7 * * ?")
    @Transactional
    public void notificarRecordatoriosHoy() {
        LocalDate hoy = LocalDate.now();
        List<Recordatorio> recordatoriosHoy = recordatorioRepository
                .findByFechaRecordatorioAndCompletadoFalse(hoy);

        for (Recordatorio r : recordatoriosHoy) {
            if (r.getUsuario() == null) {
                continue;
            }

            String tituloNotif = "Tarea para hoy: " + r.getTitulo();
            String mensaje = String.format("Tienes una tarea programada para hoy a las %s. Detalles: %s",
                    r.getHoraRecordatorio() != null ? r.getHoraRecordatorio().toString() : "sin hora",
                    r.getDetalles() != null ? r.getDetalles() : "");

            // Crear y enviar la notificación (al ser personal, silenciamos superiores)
            notificacionService.crearNotificacion(
                    r.getUsuario(),
                    null,
                    NotificacionEvento.TAREA_RECORDATORIO,
                    tituloNotif,
                    mensaje,
                    r.getPrioridad(),
                    String.valueOf(r.getId()),
                    true // Silenciar superiores = true
            );
        }
    }

    // Se ejecuta cada minuto para detectar tareas creadas en el mismo día
    // y notificarlas 15 minutos antes de su vencimiento
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
            LocalTime limiteAviso = r.getHoraRecordatorio().minusMinutes(15);
            if (ahora.isAfter(limiteAviso) || ahora.equals(limiteAviso)) {
                // Verificar si ya fue notificado previamente
                boolean yaNotificado = notificacionRepository.existsByEventoAndEntidadId(
                        NotificacionEvento.TAREA_RECORDATORIO, String.valueOf(r.getId()));

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
                            String.valueOf(r.getId()),
                            true // Silenciar superiores = true
                    );
                }
            }
        }
    }
}
