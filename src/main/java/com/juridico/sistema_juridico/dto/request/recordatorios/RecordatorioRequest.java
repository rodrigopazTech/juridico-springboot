package com.juridico.sistema_juridico.dto.request.recordatorios;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RecordatorioRequest {
    @NotBlank(message = "El título del recordatorio es obligatorio")
    private String titulo;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @Pattern(regexp = "^(normal|urgent)$", message = "Prioridad debe ser normal o urgent")
    private String prioridad;

    private String detalles;
}