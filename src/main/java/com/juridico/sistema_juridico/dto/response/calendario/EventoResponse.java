package com.juridico.sistema_juridico.dto.response.calendario;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Representación unificada de cualquier evento en el calendario.
 */
@Data
public class EventoResponse {
    private Long id;
    private String titulo;
    private String tipo; // 'audiencia', 'termino', 'recordatorio'
    private LocalDate fecha;
    private LocalTime hora;
    private String colorSolid; // Para respetar tus 'gobColors'
    private Integer asuntoId; // ID del expediente relacionado
}