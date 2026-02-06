package com.juridico.sistema_juridico.dto.response.calendario;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventoResponse {
    private Long id;
    private String titulo;
    private String tipo;      // "audiencia", "termino", "recordatorio"
    private String fecha;     // Formato "yyyy-MM-dd"
    private String hora;      // Formato "HH:mm"
    private Long gerenciaId;
    private Long usuarioId;
    private String expediente; 
    private String detalles;
}