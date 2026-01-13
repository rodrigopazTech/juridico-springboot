package com.juridico.sistema_juridico.dto.request.calendario;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Datos de entrada para crear un nuevo evento desde el calendario.
 */
@Data
public class EventoRequest {
    @NotBlank(message = "El tipo de evento es obligatorio")
    private String tipo;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private String hora;

    @NotNull(message = "Debe asignar una gerencia")
    private Integer gerenciaId;

    @NotNull(message = "Debe asignar un usuario responsable")
    private Integer usuarioId;
}
