package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AudienciaRepository extends JpaRepository<Audiencia, Integer> {

    List<Audiencia> findByExpedienteId(UUID expedienteId);

    // Para la agenda del día: Audiencias de hoy que no estén concluidas
    List<Audiencia> findByFechaAudienciaAndEstatusAudienciaNot(LocalDate fecha, String estatusExcluido);

    // === SEGURIDAD (Delegación) ===
    
    /**
     * Verifica si un abogado tiene una audiencia activa (pendiente) delegada en un expediente.
     * Usado por ExpedienteSecurityService.
     */
    boolean existsByExpedienteIdAndAbogadoCompareceIdAndEstatusAudienciaNot(
            UUID expedienteId, 
            Integer abogadoId, 
            String estatusExcluido
    );
}