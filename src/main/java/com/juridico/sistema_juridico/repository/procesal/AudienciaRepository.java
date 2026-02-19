package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AudienciaRepository extends JpaRepository<Audiencia, Integer> {

    // --- 1. BUSQUEDA PAGINADA (INDEX) ---
    @Query("SELECT a FROM Audiencia a " +
           "LEFT JOIN a.expediente e " +
           "WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " + 
           "LOWER(a.salaLugar) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CONCAT(a.id, '') LIKE CONCAT('%', :keyword, '%')) " + 
           "AND (:tipo IS NULL OR :tipo = '' OR a.tipoAudiencia.nombre = :tipo) " + 
           "AND (:gerencia IS NULL OR :gerencia = '' OR e.gerencia.nombre = :gerencia) " +
           "AND (:materia IS NULL OR :materia = '' OR e.materia.nombre = :materia) " +
           "AND (:estatus IS NULL OR :estatus = '' OR a.estatusAudiencia = :estatus) " +
           "AND (a.fechaAudiencia >= :fechaInicio) " + 
           "AND (a.fechaAudiencia <= :fechaFin) " +
           "AND (:targetAbogadoId IS NULL OR (e.abogadoResponsable.id = :targetAbogadoId OR a.abogadoComparece.id = :targetAbogadoId)) " +
           "AND (:filtroUsuarioId IS NULL OR (e.abogadoResponsable.id = :filtroUsuarioId OR a.abogadoComparece.id = :filtroUsuarioId)) " +
           "AND (:filtroGerenciaId IS NULL OR e.gerencia.id = :filtroGerenciaId) " +
           "AND ((:filtroMateriaIds) IS NULL OR e.materia.id IN (:filtroMateriaIds))")
    Page<Audiencia> buscarConFiltros(
            @Param("keyword") String keyword,
            @Param("tipo") String tipo,
            @Param("gerencia") String gerencia,
            @Param("materia") String materia,
            @Param("estatus") String estatus,
            @Param("targetAbogadoId") Integer targetAbogadoId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("filtroUsuarioId") Integer filtroUsuarioId,
            @Param("filtroGerenciaId") Integer filtroGerenciaId,
            @Param("filtroMateriaIds") List<Integer> filtroMateriaIds,
            Pageable pageable);

    // --- 2. LISTADO PARA EXCEL (Sin paginación) ---
    @Query("SELECT a FROM Audiencia a " +
           "LEFT JOIN a.expediente e " +
           "WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " + 
           "LOWER(a.salaLugar) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CONCAT(a.id, '') LIKE CONCAT('%', :keyword, '%')) " + 
           "AND (:tipo IS NULL OR :tipo = '' OR a.tipoAudiencia.nombre = :tipo) " + 
           "AND (:gerencia IS NULL OR :gerencia = '' OR e.gerencia.nombre = :gerencia) " +
           "AND (:materia IS NULL OR :materia = '' OR e.materia.nombre = :materia) " +
           "AND (:estatus IS NULL OR :estatus = '' OR a.estatusAudiencia = :estatus) " +
           "AND (a.fechaAudiencia >= :fechaInicio) " +
           "AND (a.fechaAudiencia <= :fechaFin) " +
           "AND (:targetAbogadoId IS NULL OR (e.abogadoResponsable.id = :targetAbogadoId OR a.abogadoComparece.id = :targetAbogadoId)) " +
           "AND (:filtroUsuarioId IS NULL OR (e.abogadoResponsable.id = :filtroUsuarioId OR a.abogadoComparece.id = :filtroUsuarioId)) " +
           "AND (:filtroGerenciaId IS NULL OR e.gerencia.id = :filtroGerenciaId) " +
           "AND ((:filtroMateriaIds) IS NULL OR e.materia.id IN (:filtroMateriaIds))")
    List<Audiencia> listarParaExcel(
            @Param("keyword") String keyword,
            @Param("tipo") String tipo,
            @Param("gerencia") String gerencia,
            @Param("materia") String materia,
            @Param("estatus") String estatus,
            @Param("targetAbogadoId") Integer targetAbogadoId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("filtroUsuarioId") Integer filtroUsuarioId,
            @Param("filtroGerenciaId") Integer filtroGerenciaId,
            @Param("filtroMateriaIds") List<Integer> filtroMateriaIds);

    // --- 3. MÉTODOS AUXILIARES ---
    List<Audiencia> findByFechaAudienciaAndEstatusAudienciaNot(LocalDate fecha, String estatusExcluido);

    List<Audiencia> findByFechaAudienciaAndHoraAudienciaBetweenAndEstatusAudienciaNot(
        LocalDate fecha, 
        LocalTime horaInicio, 
        LocalTime horaFin, 
        String estatusExcluido
    );

    Optional<Audiencia> findTopByExpedienteIdAndFechaAudienciaAfterOrderByFechaAudienciaAsc(
        UUID expedienteId, 
        LocalDate fechaActual
    );

    List<Audiencia> findByExpedienteIdAndFechaAudiencia(UUID expedienteId, LocalDate fecha);

    // --- 4. CARGA DE TRABAJO POR USUARIO (from aurora5) ---
    @Query("""
        SELECT u.nombreCompleto, COUNT(a)
        FROM Audiencia a
        JOIN a.expediente e
        JOIN e.abogadoResponsable u
        GROUP BY u.nombreCompleto
    """)
    List<Object[]> contarAudienciasPorUsuario();

    @Query("""
        SELECT u.nombreCompleto, COUNT(a)
        FROM Audiencia a
        JOIN a.expediente e
        JOIN e.abogadoResponsable u
        WHERE e.gerencia.id = :gerenciaId
        GROUP BY u.nombreCompleto
    """)
    List<Object[]> contarAudienciasPorUsuarioYGerencia(@Param("gerenciaId") Long gerenciaId);

    @Query("""
        SELECT u.nombreCompleto, COUNT(a)
        FROM Audiencia a
        JOIN a.expediente e
        JOIN e.abogadoResponsable u
        WHERE e.gerencia.id IN :gerenciaIds
        GROUP BY u.nombreCompleto
    """)
    List<Object[]> contarAudienciasPorUsuarioYGerencias(@Param("gerenciaIds") List<Long> gerenciaIds);
}
