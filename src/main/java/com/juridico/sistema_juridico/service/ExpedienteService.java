package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.dto.request.expedientes.ExpedienteRequest;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpedienteService {

    private final ExpedienteRepository expedienteRepository;
    private final MateriaRepository materiaRepository;
    private final GerenciaRepository gerenciaRepository;

    public ExpedienteService(ExpedienteRepository expedienteRepository,
            MateriaRepository materiaRepository,
            GerenciaRepository gerenciaRepository) {
        this.expedienteRepository = expedienteRepository;
        this.materiaRepository = materiaRepository;
        this.gerenciaRepository = gerenciaRepository;
    }

    @Transactional
    public Expediente guardarExpediente(ExpedienteRequest request) {
        Materia materia = materiaRepository.findById(request.getMateriaId())
                .orElseThrow(() -> new RuntimeException("Materia no encontrada"));
        Gerencia gerencia = gerenciaRepository.findById(request.getGerenciaId())
                .orElseThrow(() -> new RuntimeException("Gerencia no encontrada"));

        Expediente nuevo = Expediente.builder()
                .numero(request.getNumero())
                .descripcion(request.getDescripcion())
                .materia(materia)
                .gerencia(gerencia)
                .etapaProcesal(EtapaProcesal.TRAMITE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return expedienteRepository.save(nuevo);
    }

    // Corregido: Coincide con los 7 parámetros y tipos Integer del Repository
    public Page<Expediente> buscarExpedientes(String folio, Integer gerenciaId, Integer materiaId, Integer prioridadId,
            Integer abogadoId, Pageable pageable) {
        return expedienteRepository.buscarExpedientes(folio, gerenciaId, materiaId, prioridadId, abogadoId, pageable);
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