package com.juridico.sistema_juridico.dto.response.notificaciones;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GrupoPreferenciaNotificacionResponse {
    private final String categoria;
    private final List<PreferenciaNotificacionEventoResponse> eventos;
}
