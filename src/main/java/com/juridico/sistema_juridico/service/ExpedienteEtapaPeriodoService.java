package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.catalogo.EtapaCatalogo;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.expediente.ExpedienteEtapaPeriodo;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteEtapaPeriodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExpedienteEtapaPeriodoService {

    private final ExpedienteEtapaPeriodoRepository periodoRepository;

    @Transactional
    public ExpedienteEtapaPeriodo asegurarPeriodoActivo(Expediente expediente, Usuario usuario) {
        if (expediente == null || expediente.getId() == null || expediente.getEtapaProcesal() == null) {
            return null;
        }

        return periodoRepository.findByExpedienteIdAndFechaFinIsNull(expediente.getId())
                .map(periodo -> {
                    if (mismoEstado(periodo, expediente.getEtapaCatalogo(), expediente.getEtapaProcesal())) {
                        return periodo;
                    }
                    periodo.setFechaFin(LocalDateTime.now());
                    periodo.setUsuarioFin(usuario);
                    periodoRepository.save(periodo);
                    return abrirPeriodoSinCerrarAnteriores(expediente, usuario,
                            "Periodo ajustado automaticamente por cambio de etapa");
                })
                .orElseGet(() -> abrirPeriodoSinCerrarAnteriores(expediente, usuario,
                        "Periodo inicial generado automaticamente"));
    }

    @Transactional
    public ExpedienteEtapaPeriodo iniciarPeriodoInicial(Expediente expediente, Usuario usuario) {
        if (expediente == null || expediente.getId() == null || expediente.getEtapaProcesal() == null) {
            return null;
        }

        return periodoRepository.findByExpedienteIdAndFechaFinIsNull(expediente.getId())
                .orElseGet(() -> abrirPeriodoSinCerrarAnteriores(expediente, usuario, "Alta del expediente"));
    }

    @Transactional
    public ExpedienteEtapaPeriodo cambiarEtapa(Expediente expediente, EtapaProcesal etapaNueva, Usuario usuario,
            String motivo) {
        if (expediente == null || expediente.getId() == null || etapaNueva == null) {
            return null;
        }

        LocalDateTime ahora = LocalDateTime.now();
        periodoRepository.findByExpedienteIdAndFechaFinIsNull(expediente.getId()).ifPresent(periodo -> {
            periodo.setFechaFin(ahora);
            periodo.setUsuarioFin(usuario);
            periodoRepository.save(periodo);
        });

        ExpedienteEtapaPeriodo nuevoPeriodo = ExpedienteEtapaPeriodo.builder()
                .expediente(expediente)
                .etapaProcesal(etapaNueva)
                .etapaCatalogo(expediente.getEtapaCatalogo())
                .fechaInicio(ahora)
                .usuarioInicio(usuario)
                .motivoCambio(motivo)
                .build();

        return periodoRepository.save(nuevoPeriodo);
    }

    @Transactional
    public ExpedienteEtapaPeriodo cambiarEtapaCatalogo(Expediente expediente, EtapaCatalogo etapaNueva,
            Usuario usuario, String motivo) {
        if (expediente == null || expediente.getId() == null || etapaNueva == null) {
            return null;
        }
        expediente.setEtapaCatalogo(etapaNueva);
        expediente.setEtapaProcesal(Boolean.TRUE.equals(etapaNueva.getCierraExpediente())
                ? EtapaProcesal.CONCLUIDO
                : EtapaProcesal.TRAMITE);
        return cambiarEtapa(expediente, expediente.getEtapaProcesal(), usuario, motivo);
    }

    private ExpedienteEtapaPeriodo abrirPeriodoSinCerrarAnteriores(Expediente expediente, Usuario usuario,
            String motivo) {
        ExpedienteEtapaPeriodo periodo = ExpedienteEtapaPeriodo.builder()
                .expediente(expediente)
                .etapaProcesal(expediente.getEtapaProcesal())
                .etapaCatalogo(expediente.getEtapaCatalogo())
                .fechaInicio(LocalDateTime.now())
                .usuarioInicio(usuario)
                .motivoCambio(motivo)
                .build();

        return periodoRepository.save(periodo);
    }

    private boolean mismoEstado(ExpedienteEtapaPeriodo periodo, EtapaCatalogo etapaCatalogo, EtapaProcesal etapaLegacy) {
        if (etapaCatalogo != null) {
            return periodo.getEtapaCatalogo() != null
                    && periodo.getEtapaCatalogo().getId().equals(etapaCatalogo.getId());
        }
        return periodo.getEtapaCatalogo() == null && periodo.getEtapaProcesal() == etapaLegacy;
    }
}
