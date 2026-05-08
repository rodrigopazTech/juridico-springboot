package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
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

    // 1. CONSULTA PARA EXCEL
    @Query("SELECT t FROM Termino t " +
           "LEFT JOIN t.expediente e " +
           "LEFT JOIN t.abogadoResponsable a " +
           "WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:estatus IS NULL OR :estatus = '' OR t.estatusTermino = :estatus) " +
           "AND (:prioridadId IS NULL OR t.prioridad.id = :prioridadId) " +
           "AND (:abogadoId IS NULL OR a.id = :abogadoId) " +
           "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId) " +
           "AND (:materiaId IS NULL OR e.materia.id = :materiaId) " +
           "AND (:id IS NULL OR t.id = :id) " +
           "AND (t.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin)")
    List<Termino> listarParaExcel(
            @Param("id") Integer id,
            @Param("keyword") String keyword,
            @Param("estatus") EstatusTermino estatus,
            @Param("prioridadId") Integer prioridadId,
            @Param("abogadoId") Integer abogadoId,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("materiaId") Integer materiaId);

    // 2. CONSULTA PARA PAGINACIÓN
    @Query("SELECT t FROM Termino t " +
           "LEFT JOIN t.expediente e " +
           "LEFT JOIN t.abogadoResponsable a " +
           "WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:estatus IS NULL OR :estatus = '' OR t.estatusTermino = :estatus) " +
           "AND (:prioridadId IS NULL OR t.prioridad.id = :prioridadId) " +
           "AND (:abogadoId IS NULL OR a.id = :abogadoId) " +
           "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId) " +
           "AND (:materiaId IS NULL OR e.materia.id = :materiaId) " +
           "AND (:id IS NULL OR t.id = :id) " +
           "AND ((:filtroMateriaIds) IS NULL OR e.materia.id IN (:filtroMateriaIds)) " +
           "AND (t.fechaVencimiento >= :fechaInicio AND t.fechaVencimiento <= :fechaFin)")
    Page<Termino> buscarConFiltros(
            @Param("id") Integer id,
            @Param("keyword") String keyword,
            @Param("estatus") EstatusTermino estatus,
            @Param("prioridadId") Integer prioridadId,
            @Param("abogadoId") Integer abogadoId,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("materiaId") Integer materiaId,
            @Param("filtroMateriaIds") List<Integer> filtroMateriaIds,
            Pageable pageable);

    // ... Métodos auxiliares (sin cambios) ...
    List<Termino> findByFechaVencimientoBeforeAndEstatusTerminoNot(LocalDate fecha, EstatusTermino estatus);

    List<Termino> findByExpedienteId(UUID expedienteId);

    // Buscar términos que vencen en una fecha específica trayendo (FETCH) el
    // Expediente y Abogado de inmediato
    @Query("SELECT t FROM Termino t " +
            "JOIN FETCH t.expediente " +
            "LEFT JOIN FETCH t.abogadoResponsable " +
            "WHERE t.fechaVencimiento = :fecha " +
            "AND t.estatusTermino NOT IN :estatusExcluidos")
    List<Termino> findByFechaVencimientoAndEstatusTerminoNotIn(
            @Param("fecha") LocalDate fecha,
            @Param("estatusExcluidos") List<EstatusTermino> estatusExcluidos);

    Page<Termino> findByEstatusTerminoInAndFechaPresentacionBetweenOrderByFechaPresentacionDesc(
            List<EstatusTermino> estatus,
            LocalDate inicio,
            LocalDate fin,
            Pageable pageable);

    @Query("""
                SELECT u.nombreCompleto, COUNT(t)
                FROM Termino t
                JOIN t.abogadoResponsable u
                GROUP BY u.nombreCompleto
            """)
    List<Object[]> contarTerminosPorUsuario();

    @Query("""
                SELECT u.nombreCompleto, COUNT(t)
                FROM Termino t
                JOIN t.expediente e
                JOIN t.abogadoResponsable u
                WHERE e.gerencia.id = :gerenciaId
                GROUP BY u.nombreCompleto
            """)
    List<Object[]> contarTerminosPorUsuarioYGerencia(@Param("gerenciaId") Long gerenciaId);

    @Query("""
                SELECT u.nombreCompleto, COUNT(t)
                FROM Termino t
                JOIN t.expediente e
                JOIN t.abogadoResponsable u
                WHERE e.gerencia.id IN :gerenciaIds
                GROUP BY u.nombreCompleto
            """)
    List<Object[]> contarTerminosPorUsuarioYGerencias(@Param("gerenciaIds") List<Long> gerenciaIds);

    // --- 4. CONTEO PARA DASHBOARD KPIs ---
    // Conteo por abogado individual (solo términos activos)
    @Query("SELECT COUNT(t) FROM Termino t JOIN t.expediente e WHERE (e.abogadoResponsable.id = :abogadoId OR t.abogadoResponsable.id = :abogadoId) AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countActivosByAbogadoId(@Param("abogadoId") Integer abogadoId);

    @Query("SELECT COUNT(t) FROM Termino t JOIN t.expediente e WHERE (e.abogadoResponsable.id = :abogadoId OR t.abogadoResponsable.id = :abogadoId) AND e.materia.id = :materiaId AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countActivosByAbogadoIdAndMateriaId(@Param("abogadoId") Integer abogadoId, @Param("materiaId") Integer materiaId);

    @Query("SELECT t.prioridad.nombre, COUNT(t) FROM Termino t JOIN t.expediente e WHERE (e.abogadoResponsable.id = :abogadoId OR t.abogadoResponsable.id = :abogadoId) AND t.prioridad IS NOT NULL AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO) GROUP BY t.prioridad.nombre")
    List<Object[]> contarTerminosPorPrioridadYAbogado(@Param("abogadoId") Integer abogadoId);

    @Query("SELECT t.prioridad.nombre, COUNT(t) FROM Termino t JOIN t.expediente e WHERE (e.abogadoResponsable.id = :abogadoId OR t.abogadoResponsable.id = :abogadoId) AND e.materia.id = :materiaId AND t.prioridad IS NOT NULL AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO) GROUP BY t.prioridad.nombre")
    List<Object[]> contarTerminosPorPrioridadYAbogadoYMateria(@Param("abogadoId") Integer abogadoId, @Param("materiaId") Integer materiaId);

    // Conteo por gerencia individual (solo términos activos - excluidos concluidos/presentados)
    @Query("SELECT COUNT(t) FROM Termino t JOIN t.expediente e WHERE e.gerencia.id = :gerenciaId AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countByExpedienteGerenciaIdAndActivo(@Param("gerenciaId") Long gerenciaId);

    @Query("SELECT t FROM Termino t " +
           "LEFT JOIN t.expediente e " +
           "WHERE t.estatusTermino IN :estatusList " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CONCAT(t.id, '') LIKE CONCAT('%', :keyword, '%')) " +
           "AND t.fechaVencimiento >= :fechaInicio AND t.fechaVencimiento <= :fechaFin " +
           "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId) " +
           "AND (:materiaId IS NULL OR e.materia.id = :materiaId) " +
           "AND (:filtroRoleGerenciaId IS NULL OR e.gerencia.id = :filtroRoleGerenciaId) " +
           "AND ((:filtroRoleMateriaIds) IS NULL OR e.materia.id IN (:filtroRoleMateriaIds))")
    Page<Termino> buscarParaHistorial(
            @Param("keyword") String keyword,
            @Param("estatusList") List<EstatusTermino> estatusList,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("materiaId") Integer materiaId,
            @Param("filtroRoleGerenciaId") Integer filtroRoleGerenciaId,
            @Param("filtroRoleMateriaIds") List<Integer> filtroRoleMateriaIds,
            Pageable pageable);

    // Conteo por lista de gerencias (solo términos activos)
    @Query("SELECT COUNT(t) FROM Termino t JOIN t.expediente e WHERE e.gerencia.id IN :gerenciaIds AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countByExpedienteGerenciaIdInAndActivo(@Param("gerenciaIds") List<Long> gerenciaIds);

    @Query("SELECT t.estatusTermino, COUNT(t) FROM Termino t " +
            "LEFT JOIN t.expediente e " +
            "LEFT JOIN t.abogadoResponsable a " +
            "WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CONCAT(t.id, '') LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:prioridadId IS NULL OR t.prioridad.id = :prioridadId) " +
            "AND (:abogadoId IS NULL OR a.id = :abogadoId) " +
            "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId) " +
            "AND (:materiaId IS NULL OR e.materia.id = :materiaId) " +
            "AND (t.fechaVencimiento >= :fechaInicio AND t.fechaVencimiento <= :fechaFin) " +
            "GROUP BY t.estatusTermino")
    List<Object[]> contarPorEtapaConFiltros(
            @Param("keyword") String keyword,
            @Param("prioridadId") Integer prioridadId,
            @Param("abogadoId") Integer abogadoId,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("materiaId") Integer materiaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    @Query("SELECT t FROM Termino t " +
            "LEFT JOIN t.expediente e " +
            "LEFT JOIN t.abogadoResponsable a " +
            "WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CONCAT(t.id, '') LIKE CONCAT('%', :keyword, '%')) " +
            "AND (t.estatusTermino IN :estatusList) " +
            "AND (:prioridadId IS NULL OR t.prioridad.id = :prioridadId) " +
            "AND (:abogadoId IS NULL OR a.id = :abogadoId) " +
            "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId) " +
            "AND (:materiaId IS NULL OR e.materia.id = :materiaId) " +
            "AND (:id IS NULL OR t.id = :id) " +
            "AND (t.fechaVencimiento >= :fechaInicio AND t.fechaVencimiento <= :fechaFin)")
    Page<Termino> buscarPorMultiplesEstatus(
            @Param("id") Integer id,
            @Param("keyword") String keyword,
            @Param("estatusList") List<EstatusTermino> estatusList,
            @Param("prioridadId") Integer prioridadId,
            @Param("abogadoId") Integer abogadoId,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("materiaId") Integer materiaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            Pageable pageable);

    // Conteo total términos activos (sin filtro)
    @Query("SELECT COUNT(t) FROM Termino t WHERE t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countByActivo();

    // --- AGENDA GENERAL ---
    @Query("SELECT t FROM Termino t WHERE t.estatusTermino IN :estatus AND t.fechaPresentacion BETWEEN :inicio AND :fin AND t.expediente.gerencia.id = :gerenciaId ORDER BY t.fechaPresentacion DESC")
    Page<Termino> findByEstatusGerencia(
            @Param("estatus") List<EstatusTermino> estatus,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("gerenciaId") Long gerenciaId,
            Pageable pageable);

    @Query("SELECT t FROM Termino t WHERE t.estatusTermino IN :estatus AND t.fechaPresentacion BETWEEN :inicio AND :fin AND t.expediente.materia.id IN :materiaIds ORDER BY t.fechaPresentacion DESC")
    Page<Termino> findByEstatusMaterias(
            @Param("estatus") List<EstatusTermino> estatus,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("materiaIds") List<Integer> materiaIds,
            Pageable pageable);

    @Query("SELECT t FROM Termino t WHERE t.abogadoResponsable.id = :usuarioId OR t.expediente.abogadoResponsable.id = :usuarioId")
    List<Termino> findByUsuarioId(@Param("usuarioId") Integer usuarioId);

    // --- MÉTODOS PARA JEFE DE DEPTO (POR MATERIAS) ---
    @Query("SELECT COUNT(t) FROM Termino t JOIN t.expediente e WHERE e.gerencia.id = :gerenciaId AND e.materia.id IN :materiaIds AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countByExpedienteGerenciaIdAndMateriaIdInAndActivo(@Param("gerenciaId") Long gerenciaId, @Param("materiaIds") List<Integer> materiaIds);

    @Query("""
                SELECT u.nombreCompleto, COUNT(t)
                FROM Termino t
                JOIN t.expediente e
                JOIN t.abogadoResponsable u
                WHERE e.gerencia.id = :gerenciaId AND e.materia.id IN :materiaIds
                GROUP BY u.nombreCompleto
            """)
    List<Object[]> contarTerminosPorUsuarioYGerenciaYMateriaIn(@Param("gerenciaId") Long gerenciaId, @Param("materiaIds") List<Integer> materiaIds);

    List<Termino> findByExpedienteAndAsuntoAndFechaVencimiento(com.juridico.sistema_juridico.Entity.expediente.Expediente expediente, String asunto, LocalDate fechaVencimiento);
}
