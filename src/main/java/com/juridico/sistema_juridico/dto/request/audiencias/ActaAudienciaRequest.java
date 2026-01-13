package com.juridico.sistema_juridico.dto.request.audiencias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para registrar el resultado y el acta de una audiencia concluida.
 */
@Data
public class ActaAudienciaRequest {
    
    @NotNull(message = "El ID de la audiencia es requerido")
    private Integer audienciaId;

    @NotBlank(message = " El resumen del acta no puede estar vacío")
    private String resumenActa;

    private String observaciones;
}