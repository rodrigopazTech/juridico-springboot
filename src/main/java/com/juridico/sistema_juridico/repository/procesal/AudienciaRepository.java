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
import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;

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
            "AND (:targetAbogadoId IS NULL OR (e.abogadoResponsable.id = :targetAbogadoId OR a.abogadoComparece.id = :targetAbogadoId)) "
            +
            "AND (:filtroUsuarioId IS NULL OR (e.abogadoResponsable.id = :filtroUsuarioId OR a.abogadoComparece.id = :filtroUsuarioId)) "
            +
            "AND (:filtroGerenciaId IS NULL OR e.gerencia.id = :filtroGerenciaId) " +
            "AND ((:filtroMateriaIds) IS NULL OR e.materia.id IN (:filtroMateriaIds)) " +
            "AND (:id IS NULL OR a.id = :id)")
    Page<Audiencia> buscarConFiltros(
            @Param("id") Integer id,
            @Param("keyword") String keyword,
            @Param("tipo") String tipo,
            @Param("gerencia") String gerencia,
            @Param("materia") String materia,
            @Param("estatus") EstatusAudiencia estatus,
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
            "AND (:targetAbogadoId IS NULL OR (e.abogadoResponsable.id = :targetAbogadoId OR a.abogadoComparece.id = :targetAbogadoId)) "
            +
            "AND (:filtroUsuarioId IS NULL OR (e.abogadoResponsable.id = :filtroUsuarioId OR a.abogadoComparece.id = :filtroUsuarioId)) "
            +
            "AND (:filtroGerenciaId IS NULL OR e.gerencia.id = :filtroGerenciaId) " +
            "AND ((:filtroMateriaIds) IS NULL OR e.materia.id IN (:filtroMateriaIds)) " +
            "AND (:id IS NULL OR a.id = :id)")
    List<Audiencia> listarParaExcel(
            @Param("id") Integer id,
            @Param("keyword") String keyword,
            @Param("tipo") String tipo,
            @Param("gerencia") String gerencia,
            @Param("materia") String materia,
            @Param("estatus") EstatusAudiencia estatus,
            @Param("targetAbogadoId") Integer targetAbogadoId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("filtroUsuarioId") Integer filtroUsuarioId,
            @Param("filtroGerenciaId") Integer filtroGerenciaId,
            @Param("filtroMateriaIds") List<Integer> filtroMateriaIds);

        List<Audiencia> findByFechaAudienciaAndEstatusAudienciaNot(LocalDate fecha, EstatusAudiencia estatusExcluido);

        List<Audiencia> findByFechaAudienciaAndHoraAudienciaBetweenAndEstatusAudienciaNot(
                        LocalDate fecha,
                        LocalTime horaInicio,
                        LocalTime horaFin,
                        EstatusAudiencia estatusExcluido);

        @Query("SELECT a FROM Audiencia a " +
                        "WHERE a.expediente.id = :expedienteId " +
                        "AND a.fechaAudiencia >= :fechaActual " +
                        "AND (a.estatusAudiencia IS NULL OR a.estatusAudiencia NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.CONCLUIDA, com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.CON_ACTA)) "
                        +
                        "ORDER BY a.fechaAudiencia ASC, a.horaAudiencia ASC LIMIT 1")
        Optional<Audiencia> findProximaAudiencia(
                        @Param("expedienteId") UUID expedienteId,
                        @Param("fechaActual") LocalDate fechaActual);

        List<Audiencia> findByExpedienteIdAndFechaAudiencia(UUID expedienteId, LocalDate fecha);

        // --- 4. CARGA DE TRABAJO POR USUARIO ---
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

        // --- 5. CONTEO PARA DASHBOARD KPIs ---
        // Conteo por abogado individual (solo audiencias pendientes)
        @Query("SELECT COUNT(a) FROM Audiencia a JOIN a.expediente e WHERE e.abogadoResponsable.id = :abogadoId AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
        long countPendientesByAbogadoId(@Param("abogadoId") Integer abogadoId);

        @Query("SELECT COUNT(a) FROM Audiencia a JOIN a.expediente e WHERE e.abogadoResponsable.id = :abogadoId AND e.materia.id = :materiaId AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
        long countPendientesByAbogadoIdAndMateriaId(@Param("abogadoId") Integer abogadoId, @Param("materiaId") Integer materiaId);

        // Fetch audiencias pendientes como lista (para calcular semáforo en el service)
        @Query("SELECT a FROM Audiencia a JOIN a.expediente e WHERE e.abogadoResponsable.id = :abogadoId AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
        List<com.juridico.sistema_juridico.Entity.procesal.Audiencia> fetchPendientesByAbogadoId(@Param("abogadoId") Integer abogadoId);

        @Query("SELECT a FROM Audiencia a JOIN a.expediente e WHERE e.abogadoResponsable.id = :abogadoId AND e.materia.id = :materiaId AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
        List<com.juridico.sistema_juridico.Entity.procesal.Audiencia> fetchPendientesByAbogadoIdAndMateriaId(@Param("abogadoId") Integer abogadoId, @Param("materiaId") Integer materiaId);

        // Conteo por gerencia individual (solo audiencias pendientes)
        @Query("SELECT COUNT(a) FROM Audiencia a JOIN a.expediente e WHERE e.gerencia.id = :gerenciaId AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
        long countByExpedienteGerenciaIdAndEstatusPendiente(@Param("gerenciaId") Long gerenciaId);

        // Conteo por lista de gerencias (solo audiencias pendientes)
        @Query("SELECT COUNT(a) FROM Audiencia a JOIN a.expediente e WHERE e.gerencia.id IN :gerenciaIds AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
        long countByExpedienteGerenciaIdInAndEstatusPendiente(@Param("gerenciaIds") List<Long> gerenciaIds);

        // Conteo total audiencias pendientes (sin filtro)
        @Query("SELECT COUNT(a) FROM Audiencia a WHERE a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
        long countByEstatusPendiente();

        // --- 6. AGENDA GENERAL ---
        Page<Audiencia> findByEstatusAudienciaAndFechaDesahogoBetweenOrderByFechaDesahogoDesc(
                        EstatusAudiencia estatus,
                        LocalDate inicio,
                        LocalDate fin,
                        Pageable pageable);

        @Query("SELECT a FROM Audiencia a WHERE a.estatusAudiencia = :estatus AND a.fechaDesahogo BETWEEN :inicio AND :fin AND a.expediente.gerencia.id = :gerenciaId ORDER BY a.fechaDesahogo DESC")
        Page<Audiencia> findByEstatusGerencia(
                        @Param("estatus") EstatusAudiencia estatus,
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin,
                        @Param("gerenciaId") Long gerenciaId,
                        Pageable pageable);

        @Query("SELECT a FROM Audiencia a WHERE a.estatusAudiencia = :estatus AND a.fechaDesahogo BETWEEN :inicio AND :fin AND a.expediente.materia.id IN :materiaIds ORDER BY a.fechaDesahogo DESC")
        Page<Audiencia> findByEstatusMaterias(
                        @Param("estatus") EstatusAudiencia estatus,
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin,
                        @Param("materiaIds") List<Integer> materiaIds,
                        Pageable pageable);

        @Query("SELECT a.estatusAudiencia, COUNT(a) FROM Audiencia a " +
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
                        "AND (a.fechaAudiencia >= :fechaInicio) " +
                        "AND (a.fechaAudiencia <= :fechaFin) " +
                        "AND (:targetAbogadoId IS NULL OR (e.abogadoResponsable.id = :targetAbogadoId OR a.abogadoComparece.id = :targetAbogadoId)) "
                        +
                        "AND (:filtroUsuarioId IS NULL OR (e.abogadoResponsable.id = :filtroUsuarioId OR a.abogadoComparece.id = :filtroUsuarioId)) "
                        +
                        "AND (:filtroGerenciaId IS NULL OR e.gerencia.id = :filtroGerenciaId) " +
                        "AND ((:filtroMateriaIds) IS NULL OR e.materia.id IN (:filtroMateriaIds)) " +
                        "GROUP BY a.estatusAudiencia")
        List<Object[]> contarPorEstatusConFiltros(
                        @Param("keyword") String keyword,
                        @Param("tipo") String tipo,
                        @Param("gerencia") String gerencia,
                        @Param("materia") String materia,
                        @Param("targetAbogadoId") Integer targetAbogadoId,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin,
                        @Param("filtroUsuarioId") Integer filtroUsuarioId,
                        @Param("filtroGerenciaId") Integer filtroGerenciaId,
                        @Param("filtroMateriaIds") List<Integer> filtroMateriaIds);
}
