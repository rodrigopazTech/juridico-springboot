package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, UUID> {

    Optional<Expediente> findByNumeroIgnoreCase(String numero);

    // =========================
    // CONSULTA MAESTRA
    // =========================
    @Query("""
        SELECT e FROM Expediente e
        LEFT JOIN e.organoJurisdiccional o
        LEFT JOIN e.abogadoResponsable a
        WHERE
        (:keyword IS NULL OR :keyword = '' OR
            LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(e.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(a.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(o.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId)
        AND (:materiaId IS NULL OR e.materia.id = :materiaId)
        AND (:tipoId IS NULL OR e.tipoExpediente.id = :tipoId)
        AND (:prioridad IS NULL OR e.prioridad = :prioridad)
        AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId)
    """)
    Page<Expediente> buscarExpedientes(
            @Param("keyword") String keyword,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("materiaId") Integer materiaId,
            @Param("tipoId") Integer tipoId,
            @Param("prioridad") Prioridad prioridad,
            @Param("abogadoId") Integer abogadoId,
            Pageable pageable
    );

    // =========================
    // DASHBOARD
    // =========================
    long countByEtapaProcesal(EtapaProcesal etapaProcesal);

    // -------- Carga de trabajo por usuario --------
    @Query("""
        SELECT u.nombreCompleto, COUNT(e)
        FROM Expediente e
        JOIN e.abogadoResponsable u
        GROUP BY u.nombreCompleto
        ORDER BY COUNT(e) DESC
    """)
    List<Object[]> contarExpedientesPorUsuario();

    // -------- Distribución por gerencia --------
    @Query("""
        SELECT g.nombre, COUNT(e)
        FROM Expediente e
        JOIN e.gerencia g
        GROUP BY g.nombre
        ORDER BY COUNT(e) DESC
    """)
    List<Object[]> contarExpedientesPorGerencia();

    // ✅ TRABAJO COMPLETADO POR MES (CORREGIDO)
    @Query("""
        SELECT 
            FUNCTION('to_char', e.updatedAt, 'YYYY-MM'),
            COUNT(e)
        FROM Expediente e
        WHERE e.etapaProcesal IN (:etapas)
          AND e.updatedAt IS NOT NULL
        GROUP BY FUNCTION('to_char', e.updatedAt, 'YYYY-MM')
        ORDER BY FUNCTION('to_char', e.updatedAt, 'YYYY-MM')
    """)
    List<Object[]> contarTrabajoCompletadoMensual(
            @Param("etapas") List<EtapaProcesal> etapas
    );
}
