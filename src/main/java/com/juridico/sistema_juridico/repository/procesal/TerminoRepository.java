package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TerminoRepository extends JpaRepository<Termino, Integer> {

// CONSULTA MAESTRA DE FILTROS
    @Query("SELECT t FROM Termino t " +
           "LEFT JOIN t.expediente e " +
           "LEFT JOIN t.abogadoResponsable a " +
           "WHERE " +
           // 1. Buscador de Texto (Actuación, Expediente o Partes)
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(t.actuacion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           // 2. Filtros Específicos
           "AND (:estatus IS NULL OR :estatus = '' OR t.estatusTermino = :estatus) " +
           "AND (:prioridad IS NULL OR t.prioridad = :prioridad) " +
           "AND (:abogadoId IS NULL OR a.id = :abogadoId)")
    Page<Termino> buscarConFiltros(
            @Param("keyword") String keyword,
            @Param("estatus") String estatus,
            @Param("prioridad") Prioridad prioridad,
            @Param("abogadoId") Integer abogadoId,
            Pageable pageable);


    // Cambiado: 'estatusTermino' para que coincida con la Entity
    List<Termino> findByFechaVencimientoBeforeAndEstatusTerminoNot(LocalDate fecha, String estatus);

    // Cambiado: UUID para que coincida con el ID de Expediente
    List<Termino> findByExpedienteId(UUID expedienteId);
}