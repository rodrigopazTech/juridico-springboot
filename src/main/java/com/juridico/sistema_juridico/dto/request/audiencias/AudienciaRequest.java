package com.juridico.sistema_juridico.dto.request.audiencias;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AudienciaRequest {
    @NotNull(message = "El ID del expediente es obligatorio")
    private Integer asuntoId;
    
    @NotBlank(message = "El tipo de audiencia es obligatorio")
    private String tipoAudiencia;
    
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
    
    private String hora;
    private String sala;
}