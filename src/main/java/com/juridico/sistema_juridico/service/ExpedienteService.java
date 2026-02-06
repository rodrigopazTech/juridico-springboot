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

    // Corregido: Coincide con los 7 parámetros y tipos Integer del Repository
    public Page<Expediente> buscarExpedientes(String folio, Integer gerenciaId, Integer materiaId, Integer tipoId, Prioridad prioridad, Integer abogadoId, Pageable pageable) {
        return expedienteRepository.buscarExpedientes(folio, gerenciaId, materiaId, tipoId, prioridad, abogadoId, pageable);
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