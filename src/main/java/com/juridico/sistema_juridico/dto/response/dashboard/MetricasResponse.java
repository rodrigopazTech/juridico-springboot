package com.juridico.sistema_juridico.dto.response.dashboard;

import lombok.Data;
import java.util.List;

/**
 * Datos para gráficas de carga de trabajo y rendimiento mensual.
 */
@Data
public class MetricasResponse {
    private List<String> etiquetas; // Nombres de abogados o meses
    private List<Integer> valores;  // Cantidad de expedientes o audiencias
    private String metricaNombre;   // 'Expedientes Asignados' o 'Audiencias Pendientes'
}