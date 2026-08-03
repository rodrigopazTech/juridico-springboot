package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TerminoRepository extends JpaRepository<Termino, Integer> {

    // 1. CONSULTA PARA EXCEL
    @Query("SELECT t FROM Termino t " +
           "LEFT JOIN t.expediente e " +
           "LEFT JOIN t.contexto c " +
           "LEFT JOIN t.abogadoResponsable a " +
           "LEFT JOIN e.abogadoResponsable er " +
           "WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.numeroExpedienteSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(er.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:estatus IS NULL OR t.estatusTermino = :estatus) " +
           "AND (:prioridadId IS NULL OR t.prioridad.id = :prioridadId) " +
           "AND (:abogadoId IS NULL OR a.id = :abogadoId) " +
           "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) " +
           "AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId) " +
           "AND (:id IS NULL OR t.id = :id) " +
           "AND (t.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin) " +
           "AND (:secGerenciaId IS NULL OR e.gerencia.id = :secGerenciaId OR c.gerencia.id = :secGerenciaId) " +
           "AND (" +
           "COALESCE(:secMateriaIds, NULL) IS NULL " +
           "OR e.materia.id IN :secMateriaIds " +
           "OR c.materia.id IN :secMateriaIds " +
           "OR a.id = :secUsuarioId " +
           "OR e.abogadoResponsable.id = :secUsuarioId" +
           ")")
    List<Termino> listarParaExcel(
            @Param("id") Integer id,
            @Param("keyword") String keyword,
            @Param("estatus") EstatusTermino estatus,
            @Param("prioridadId") Integer prioridadId,
            @Param("abogadoId") Integer abogadoId,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("materiaId") Integer materiaId,
            @Param("secGerenciaId") Integer secGerenciaId,
            @Param("secMateriaIds") List<Integer> secMateriaIds,
            @Param("secUsuarioId") Integer secUsuarioId);

    // 2. CONSULTA PARA PAGINACIÓN
    @Query("SELECT t FROM Termino t " +
           "LEFT JOIN t.expediente e " +
           "LEFT JOIN t.contexto c " +
           "LEFT JOIN t.abogadoResponsable a " +
           "LEFT JOIN e.abogadoResponsable er " +
           "WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.numeroExpedienteSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(er.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:estatus IS NULL OR t.estatusTermino = :estatus) " +
           "AND (:prioridadId IS NULL OR t.prioridad.id = :prioridadId) " +
           "AND (:abogadoId IS NULL OR a.id = :abogadoId) " +
           "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) " +
           "AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId) " +
           "AND (:id IS NULL OR t.id = :id) " +
           "AND ((:filtroMateriaIds) IS NULL OR e.materia.id IN (:filtroMateriaIds) OR c.materia.id IN (:filtroMateriaIds)) " +
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

    // ... Métodos auxiliares ...
    List<Termino> findByFechaVencimientoBeforeAndEstatusTerminoNot(LocalDate fecha, EstatusTermino estatus);

    List<Termino> findByExpedienteId(UUID expedienteId);

    @Query("SELECT t FROM Termino t " +
            "LEFT JOIN FETCH t.expediente " +
            "LEFT JOIN FETCH t.contexto " +
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
                LEFT JOIN t.expediente e
                LEFT JOIN t.contexto c
                JOIN t.abogadoResponsable u
                WHERE e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId
                GROUP BY u.nombreCompleto
            """)
    List<Object[]> contarTerminosPorUsuarioYGerencia(@Param("gerenciaId") Integer gerenciaId);

    @Query("""
                SELECT u.nombreCompleto, COUNT(t)
                FROM Termino t
                LEFT JOIN t.expediente e
                LEFT JOIN t.contexto c
                JOIN t.abogadoResponsable u
                WHERE e.gerencia.id IN :gerenciaIds OR c.gerencia.id IN :gerenciaIds
                GROUP BY u.nombreCompleto
            """)
    List<Object[]> contarTerminosPorUsuarioYGerencias(@Param("gerenciaIds") List<Integer> gerenciaIds);

    @Query("SELECT COUNT(t) FROM Termino t LEFT JOIN t.expediente e WHERE (e.abogadoResponsable.id = :abogadoId OR t.abogadoResponsable.id = :abogadoId) AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countActivosByAbogadoId(@Param("abogadoId") Integer abogadoId);

    @Query("SELECT COUNT(t) FROM Termino t LEFT JOIN t.expediente e LEFT JOIN t.contexto c WHERE (e.abogadoResponsable.id = :abogadoId OR t.abogadoResponsable.id = :abogadoId) AND (e.materia.id = :materiaId OR c.materia.id = :materiaId) AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countActivosByAbogadoIdAndMateriaId(@Param("abogadoId") Integer abogadoId, @Param("materiaId") Integer materiaId);

    @Query("SELECT t.prioridad.nombre, COUNT(t) FROM Termino t LEFT JOIN t.expediente e WHERE (e.abogadoResponsable.id = :abogadoId OR t.abogadoResponsable.id = :abogadoId) AND t.prioridad IS NOT NULL AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO) GROUP BY t.prioridad.nombre")
    List<Object[]> contarTerminosPorPrioridadYAbogado(@Param("abogadoId") Integer abogadoId);

    @Query("SELECT t.prioridad.nombre, COUNT(t) FROM Termino t LEFT JOIN t.expediente e LEFT JOIN t.contexto c WHERE (e.abogadoResponsable.id = :abogadoId OR t.abogadoResponsable.id = :abogadoId) AND (e.materia.id = :materiaId OR c.materia.id = :materiaId) AND t.prioridad IS NOT NULL AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO) GROUP BY t.prioridad.nombre")
    List<Object[]> contarTerminosPorPrioridadYAbogadoYMateria(@Param("abogadoId") Integer abogadoId, @Param("materiaId") Integer materiaId);

    @Query("SELECT COUNT(t) FROM Termino t LEFT JOIN t.expediente e LEFT JOIN t.contexto c WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countByExpedienteGerenciaIdAndActivo(@Param("gerenciaId") Long gerenciaId);

    @Query("SELECT t FROM Termino t " +
           "LEFT JOIN t.expediente e " +
           "LEFT JOIN t.contexto c " +
           "LEFT JOIN t.abogadoResponsable a " +
           "WHERE t.estatusTermino = :estatus " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.numeroExpedienteSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CONCAT(t.id, '') LIKE CONCAT('%', :keyword, '%')) " +
           "AND t.updatedAt >= :fechaInicio AND t.updatedAt <= :fechaFin " +
           "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) " +
           "AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId) " +
           "AND (:secGerenciaId IS NULL OR e.gerencia.id = :secGerenciaId OR c.gerencia.id = :secGerenciaId) " +
           "AND (" +
           "COALESCE(:secMateriaIds, NULL) IS NULL " +
           "OR e.materia.id IN :secMateriaIds " +
           "OR c.materia.id IN :secMateriaIds " +
           "OR a.id = :secUsuarioId " +
           "OR e.abogadoResponsable.id = :secUsuarioId" +
           ") " +
           "ORDER BY t.updatedAt DESC, t.id DESC")
    Page<Termino> buscarParaHistorial(
            @Param("keyword") String keyword,
            @Param("estatus") EstatusTermino estatus,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("materiaId") Integer materiaId,
            @Param("secGerenciaId") Integer secGerenciaId,
            @Param("secMateriaIds") List<Integer> secMateriaIds,
            @Param("secUsuarioId") Integer secUsuarioId,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM Termino t LEFT JOIN t.expediente e LEFT JOIN t.contexto c WHERE (e.gerencia.id IN :gerenciaIds OR c.gerencia.id IN :gerenciaIds) AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countByExpedienteGerenciaIdInAndActivo(@Param("gerenciaIds") List<Long> gerenciaIds);

    @Query("SELECT t.estatusTermino, COUNT(t) FROM Termino t " +
            "LEFT JOIN t.expediente e " +
            "LEFT JOIN t.contexto c " +
            "LEFT JOIN t.abogadoResponsable a " +
            "WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.numeroExpedienteSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CONCAT(t.id, '') LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:prioridadId IS NULL OR t.prioridad.id = :prioridadId) " +
            "AND (:abogadoId IS NULL OR a.id = :abogadoId) " +
            "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) " +
            "AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId) " +
            "AND ((:filtroMateriaIds) IS NULL OR e.materia.id IN (:filtroMateriaIds) OR c.materia.id IN (:filtroMateriaIds)) " +
            "AND (t.fechaVencimiento >= :fechaInicio AND t.fechaVencimiento <= :fechaFin) " +
            "GROUP BY t.estatusTermino")
    List<Object[]> contarPorEtapaConFiltros(
            @Param("keyword") String keyword,
            @Param("prioridadId") Integer prioridadId,
            @Param("abogadoId") Integer abogadoId,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("materiaId") Integer materiaId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("filtroMateriaIds") List<Integer> filtroMateriaIds);

    @Query("SELECT t FROM Termino t " +
            "LEFT JOIN t.expediente e " +
            "LEFT JOIN t.contexto c " +
            "LEFT JOIN t.abogadoResponsable a " +
            "WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(t.asunto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.numeroExpedienteSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CONCAT(t.id, '') LIKE CONCAT('%', :keyword, '%')) " +
            "AND (t.estatusTermino IN :estatusList) " +
            "AND (:prioridadId IS NULL OR t.prioridad.id = :prioridadId) " +
            "AND (:abogadoId IS NULL OR a.id = :abogadoId) " +
            "AND (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) " +
            "AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId) " +
            "AND (:id IS NULL OR t.id = :id) " +
            "AND (t.fechaVencimiento >= :fechaInicio AND t.fechaVencimiento <= :fechaFin) " +
            "AND (:secGerenciaId IS NULL OR e.gerencia.id = :secGerenciaId OR c.gerencia.id = :secGerenciaId) " +
            "AND (" +
            "COALESCE(:secMateriaIds, NULL) IS NULL " +
            "OR e.materia.id IN :secMateriaIds " +
            "OR c.materia.id IN :secMateriaIds " +
            "OR a.id = :secUsuarioId " +
            "OR e.abogadoResponsable.id = :secUsuarioId" +
            ")")
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
            @Param("secGerenciaId") Integer secGerenciaId,
            @Param("secMateriaIds") List<Integer> secMateriaIds,
            @Param("secUsuarioId") Integer secUsuarioId,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM Termino t WHERE t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countByActivo();

    @Query("SELECT t FROM Termino t LEFT JOIN t.expediente e LEFT JOIN t.contexto c WHERE t.estatusTermino IN :estatus AND t.fechaPresentacion BETWEEN :inicio AND :fin AND (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) ORDER BY t.fechaPresentacion DESC")
    Page<Termino> findByEstatusGerencia(
            @Param("estatus") List<EstatusTermino> estatus,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("gerenciaId") Long gerenciaId,
            Pageable pageable);

    @Query("SELECT t FROM Termino t LEFT JOIN t.expediente e LEFT JOIN t.contexto c WHERE t.estatusTermino IN :estatus AND t.fechaPresentacion BETWEEN :inicio AND :fin AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds) ORDER BY t.fechaPresentacion DESC")
    Page<Termino> findByEstatusMaterias(
            @Param("estatus") List<EstatusTermino> estatus,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("materiaIds") List<Integer> materiaIds,
            Pageable pageable);

    @Query("SELECT t FROM Termino t LEFT JOIN t.expediente e WHERE t.abogadoResponsable.id = :usuarioId OR e.abogadoResponsable.id = :usuarioId")
    List<Termino> findByUsuarioId(@Param("usuarioId") Integer usuarioId);

    @Query("""
        SELECT COUNT(t)
        FROM Termino t
        LEFT JOIN t.expediente e
        LEFT JOIN t.contexto c
        WHERE (e.abogadoResponsable.id = :usuarioId OR t.abogadoResponsable.id = :usuarioId)
          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
          AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
          )
          AND t.fechaVencimiento < :hoy
    """)
    long countMisTerminosVencidos(@Param("usuarioId") Integer usuarioId,
                                  @Param("materiaId") Integer materiaId,
                                  @Param("hoy") LocalDate hoy);

    @Query("""
        SELECT COUNT(t)
        FROM Termino t
        LEFT JOIN t.expediente e
        LEFT JOIN t.contexto c
        WHERE (e.abogadoResponsable.id = :usuarioId OR t.abogadoResponsable.id = :usuarioId)
          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
          AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
          )
          AND t.fechaVencimiento = :hoy
    """)
    long countMisTerminosHoy(@Param("usuarioId") Integer usuarioId,
                             @Param("materiaId") Integer materiaId,
                             @Param("hoy") LocalDate hoy);

    @Query("""
        SELECT COUNT(t)
        FROM Termino t
        LEFT JOIN t.expediente e
        LEFT JOIN t.contexto c
        WHERE (e.abogadoResponsable.id = :usuarioId OR t.abogadoResponsable.id = :usuarioId)
          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
          AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
          )
          AND t.fechaVencimiento BETWEEN :hoy AND :hasta
    """)
    long countMisTerminosProximos(@Param("usuarioId") Integer usuarioId,
                                  @Param("materiaId") Integer materiaId,
                                  @Param("hoy") LocalDate hoy,
                                  @Param("hasta") LocalDate hasta);

    @Query("""
        SELECT t
        FROM Termino t
        LEFT JOIN FETCH t.expediente e
        LEFT JOIN FETCH t.contexto c
        LEFT JOIN FETCH t.prioridad p
        LEFT JOIN FETCH t.abogadoResponsable ar
        WHERE (e.abogadoResponsable.id = :usuarioId OR t.abogadoResponsable.id = :usuarioId)
          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
          AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
          )
          AND t.fechaVencimiento <= :hasta
        ORDER BY t.fechaVencimiento ASC
    """)
    List<Termino> findMisTerminosCriticos(@Param("usuarioId") Integer usuarioId,
                                          @Param("materiaId") Integer materiaId,
                                          @Param("hasta") LocalDate hasta,
                                          Pageable pageable);

    // --- MÉTODOS PARA JEFE DE DEPTO (POR MATERIAS) ---
    @Query("SELECT COUNT(t) FROM Termino t LEFT JOIN t.expediente e LEFT JOIN t.contexto c WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds) AND t.estatusTermino NOT IN (com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO, com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO)")
    long countByExpedienteGerenciaIdAndMateriaIdInAndActivo(@Param("gerenciaId") Long gerenciaId, @Param("materiaIds") List<Integer> materiaIds);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento < :hoy
    """)
    long countDepartamentoTerminosVencidos(@Param("gerenciaId") Long gerenciaId,
                                           @Param("materiaIds") List<Integer> materiaIds,
                                           @Param("hoy") LocalDate hoy);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento = :hoy
    """)
    long countDepartamentoTerminosHoy(@Param("gerenciaId") Long gerenciaId,
                                      @Param("materiaIds") List<Integer> materiaIds,
                                      @Param("hoy") LocalDate hoy);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :hoy AND :hasta
    """)
    long countDepartamentoTerminosProximos(@Param("gerenciaId") Long gerenciaId,
                                           @Param("materiaIds") List<Integer> materiaIds,
                                           @Param("hoy") LocalDate hoy,
                                           @Param("hasta") LocalDate hasta);

    @Query("""
          SELECT t
          FROM Termino t
          LEFT JOIN FETCH t.expediente e
          LEFT JOIN FETCH t.contexto c
          LEFT JOIN FETCH t.abogadoResponsable ar
          LEFT JOIN FETCH t.prioridad p
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento <= :hasta
          ORDER BY t.fechaVencimiento ASC
    """)
    List<Termino> findDepartamentoTerminosCriticos(@Param("gerenciaId") Long gerenciaId,
                                                   @Param("materiaIds") List<Integer> materiaIds,
                                                   @Param("hasta") LocalDate hasta,
                                                   Pageable pageable);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento < :hoy
    """)
    long countGerenciaTerminosVencidos(@Param("gerenciaId") Long gerenciaId,
                                       @Param("hoy") LocalDate hoy);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento = :hoy
    """)
    long countGerenciaTerminosHoy(@Param("gerenciaId") Long gerenciaId,
                                  @Param("hoy") LocalDate hoy);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :hoy AND :hasta
    """)
    long countGerenciaTerminosProximos(@Param("gerenciaId") Long gerenciaId,
                                       @Param("hoy") LocalDate hoy,
                                       @Param("hasta") LocalDate hasta);

    @Query("""
          SELECT t
          FROM Termino t
          LEFT JOIN FETCH t.expediente e
          LEFT JOIN FETCH t.contexto c
          LEFT JOIN FETCH t.abogadoResponsable ar
          LEFT JOIN FETCH t.prioridad p
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento <= :hasta
          ORDER BY t.fechaVencimiento ASC
    """)
    List<Termino> findGerenciaTerminosCriticos(@Param("gerenciaId") Long gerenciaId,
                                               @Param("hasta") LocalDate hasta,
                                               Pageable pageable);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento < :hoy
    """)
    long countGerenciaTerminosVencidosFiltrado(@Param("gerenciaId") Long gerenciaId,
                                               @Param("materiaId") Integer materiaId,
                                               @Param("abogadoId") Integer abogadoId,
                                               @Param("hoy") LocalDate hoy);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento = :hoy
    """)
    long countGerenciaTerminosHoyFiltrado(@Param("gerenciaId") Long gerenciaId,
                                          @Param("materiaId") Integer materiaId,
                                          @Param("abogadoId") Integer abogadoId,
                                          @Param("hoy") LocalDate hoy);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :hoy AND :hasta
    """)
    long countGerenciaTerminosProximosFiltrado(@Param("gerenciaId") Long gerenciaId,
                                               @Param("materiaId") Integer materiaId,
                                               @Param("abogadoId") Integer abogadoId,
                                               @Param("hoy") LocalDate hoy,
                                               @Param("hasta") LocalDate hasta);

    @Query("""
          SELECT t
          FROM Termino t
          LEFT JOIN FETCH t.expediente e
          LEFT JOIN FETCH t.contexto c
          LEFT JOIN FETCH t.abogadoResponsable ar
          LEFT JOIN FETCH e.abogadoResponsable er
          LEFT JOIN FETCH t.prioridad p
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin
          ORDER BY t.fechaVencimiento ASC
    """)
    List<Termino> findGerenciaTerminosPeriodoFiltrado(@Param("gerenciaId") Long gerenciaId,
                                                      @Param("materiaId") Integer materiaId,
                                                      @Param("abogadoId") Integer abogadoId,
                                                      @Param("fechaInicio") LocalDate fechaInicio,
                                                      @Param("fechaFin") LocalDate fechaFin,
                                                      Pageable pageable);

    @Query("""
          SELECT COALESCE(ar.nombreCompleto, er.nombreCompleto, 'Sin responsable'), COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin
          GROUP BY COALESCE(ar.nombreCompleto, er.nombreCompleto, 'Sin responsable')
    """)
    List<Object[]> contarGerenciaTerminosPorUsuarioFiltrado(@Param("gerenciaId") Long gerenciaId,
                                                            @Param("materiaId") Integer materiaId,
                                                            @Param("abogadoId") Integer abogadoId,
                                                            @Param("fechaInicio") LocalDate fechaInicio,
                                                            @Param("fechaFin") LocalDate fechaFin);

    @Query("""
          SELECT COALESCE(e.materia.nombre, c.materia.nombre, 'Sin materia'), COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin
          GROUP BY COALESCE(e.materia.nombre, c.materia.nombre, 'Sin materia')
    """)
    List<Object[]> contarGerenciaTerminosPorMateriaFiltrado(@Param("gerenciaId") Long gerenciaId,
                                                            @Param("materiaId") Integer materiaId,
                                                            @Param("abogadoId") Integer abogadoId,
                                                            @Param("fechaInicio") LocalDate fechaInicio,
                                                            @Param("fechaFin") LocalDate fechaFin);

    @Query("""
                SELECT u.nombreCompleto, COUNT(t)
                FROM Termino t
                LEFT JOIN t.expediente e
                LEFT JOIN t.contexto c
                JOIN t.abogadoResponsable u
                WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                  AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds)
                GROUP BY u.nombreCompleto
            """)
    List<Object[]> contarTerminosPorUsuarioYGerenciaYMateriaIn(@Param("gerenciaId") Long gerenciaId, @Param("materiaIds") List<Integer> materiaIds);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento < :hoy
    """)
    long countDirectivoTerminosVencidosFiltrado(@Param("gerenciaId") Long gerenciaId,
                                                @Param("materiaId") Integer materiaId,
                                                @Param("abogadoId") Integer abogadoId,
                                                @Param("hoy") LocalDate hoy);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento = :hoy
    """)
    long countDirectivoTerminosHoyFiltrado(@Param("gerenciaId") Long gerenciaId,
                                           @Param("materiaId") Integer materiaId,
                                           @Param("abogadoId") Integer abogadoId,
                                           @Param("hoy") LocalDate hoy);

    @Query("""
          SELECT COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :hoy AND :hasta
    """)
    long countDirectivoTerminosProximosFiltrado(@Param("gerenciaId") Long gerenciaId,
                                                @Param("materiaId") Integer materiaId,
                                                @Param("abogadoId") Integer abogadoId,
                                                @Param("hoy") LocalDate hoy,
                                                @Param("hasta") LocalDate hasta);

    @Query("""
          SELECT t
          FROM Termino t
          LEFT JOIN FETCH t.expediente e
          LEFT JOIN FETCH t.contexto c
          LEFT JOIN FETCH t.abogadoResponsable ar
          LEFT JOIN FETCH e.abogadoResponsable er
          LEFT JOIN FETCH t.prioridad p
          WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin
          ORDER BY t.fechaVencimiento ASC
    """)
    List<Termino> findDirectivoTerminosPeriodoFiltrado(@Param("gerenciaId") Long gerenciaId,
                                                       @Param("materiaId") Integer materiaId,
                                                       @Param("abogadoId") Integer abogadoId,
                                                       @Param("fechaInicio") LocalDate fechaInicio,
                                                       @Param("fechaFin") LocalDate fechaFin,
                                                       Pageable pageable);

    @Query("""
          SELECT COALESCE(ar.nombreCompleto, er.nombreCompleto, 'Sin responsable'), COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin
          GROUP BY COALESCE(ar.nombreCompleto, er.nombreCompleto, 'Sin responsable')
    """)
    List<Object[]> contarDirectivoTerminosPorUsuarioFiltrado(@Param("gerenciaId") Long gerenciaId,
                                                             @Param("materiaId") Integer materiaId,
                                                             @Param("abogadoId") Integer abogadoId,
                                                             @Param("fechaInicio") LocalDate fechaInicio,
                                                             @Param("fechaFin") LocalDate fechaFin);

    @Query("""
          SELECT COALESCE(e.materia.nombre, c.materia.nombre, 'Sin materia'), COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin
          GROUP BY COALESCE(e.materia.nombre, c.materia.nombre, 'Sin materia')
    """)
    List<Object[]> contarDirectivoTerminosPorMateriaFiltrado(@Param("gerenciaId") Long gerenciaId,
                                                             @Param("materiaId") Integer materiaId,
                                                             @Param("abogadoId") Integer abogadoId,
                                                             @Param("fechaInicio") LocalDate fechaInicio,
                                                             @Param("fechaFin") LocalDate fechaFin);

    @Query("""
          SELECT COALESCE(e.gerencia.nombre, c.gerencia.nombre, 'Sin gerencia'), COUNT(t)
          FROM Termino t
          LEFT JOIN t.expediente e
          LEFT JOIN t.contexto c
          LEFT JOIN t.abogadoResponsable ar
          LEFT JOIN e.abogadoResponsable er
          WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
            AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
            AND (:abogadoId IS NULL OR ar.id = :abogadoId OR er.id = :abogadoId)
            AND t.estatusTermino NOT IN (
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO,
              com.juridico.sistema_juridico.Entity.enums.EstatusTermino.PRESENTADO
            )
            AND t.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin
          GROUP BY COALESCE(e.gerencia.nombre, c.gerencia.nombre, 'Sin gerencia')
    """)
    List<Object[]> contarDirectivoTerminosPorGerenciaFiltrado(@Param("gerenciaId") Long gerenciaId,
                                                              @Param("materiaId") Integer materiaId,
                                                              @Param("abogadoId") Integer abogadoId,
                                                              @Param("fechaInicio") LocalDate fechaInicio,
                                                              @Param("fechaFin") LocalDate fechaFin);

    List<Termino> findByExpedienteAndAsuntoAndFechaVencimiento(com.juridico.sistema_juridico.Entity.expediente.Expediente expediente, String asunto, LocalDate fechaVencimiento);

    @Modifying
    @Query("""
        UPDATE Termino t
        SET t.abogadoResponsable.id = :nuevoAbogadoId
        WHERE t.expediente.id = :expedienteId
          AND t.estatusTermino <> com.juridico.sistema_juridico.Entity.enums.EstatusTermino.CONCLUIDO
    """)
    int reasignarResponsableEnTerminosActivos(@Param("expedienteId") UUID expedienteId,
                                              @Param("nuevoAbogadoId") Integer nuevoAbogadoId);
}
