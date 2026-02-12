package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal; 
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DashboardService {

    private final ExpedienteRepository expedienteRepository;
    private final AudienciaRepository audienciaRepository;
    private final TerminoRepository terminoRepository;

    // Constantes para grupos de gerencias
    public static final List<Long> GRUPO_CIVIL_MERCANTIL = Arrays.asList(1L, 2L, 3L, 4L); // Civil, Mercantil, Fiscal, Administrativo
    public static final List<Long> GRUPO_LABORAL_PENAL = Arrays.asList(5L, 6L); // Laboral, Penal
    public static final List<Long> GRUPO_TRANSPARENCIA = Arrays.asList(7L, 8L); // Transparencia, Amparo

    public DashboardService(ExpedienteRepository expedienteRepository,
                            AudienciaRepository audienciaRepository,
                            TerminoRepository terminoRepository) {
        this.expedienteRepository = expedienteRepository;
        this.audienciaRepository = audienciaRepository;
        this.terminoRepository = terminoRepository;
    }

    /**
     * Convierte un grupo de gerencia a lista de IDs
     */
    public List<Long> obtenerListaGerencias(String grupoGerencia) {
        if (grupoGerencia == null) return null;
        return switch (grupoGerencia) {
            case "CIVIL_MERCANTIL" -> GRUPO_CIVIL_MERCANTIL;
            case "LABORAL_PENAL" -> GRUPO_LABORAL_PENAL;
            case "TRANSPARENCIA" -> GRUPO_TRANSPARENCIA;
            default -> null;
        };
    }

    public Map<String, Long> obtenerKpis(Long gerenciaId) {
        return obtenerKpisPorLista(gerenciaId, null);
    }

    public Map<String, Long> obtenerKpisPorLista(Long gerenciaId, List<Long> gerenciaIds) {
        Map<String, Long> kpis = new HashMap<>();
        
        // Si hay lista de gerencias, usar esa; si hay gerenciaId individual, usar esa
        boolean hayFiltro = gerenciaIds != null && !gerenciaIds.isEmpty();
        
        if (gerenciaId != null) {
            // Filtro por gerencia individual
            kpis.put("totalExpedientes", expedienteRepository.countByGerenciaId(gerenciaId));
            kpis.put("expedientesActivos", expedienteRepository.countByEtapaProcesalAndGerenciaId(EtapaProcesal.TRAMITE, gerenciaId));
        } else if (hayFiltro) {
            // Filtro por lista de gerencias (grupo)
            kpis.put("totalExpedientes", expedienteRepository.countByGerenciaIdIn(gerenciaIds));
            kpis.put("expedientesActivos", expedienteRepository.countByEtapaProcesalAndGerenciaIdIn(EtapaProcesal.TRAMITE, gerenciaIds));
        } else {
            // Sin filtro - todos los datos
            kpis.put("totalExpedientes", expedienteRepository.count());
            kpis.put("expedientesActivos", expedienteRepository.countByEtapaProcesal(EtapaProcesal.TRAMITE));
        }
        
        kpis.put("audienciasProgramadas", audienciaRepository.count());
        kpis.put("terminosActivos", terminoRepository.count());
        return kpis;
    }

    public Map<String, Object> obtenerMetricas(Long gerenciaId) {
        return obtenerMetricasPorLista(gerenciaId, null);
    }

    public Map<String, Object> obtenerMetricasPorLista(Long gerenciaId, List<Long> gerenciaIds) {
        Map<String, Object> data = new HashMap<>();
        data.put("estatusExpedientes", obtenerEstatusExpedientes(gerenciaId, gerenciaIds));
        data.put("cargaTrabajoUsuarios", obtenerCargaTrabajoUsuarios(gerenciaId, gerenciaIds));
        data.put("distribucionGerencias", obtenerDistribucionGerencias());
        data.put("trabajoMensual", obtenerTrabajoMensual(gerenciaId, gerenciaIds));
        return data;
    }

    private Map<String, Object> obtenerEstatusExpedientes(Long gerenciaId, List<Long> gerenciaIds) {
        List<Object[]> rows;
        
        if (gerenciaId != null) {
            rows = expedienteRepository.contarExpedientesPorEstatusYGerencia(gerenciaId);
        } else if (gerenciaIds != null && !gerenciaIds.isEmpty()) {
            rows = expedienteRepository.contarExpedientesPorEstatusYGerencias(gerenciaIds);
        } else {
            rows = expedienteRepository.contarExpedientesPorEstatus();
        }
        
        Map<EtapaProcesal, Long> statusMap = new HashMap<>();
        for (Object[] row : rows) {
            EtapaProcesal etapa = (EtapaProcesal) row[0];
            Long count = ((Number) row[1]).longValue();
            statusMap.put(etapa, count);
        }
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (EtapaProcesal etapa : Arrays.asList(EtapaProcesal.TRAMITE, EtapaProcesal.LAUDO, EtapaProcesal.FIRME)) {
            labels.add(etapa.name());
            values.add(statusMap.getOrDefault(etapa, 0L));
        }
        return Map.of("labels", labels, "values", values);
    }

    private Map<String, Object> obtenerCargaTrabajoUsuarios(Long gerenciaId, List<Long> gerenciaIds) {
        Map<String, Integer> expedientesMap;
        Map<String, Integer> audienciasMap;
        Map<String, Integer> terminosMap;

        if (gerenciaId != null) {
            // Filtro por gerencia individual
            expedientesMap = toMap(expedienteRepository.contarExpedientesPorUsuarioYGerencia(gerenciaId));
            audienciasMap = toMap(audienciaRepository.contarAudienciasPorUsuarioYGerencia(gerenciaId));
            terminosMap = toMap(terminoRepository.contarTerminosPorUsuarioYGerencia(gerenciaId));
        } else if (gerenciaIds != null && !gerenciaIds.isEmpty()) {
            // Filtro por lista de gerencias (grupo)
            expedientesMap = toMap(expedienteRepository.contarExpedientesPorUsuarioYGerencias(gerenciaIds));
            audienciasMap = toMap(audienciaRepository.contarAudienciasPorUsuarioYGerencias(gerenciaIds));
            terminosMap = toMap(terminoRepository.contarTerminosPorUsuarioYGerencias(gerenciaIds));
        } else {
            // Sin filtro
            expedientesMap = toMap(expedienteRepository.contarExpedientesPorUsuario());
            audienciasMap = toMap(audienciaRepository.contarAudienciasPorUsuario());
            terminosMap = toMap(terminoRepository.contarTerminosPorUsuario());
        }

        Set<String> usuarios = new LinkedHashSet<>();
        usuarios.addAll(expedientesMap.keySet());
        usuarios.addAll(audienciasMap.keySet());
        usuarios.addAll(terminosMap.keySet());

        List<String> labels = new ArrayList<>();
        List<Integer> expedientes = new ArrayList<>();
        List<Integer> audiencias = new ArrayList<>();
        List<Integer> terminos = new ArrayList<>();

        for (String u : usuarios) {
            labels.add(u);
            expedientes.add(expedientesMap.getOrDefault(u, 0));
            audiencias.add(audienciasMap.getOrDefault(u, 0));
            terminos.add(terminosMap.getOrDefault(u, 0));
        }
        return Map.of("labels", labels, "expedientes", expedientes, "audiencias", audiencias, "terminos", terminos);
    }

    private Map<String, Object> obtenerDistribucionGerencias() {
        return processRows(expedienteRepository.contarExpedientesPorGerencia());
    }

    private Map<String, Object> obtenerTrabajoMensual(Long gerenciaId, List<Long> gerenciaIds) {
        if (gerenciaId != null) {
            return processRows(expedienteRepository.contarExpedientesCompletadosPorMes());
        } else if (gerenciaIds != null && !gerenciaIds.isEmpty()) {
            return processRows(expedienteRepository.contarExpedientesCompletadosPorMesYGerencias(gerenciaIds));
        } else {
            return processRows(expedienteRepository.contarExpedientesCompletadosPorMes());
        }
    }

    private Map<String, Object> processRows(List<Object[]> rows) {
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (Object[] row : rows) {
            labels.add(row[0] != null ? row[0].toString() : "N/A");
            values.add(row[1] != null ? ((Number) row[1]).longValue() : 0L);
        }
        return Map.of("labels", labels, "values", values);
    }

    private Map<String, Integer> toMap(List<Object[]> rows) {
        Map<String, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) map.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        return map;
    }
}
