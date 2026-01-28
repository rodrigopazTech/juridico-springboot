package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.dto.response.dashboard.EstadisticasResponse;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
    public EstadisticasResponse obtenerKpis() {
        return EstadisticasResponse.builder()
                .totalExpedientes(expedienteRepository.count())
                .expedientesActivos(
                        expedienteRepository.countByEtapaProcesal(EtapaProcesal.TRAMITE)
                )
                .audienciasProgramadas(audienciaRepository.count())
                .terminosActivos(terminoRepository.count())
                .build();
    }

    // =========================
    // GRÁFICAS
    // =========================
    public Map<String, Object> obtenerMetricas() {

        // -------- CARGA POR USUARIO --------
        List<Object[]> cargaTrabajo = expedienteRepository.contarExpedientesPorUsuario();
        List<String> usuarios = cargaTrabajo.stream().map(r -> (String) r[0]).toList();
        List<Long> cantidadesUsuarios = cargaTrabajo.stream().map(r -> (Long) r[1]).toList();

        // -------- DISTRIBUCIÓN POR GERENCIA --------
        List<Object[]> porGerencia = expedienteRepository.contarExpedientesPorGerencia();
        List<String> gerencias = porGerencia.stream().map(r -> (String) r[0]).toList();
        List<Long> cantidadesGerencias = porGerencia.stream().map(r -> (Long) r[1]).toList();

        // -------- TRABAJO COMPLETADO MENSUAL --------
        List<Object[]> trabajoMensual =
                expedienteRepository.contarTrabajoCompletadoMensual(
                        List.of(EtapaProcesal.LAUDO, EtapaProcesal.FIRME)
                );

        List<String> meses = trabajoMensual.stream().map(r -> (String) r[0]).toList();
        List<Long> cantidadesMes = trabajoMensual.stream().map(r -> (Long) r[1]).toList();

        return Map.of(
                "estatusExpedientes", Map.of(
                        "labels", List.of("Trámite", "Laudo", "Firme"),
                        "values", List.of(
                                expedienteRepository.countByEtapaProcesal(EtapaProcesal.TRAMITE),
                                expedienteRepository.countByEtapaProcesal(EtapaProcesal.LAUDO),
                                expedienteRepository.countByEtapaProcesal(EtapaProcesal.FIRME)
                        )
                ),
                "cargaTrabajoUsuarios", Map.of(
                        "labels", usuarios,
                        "values", cantidadesUsuarios
                ),
                "distribucionGerencias", Map.of(
                        "labels", gerencias,
                        "values", cantidadesGerencias
                ),
                // ✅ NUEVO
                "trabajoCompletadoMensual", Map.of(
                        "labels", meses,
                        "values", cantidadesMes
                )
        );
    }
}
