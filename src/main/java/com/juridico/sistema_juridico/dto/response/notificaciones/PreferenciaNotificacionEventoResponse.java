package com.juridico.sistema_juridico.dto.response.notificaciones;

import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PreferenciaNotificacionEventoResponse {
    private final NotificacionEvento evento;
    private final String nombre;
    private final String descripcion;
    private final boolean habilitada;
    private final boolean configurable;
}
