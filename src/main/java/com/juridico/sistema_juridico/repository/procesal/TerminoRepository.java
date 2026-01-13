package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.Termino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TerminoRepository extends JpaRepository<Termino, Integer> {

    // Encontrar términos que vencen en un rango de fechas (ej. próxima semana)
    // SELECT * FROM terminos WHERE fecha_vencimiento BETWEEN ? AND ?
    List<Termino> findByFechaVencimientoBetween(LocalDate inicio, LocalDate fin);

    // === SEGURIDAD (Delegación) ===
    
    /**
     * Verifica si un abogado es responsable de un término activo en un expediente.
     */
    boolean existsByExpedienteIdAndAbogadoResponsableIdAndEstatusTerminoNot(
            UUID expedienteId, 
            Integer abogadoId, 
            String estatusExcluido
    );
}