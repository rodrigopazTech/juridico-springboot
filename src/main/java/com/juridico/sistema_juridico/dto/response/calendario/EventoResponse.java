package com.juridico.sistema_juridico.dto.response.calendario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // Requerido para evitar errores de inferencia
@AllArgsConstructor // Requerido por @Builder
public class EventoResponse {
    private Long id;
    private String titulo;
    private String tipo;      
    private String fecha;     
    private String hora;      
    private Long gerenciaId;
    private Long usuarioId;
    private String expediente; 
    private String detalles;
}