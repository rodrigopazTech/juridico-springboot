package com.juridico.sistema_juridico.dto.response.expedientes;

import lombok.Data;
import java.util.List;

/**
 * DTO detallado para la vista 360 del expediente.
 */
@Data
public class ExpedienteDetalleResponse {
    private Integer id;
    private String numero;
    private String materia;
    private String gerencia;
    private String abogado;
    private String sede;
    private String partes;
    private String organo;
    private String prioridad;
    private String estado;
    private String descripcion;
    
    // Relaciones para el Timeline y Explorador
    private List<ActividadDTO> actividad;
    private List<DocumentoDTO> documentos;
}