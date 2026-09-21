package com.juridico.sistema_juridico.dto.request.recordatorios;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class RecordatorioRequest {
    @NotBlank(message = "El título del recordatorio es obligatorio")
    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    private String titulo;

    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha no puede estar en el pasado")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaRecordatorio;

    @NotNull(message = "La hora es obligatoria")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaRecordatorio;

    @NotNull(message = "La prioridad es obligatoria")
    private Integer prioridadId;

    @Size(max = 5000, message = "Los detalles no pueden exceder 5000 caracteres")
    private String detalles;
}
