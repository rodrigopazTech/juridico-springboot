package com.juridico.sistema_juridico.dto.response.notificaciones;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class NotificacionResponse {
    private String id;
    private String eventType; // audiencia, termino, recordatorio
    private String title;
    private String expediente;
    private String status;
    private LocalDateTime notifyAt;
    private Map<String, String> detalles;
    private boolean read;
}