package com.juridico.sistema_juridico.dto.response.audiencias;

import lombok.Data;
import java.time.LocalDate;

/**
 * DTO para representar audiencias desahogadas en la Agenda General.
 * Refleja la lógica de sincronización de audiencias.js.
 */
@Data
public class AudienciaResponse {
    private Long id;
    private String asunto; 
    private LocalDate fechaAudiencia;
    private String horaAudiencia;
    private String tipoAudiencia; // Inicial, Intermedia, etc.
    private String partes;
    private String abogado;
    private String resultado; 
    private String actaDocumento; // Nombre del archivo PDF/Word
    private boolean esEnLinea;    // Para mostrar icono de video o mapa
    private String urlReunion;
    private String sala;
    private LocalDate fechaDesahogo; // Fecha real en que se concluyó
}