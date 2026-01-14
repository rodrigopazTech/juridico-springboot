package com.juridico.sistema_juridico.dto.request.terminos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PresentacionTerminoRequest {
    @NotBlank(message = "El nombre del archivo de acuse es obligatorio")
    private String acuseDocumento;
    
    private String observaciones;
}