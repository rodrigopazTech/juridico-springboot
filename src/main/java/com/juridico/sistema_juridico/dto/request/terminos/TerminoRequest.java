package com.juridico.sistema_juridico.dto.request.terminos;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;

/**
 * DTO para la creación y actualización de términos procesales.
 */
@Data
public class TerminoRequest {

    @NotNull(message = "Debe vincular el término a un expediente")
    private Integer asuntoId;

    @NotBlank(message = "La actuación o asunto es obligatorio")
    private String asunto;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    private LocalDate fechaIngreso;

    private EstatusTermino estatus;
}