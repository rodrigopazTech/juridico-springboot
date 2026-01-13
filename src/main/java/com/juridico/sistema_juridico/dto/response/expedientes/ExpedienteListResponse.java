package com.juridico.sistema_juridico.dto.response.expedientes;

import lombok.Data;

/**
 * DTO para la lista general de expedientes.
 * Basado en la función buildCard de expedientes.js.
 */
@Data
public class ExpedienteListResponse {
    private Integer id;
    private String numero;
    private String descripcion;
    private String materia;
    private String abogado;
    private String ultimaActividad;
    private String estado; // TRAMITE, LAUDO, FIRME
    private String prioridad; // Alta, Media, Baja
}