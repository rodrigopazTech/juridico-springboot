package com.juridico.sistema_juridico.dto.response.dashboard;

import lombok.Builder;
import lombok.Data;

/**
 * Datos para los KPIs del Dashboard.
 */
@Data
@Builder
public class EstadisticasResponse {
    private int totalExpedientes;
    private int expedientesActivos; // e.estado !== 'CONCLUIDO'
    private int audienciasProgramadas; // !a.atendida
    private int terminosActivos; // t.estatus !== 'Concluido'
}