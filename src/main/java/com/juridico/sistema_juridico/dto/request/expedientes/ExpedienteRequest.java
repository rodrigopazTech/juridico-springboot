package com.juridico.sistema_juridico.dto.request.expedientes;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para la creación y actualización de expedientes jurídicos.
 */
@Data
public class ExpedienteRequest {

    @NotBlank(message = "El número de expediente es obligatorio")
    private String numero;

    @NotNull(message = "La materia es obligatoria")
    private Integer materiaId;

    @NotNull(message = "La gerencia es obligatoria")
    private Integer gerenciaId;

    @NotBlank(message = "La descripción del asunto es necesaria")
    private String descripcion;
}