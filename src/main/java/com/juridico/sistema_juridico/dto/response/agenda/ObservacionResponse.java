package com.juridico.sistema_juridico.dto.response.agenda;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ObservacionResponse {
    private String expediente;
    private String contenido;
}