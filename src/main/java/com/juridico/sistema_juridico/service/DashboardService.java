package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    private final ExpedienteRepository expedienteRepository;
    private final AudienciaRepository audienciaRepository;
    private final TerminoRepository terminoRepository;

    public DashboardService(
            ExpedienteRepository expedienteRepository,
            AudienciaRepository audienciaRepository,
            TerminoRepository terminoRepository
    ) {
        this.expedienteRepository = expedienteRepository;
        this.audienciaRepository = audienciaRepository;
        this.terminoRepository = terminoRepository;
    }

    // =========================
    // KPIs
    // =========================
    public Map<String, Long> obtenerKpis() {
        Map<String, Long> kpis = new HashMap<>();

        kpis.put("totalExpedientes", expedienteRepository.count());
        kpis.put("expedientesActivos", expedienteRepository.count());
        kpis.put("audienciasProgramadas", audienciaRepository.count());
        kpis.put("terminosActivos", terminoRepository.count());

        return kpis;
    }

    // =========================
    // MÉTRICAS DASHBOARD
    // =========================
    public Map<String, Object> obtenerMetricas() {
        Map<String, Object> data = new HashMap<>();

        data.put("estatusExpedientes", obtenerEstatusExpedientes());
        data.put("cargaTrabajoUsuarios", obtenerCargaTrabajoUsuarios());
        data.put("distribucionGerencias", obtenerDistribucionGerencias());
        data.put("trabajoMensual", obtenerTrabajoMensual());

        return data;
    }

    // =========================
    // ESTATUS EXPEDIENTES
    // =========================
    private Map<String, Object> obtenerEstatusExpedientes() {
        List<Object[]> rows = expedienteRepository.contarExpedientesPorEstatus();

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        for (Object[] row : rows) {
            labels.add(row[0].toString());
            values.add((Long) row[1]);
        }

        return Map.of(
                "labels", labels,
                "values", values
        );
    }

    // =========================
    // CARGA DE TRABAJO
    // =========================
    private Map<String, Object> obtenerCargaTrabajoUsuarios() {

        Map<String, Integer> expedientesMap =
                toMap(expedienteRepository.contarExpedientesPorUsuario());

        Map<String, Integer> audienciasMap =
                toMap(audienciaRepository.contarAudienciasPorUsuario());

        Map<String, Integer> terminosMap =
                toMap(terminoRepository.contarTerminosPorUsuario());

        Set<String> usuarios = new LinkedHashSet<>();
        usuarios.addAll(expedientesMap.keySet());
        usuarios.addAll(audienciasMap.keySet());
        usuarios.addAll(terminosMap.keySet());

        List<String> labels = new ArrayList<>();
        List<Integer> expedientes = new ArrayList<>();
        List<Integer> audiencias = new ArrayList<>();
        List<Integer> terminos = new ArrayList<>();

        for (String usuario : usuarios) {
            labels.add(usuario);
            expedientes.add(expedientesMap.getOrDefault(usuario, 0));
            audiencias.add(audienciasMap.getOrDefault(usuario, 0));
            terminos.add(terminosMap.getOrDefault(usuario, 0));
        }

        Map<String, Object> chart = new HashMap<>();
        chart.put("labels", labels);
        chart.put("expedientes", expedientes);
        chart.put("audiencias", audiencias);
        chart.put("terminos", terminos);

        return chart;
    }

    // =========================
    // GERENCIAS
    // =========================
    private Map<String, Object> obtenerDistribucionGerencias() {
        List<Object[]> rows = expedienteRepository.contarExpedientesPorGerencia();

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        for (Object[] row : rows) {
            labels.add((String) row[0]);
            values.add((Long) row[1]);
        }

        return Map.of(
                "labels", labels,
                "values", values
        );
    }

    // =========================
    // TRABAJO MENSUAL
    // =========================
    private Map<String, Object> obtenerTrabajoMensual() {
        List<Object[]> rows = expedienteRepository.contarExpedientesPorMes();

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        for (Object[] row : rows) {
            labels.add("Mes " + row[0]);
            values.add((Long) row[1]);
        }

        return Map.of(
                "labels", labels,
                "values", values
        );
    }

    // =========================
    // UTILIDAD
    // =========================
    private Map<String, Integer> toMap(List<Object[]> rows) {
        Map<String, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((String) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }
}
