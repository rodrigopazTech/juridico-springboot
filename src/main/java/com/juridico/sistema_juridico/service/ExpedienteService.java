package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpedienteService {

    private final ExpedienteRepository expedienteRepository;

    public ExpedienteService(ExpedienteRepository expedienteRepository) {
        this.expedienteRepository = expedienteRepository;
    }

    // Corregido: Incluye parámetros de seguridad para RBAC
    public Page<Expediente> buscarExpedientes(
            String keyword, Integer gerenciaId, Integer materiaId, Integer tipoId,
            Prioridad prioridad, Integer abogadoId,
            Integer secGerenciaId, List<Integer> secMateriaIds, Integer secUsuarioId,
            Pageable pageable) {
        return expedienteRepository.buscarConSeguridad(
                keyword, gerenciaId, materiaId, tipoId, prioridad, null, abogadoId,
                secGerenciaId, secMateriaIds, secUsuarioId, pageable);
    }

    // Corregido: count() es el método estándar de JpaRepository
    public long totalExpedientes() {
        return expedienteRepository.count();
    }

    // Corregido: Nombres vinculados a las @Query de tu Repository
    public List<Object[]> contarPorEtapaProcesal() {
        return expedienteRepository.contarExpedientesPorEstatus();
    }

    public List<Object[]> contarPorAbogado() {
        return expedienteRepository.contarExpedientesPorUsuario();
    }

    public List<Object[]> contarPorGerencia() {
        return expedienteRepository.contarExpedientesPorGerencia();
    }

    public List<Object[]> contarExpedientesPorMes() {
        return expedienteRepository.contarExpedientesPorMes();
    }
}