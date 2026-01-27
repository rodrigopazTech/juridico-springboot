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
                // 🔥 mientras no tengas campo "concluida"
                .audienciasProgramadas(audienciaRepository.count())
                .terminosActivos(terminoRepository.count())
                .build();
    }

    // =========================
    // GRÁFICAS
    // =========================
    public Map<String, Object> obtenerMetricas() {
        return Map.of(
                "estatusExpedientes", Map.of(
                        "labels", List.of("Trámite", "Laudo", "Firme"),
                        "values", List.of(
                                expedienteRepository.countByEtapaProcesal(EtapaProcesal.TRAMITE),
                                expedienteRepository.countByEtapaProcesal(EtapaProcesal.LAUDO),
                                expedienteRepository.countByEtapaProcesal(EtapaProcesal.FIRME)
                              //  expedienteRepository.countByEtapaProcesal(EtapaProcesal.CONCLUIDO)
                        )
                )
        );
    }
}
