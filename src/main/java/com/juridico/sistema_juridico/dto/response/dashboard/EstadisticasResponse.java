package com.juridico.sistema_juridico.dto.response.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstadisticasResponse {
    private long totalExpedientes;
    private long expedientesActivos;
    private long audienciasProgramadas;
    private long terminosActivos;
}

