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

    public DashboardService(ExpedienteRepository expedienteRepository,
            AudienciaRepository audienciaRepository,
            TerminoRepository terminoRepository) {
        this.expedienteRepository = expedienteRepository;
        this.audienciaRepository = audienciaRepository;
        this.terminoRepository = terminoRepository;
    }

    public Map<String, Long> obtenerKpis(Long gerenciaId) {
        Map<String, Long> kpis = new HashMap<>();
        kpis.put("totalExpedientes",
                gerenciaId == null ? expedienteRepository.count() : expedienteRepository.countByGerenciaId(gerenciaId));
        kpis.put("expedientesActivos",
                gerenciaId == null ? expedienteRepository.countByEtapaProcesal(EtapaProcesal.TRAMITE)
                        : expedienteRepository.countByEtapaProcesalAndGerenciaId(EtapaProcesal.TRAMITE, gerenciaId));
        kpis.put("audienciasProgramadas", audienciaRepository.count());
        kpis.put("terminosActivos", terminoRepository.count());
        return kpis;
    }

    public Map<String, Long> obtenerKpisPersonales(Integer usuarioId) {
        Map<String, Long> kpis = new HashMap<>();
        kpis.put("totalExpedientes", expedienteRepository.countByAbogadoResponsableId(usuarioId));
        kpis.put("expedientesActivos",
                expedienteRepository.countByEtapaProcesalAndAbogadoResponsableId(EtapaProcesal.TRAMITE, usuarioId));
        kpis.put("audienciasProgramadas", audienciaRepository.countByAbogadoCompareceId(usuarioId));
        kpis.put("terminosActivos", terminoRepository.countByAbogadoResponsableId(usuarioId));
        return kpis;
    }

    public Map<String, Object> obtenerMetricas(Long gerenciaId) {
        Map<String, Object> data = new HashMap<>();
        data.put("estatusExpedientes", obtenerEstatusExpedientes(gerenciaId));
        data.put("cargaTrabajoUsuarios", obtenerCargaTrabajoUsuarios(gerenciaId));
        data.put("distribucionGerencias", obtenerDistribucionGerencias());
        data.put("trabajoMensual", obtenerTrabajoMensual(gerenciaId));
        return data;
    }

    public Map<String, Object> obtenerMetricasPersonales(Integer usuarioId) {
        Map<String, Object> data = new HashMap<>();
        data.put("estatusExpedientes", obtenerEstatusExpedientesPersonales(usuarioId));
        data.put("trabajoMensual", obtenerTrabajoMensualPersonal(usuarioId));
        return data;
    }

    private Map<String, Object> obtenerEstatusExpedientes(Long gerenciaId) {
        List<Object[]> rows = (gerenciaId == null) ? expedienteRepository.contarExpedientesPorEstatus()
                : expedienteRepository.contarExpedientesPorEstatusYGerencia(gerenciaId);
        Map<EtapaProcesal, Long> statusMap = new HashMap<>();
        for (Object[] row : rows) {
            EtapaProcesal etapa = (EtapaProcesal) row[0];
            Long count = ((Number) row[1]).longValue();
            statusMap.put(etapa, count);
        }
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (EtapaProcesal etapa : EtapaProcesal.values()) {
            labels.add(etapa.name());
            values.add(statusMap.getOrDefault(etapa, 0L));
        }
        return Map.of("labels", labels, "values", values);
    }

    private Map<String, Object> obtenerCargaTrabajoUsuarios(Long gerenciaId) {
        Map<String, Integer> expedientesMap = toMap(expedienteRepository.contarExpedientesPorUsuario());
        Map<String, Integer> audienciasMap = toMap(audienciaRepository.contarAudienciasPorUsuario());
        Map<String, Integer> terminosMap = toMap(terminoRepository.contarTerminosPorUsuario());

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

    private Map<String, Object> obtenerTrabajoMensual(Long gerenciaId) {
        List<Object[]> rows = (gerenciaId != null)
                ? expedienteRepository.contarExpedientesPorEtapaYGerencia(EtapaProcesal.FIRME.name(), gerenciaId)
                : expedienteRepository.contarExpedientesPorEtapa(EtapaProcesal.FIRME.name());
        return processRows(rows);
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
            if (row[0] != null)
                map.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        return map;
    }

    // 👤 ROD-12 Dashboard Personal (Abogados)

    private Map<String, Object> obtenerEstatusExpedientesPersonales(Integer usuarioId) {
        List<Object[]> rows = expedienteRepository.contarExpedientesPorEstatusYAbogado(usuarioId);
        return processStatusRows(rows);
    }

    private Map<String, Object> processStatusRows(List<Object[]> rows) {
        Map<EtapaProcesal, Long> statusMap = new HashMap<>();
        for (Object[] row : rows) {
            EtapaProcesal etapa = (EtapaProcesal) row[0];
            Long count = ((Number) row[1]).longValue();
            statusMap.put(etapa, count);
        }
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (EtapaProcesal etapa : EtapaProcesal.values()) {
            labels.add(etapa.name());
            values.add(statusMap.getOrDefault(etapa, 0L));
        }
        return Map.of("labels", labels, "values", values);
    }

    private Map<String, Object> obtenerTrabajoMensualPersonal(Integer usuarioId) {
        return processRows(
                expedienteRepository.contarExpedientesPorEtapaYAbogado(EtapaProcesal.FIRME.name(), usuarioId));
    }
}