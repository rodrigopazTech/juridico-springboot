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

        long countByAbogadoCompareceIdOrUsuarioDelegadoId(Integer abogadoCompareceId, Integer usuarioDelegadoId);

        // --- 1. BUSQUEDA PAGINADA (INDEX) ---
        @Query("SELECT a FROM Audiencia a " +
                        "LEFT JOIN a.expediente e " +
                        "LEFT JOIN a.contexto c " +
                        "LEFT JOIN a.tipoAudiencia ta " +
                        "LEFT JOIN e.abogadoResponsable abr " +
                        "LEFT JOIN a.abogadoComparece abc " +
                        "LEFT JOIN a.usuarioDelegado ud " +
                        "LEFT JOIN e.gerencia eg " +
                        "LEFT JOIN c.gerencia cg " +
                        "LEFT JOIN e.materia em " +
                        "LEFT JOIN c.materia cm " +
                        "WHERE " +
                        "(:keyword IS NULL OR :keyword = '' OR " +
                        "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.numeroExpedienteSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(e.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.salaLugar) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(ta.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(abr.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(abc.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(ud.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.estatusAudiencia) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "CONCAT(a.fechaAudiencia, '') LIKE CONCAT('%', :keyword, '%') OR " +
                        "CONCAT(a.horaAudiencia, '') LIKE CONCAT('%', :keyword, '%') OR " +
                        "CONCAT(a.id, '') LIKE CONCAT('%', :keyword, '%')) " +
                        "AND (:tipo IS NULL OR :tipo = '' OR ta.nombre = :tipo) " +
                        "AND (:gerencia IS NULL OR :gerencia = '' OR eg.nombre = :gerencia OR cg.nombre = :gerencia) " +
                        "AND (:materia IS NULL OR :materia = '' OR em.nombre = :materia OR cm.nombre = :materia) " +
                        "AND (:estatus IS NULL OR a.estatusAudiencia = :estatus) " +
                        "AND (a.fechaAudiencia >= :fechaInicio) " +
                        "AND (a.fechaAudiencia <= :fechaFin) " +
                        "AND (:targetAbogadoId IS NULL OR (abr.id = :targetAbogadoId OR abc.id = :targetAbogadoId OR a.usuarioDelegado.id = :targetAbogadoId)) " +
                        "AND (:filtroUsuarioId IS NULL OR (abr.id = :filtroUsuarioId OR abc.id = :filtroUsuarioId OR a.usuarioDelegado.id = :filtroUsuarioId)) " +
                        "AND (:filtroGerenciaId IS NULL OR eg.id = :filtroGerenciaId OR cg.id = :filtroGerenciaId) " +
                        "AND ((:filtroMateriaIds) IS NULL OR em.id IN (:filtroMateriaIds) OR cm.id IN (:filtroMateriaIds)) " +
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
                        "LEFT JOIN a.contexto c " +
                        "LEFT JOIN a.tipoAudiencia ta " +
                        "LEFT JOIN e.abogadoResponsable abr " +
                        "LEFT JOIN a.abogadoComparece abc " +
                        "LEFT JOIN a.usuarioDelegado ud " +
                        "LEFT JOIN e.gerencia eg " +
                        "LEFT JOIN c.gerencia cg " +
                        "LEFT JOIN e.materia em " +
                        "LEFT JOIN c.materia cm " +
                        "WHERE a.estatusAudiencia = :estatus " +
                        "AND (:keyword IS NULL OR :keyword = '' OR " +
                        "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.numeroExpedienteSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(ta.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "CONCAT(a.id, '') LIKE CONCAT('%', :keyword, '%')) " +
                        "AND a.fechaDesahogo >= :fechaInicio AND a.fechaDesahogo <= :fechaFin " +
                        "AND (:gerenciaId IS NULL OR eg.id = :gerenciaId OR cg.id = :gerenciaId) " +
                        "AND (:materiaId IS NULL OR em.id = :materiaId OR cm.id = :materiaId) " +
                        "AND (:abogadoId IS NULL OR abr.id = :abogadoId OR abc.id = :abogadoId OR ud.id = :abogadoId) " +
                        "AND (:tipoAudienciaId IS NULL OR ta.id = :tipoAudienciaId) " +
                        "AND (:documento IS NULL OR :documento = '' " +
                        "OR (:documento = 'con' AND a.actaDocumento IS NOT NULL AND a.actaDocumento <> '') " +
                        "OR (:documento = 'sin' AND (a.actaDocumento IS NULL OR a.actaDocumento = ''))) " +
                        "AND (:filtroRoleGerenciaId IS NULL OR eg.id = :filtroRoleGerenciaId OR cg.id = :filtroRoleGerenciaId) " +
                        "AND ((:filtroRoleMateriaIds) IS NULL OR em.id IN (:filtroRoleMateriaIds) OR cm.id IN (:filtroRoleMateriaIds)) " +
                        "AND (:filtroRoleUsuarioId IS NULL OR abr.id = :filtroRoleUsuarioId OR abc.id = :filtroRoleUsuarioId OR a.usuarioDelegado.id = :filtroRoleUsuarioId) " +
                        "ORDER BY a.fechaDesahogo DESC, a.id DESC")
        Page<Audiencia> buscarParaHistorial(
                        @Param("keyword") String keyword,
                        @Param("estatus") EstatusAudiencia estatus,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin,
                        @Param("gerenciaId") Integer gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("tipoAudienciaId") Integer tipoAudienciaId,
                        @Param("documento") String documento,
                        @Param("filtroRoleGerenciaId") Integer filtroRoleGerenciaId,
                        @Param("filtroRoleMateriaIds") List<Integer> filtroRoleMateriaIds,
                        @Param("filtroRoleUsuarioId") Integer filtroRoleUsuarioId,
                        Pageable pageable);

        @Query("SELECT a FROM Audiencia a " +
                        "LEFT JOIN a.expediente e " +
                        "LEFT JOIN a.contexto c " +
                        "LEFT JOIN a.tipoAudiencia ta " +
                        "LEFT JOIN e.abogadoResponsable abr " +
                        "LEFT JOIN a.abogadoComparece abc " +
                        "LEFT JOIN a.usuarioDelegado ud " +
                        "LEFT JOIN e.gerencia eg " +
                        "LEFT JOIN c.gerencia cg " +
                        "LEFT JOIN e.materia em " +
                        "LEFT JOIN c.materia cm " +
                        "WHERE " +
                        "(:keyword IS NULL OR :keyword = '' OR " +
                        "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.numeroExpedienteSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(e.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.salaLugar) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(ta.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(abr.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(abc.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(ud.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.estatusAudiencia) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "CONCAT(a.fechaAudiencia, '') LIKE CONCAT('%', :keyword, '%') OR " +
                        "CONCAT(a.horaAudiencia, '') LIKE CONCAT('%', :keyword, '%') OR " +
                        "CONCAT(a.id, '') LIKE CONCAT('%', :keyword, '%')) " +
                        "AND (:tipo IS NULL OR :tipo = '' OR ta.nombre = :tipo) " +
                        "AND (:gerencia IS NULL OR :gerencia = '' OR eg.nombre = :gerencia OR cg.nombre = :gerencia) " +
                        "AND (:materia IS NULL OR :materia = '' OR em.nombre = :materia OR cm.nombre = :materia) " +
                        "AND (:estatus IS NULL OR a.estatusAudiencia = :estatus) " +
                        "AND (a.fechaAudiencia >= :fechaInicio) " +
                        "AND (a.fechaAudiencia <= :fechaFin) " +
                        "AND (:targetAbogadoId IS NULL OR (abr.id = :targetAbogadoId OR abc.id = :targetAbogadoId OR a.usuarioDelegado.id = :targetAbogadoId)) " +
                        "AND (:filtroUsuarioId IS NULL OR (abr.id = :filtroUsuarioId OR abc.id = :filtroUsuarioId OR a.usuarioDelegado.id = :filtroUsuarioId)) " +
                        "AND (:filtroGerenciaId IS NULL OR eg.id = :filtroGerenciaId OR cg.id = :filtroGerenciaId) " +
                        "AND ((:filtroMateriaIds) IS NULL OR em.id IN (:filtroMateriaIds) OR cm.id IN (:filtroMateriaIds)) " +
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

        List<Audiencia> findByExpedienteIdOrderByFechaAudienciaDesc(UUID expedienteId);

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
        @Query("SELECT COUNT(a) FROM Audiencia a LEFT JOIN a.expediente e LEFT JOIN a.contexto c WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
        long countByExpedienteGerenciaIdAndEstatusPendiente(@Param("gerenciaId") Long gerenciaId);

        // Conteo por lista de gerencias (solo audiencias pendientes)
        @Query("SELECT COUNT(a) FROM Audiencia a LEFT JOIN a.expediente e LEFT JOIN a.contexto c WHERE (e.gerencia.id IN :gerenciaIds OR c.gerencia.id IN :gerenciaIds) AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
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

        @Query("SELECT a FROM Audiencia a LEFT JOIN a.expediente e LEFT JOIN a.contexto c WHERE a.estatusAudiencia = :estatus AND a.fechaDesahogo BETWEEN :inicio AND :fin AND (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) ORDER BY a.fechaDesahogo DESC")
        Page<Audiencia> findByEstatusGerencia(
                        @Param("estatus") EstatusAudiencia estatus,
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin,
                        @Param("gerenciaId") Long gerenciaId,
                        Pageable pageable);

        @Query("SELECT a FROM Audiencia a LEFT JOIN a.expediente e LEFT JOIN a.contexto c WHERE a.estatusAudiencia = :estatus AND a.fechaDesahogo BETWEEN :inicio AND :fin AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds) ORDER BY a.fechaDesahogo DESC")
        Page<Audiencia> findByEstatusMaterias(
                        @Param("estatus") EstatusAudiencia estatus,
                        @Param("inicio") LocalDate inicio,
                        @Param("fin") LocalDate fin,
                        @Param("materiaIds") List<Integer> materiaIds,
                        Pageable pageable);

        @Query("SELECT a.estatusAudiencia, COUNT(a) FROM Audiencia a " +
                        "LEFT JOIN a.expediente e " +
                        "LEFT JOIN a.contexto c " +
                        "LEFT JOIN a.tipoAudiencia ta " +
                        "LEFT JOIN e.abogadoResponsable abr " +
                        "LEFT JOIN a.abogadoComparece abc " +
                        "LEFT JOIN a.usuarioDelegado ud " +
                        "LEFT JOIN e.gerencia eg " +
                        "LEFT JOIN c.gerencia cg " +
                        "LEFT JOIN e.materia em " +
                        "LEFT JOIN c.materia cm " +
                        "WHERE " +
                        "(:keyword IS NULL OR :keyword = '' OR " +
                        "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.numeroExpedienteSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(e.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.salaLugar) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(ta.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(abr.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(abc.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(ud.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.estatusAudiencia) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "CONCAT(a.fechaAudiencia, '') LIKE CONCAT('%', :keyword, '%') OR " +
                        "CONCAT(a.horaAudiencia, '') LIKE CONCAT('%', :keyword, '%') OR " +
                        "CONCAT(a.id, '') LIKE CONCAT('%', :keyword, '%')) " +
                        "AND (:tipo IS NULL OR :tipo = '' OR ta.nombre = :tipo) " +
                        "AND (:gerencia IS NULL OR :gerencia = '' OR eg.nombre = :gerencia OR cg.nombre = :gerencia) " +
                        "AND (:materia IS NULL OR :materia = '' OR em.nombre = :materia OR cm.nombre = :materia) " +
                        "AND (a.fechaAudiencia >= :fechaInicio) " +
                        "AND (a.fechaAudiencia <= :fechaFin) " +
                        "AND (:targetAbogadoId IS NULL OR (abr.id = :targetAbogadoId OR abc.id = :targetAbogadoId OR a.usuarioDelegado.id = :targetAbogadoId)) " +
                        "AND (:filtroUsuarioId IS NULL OR (abr.id = :filtroUsuarioId OR abc.id = :filtroUsuarioId OR a.usuarioDelegado.id = :filtroUsuarioId)) " +
                        "AND (:filtroGerenciaId IS NULL OR eg.id = :filtroGerenciaId OR cg.id = :filtroGerenciaId) " +
                        "AND ((:filtroMateriaIds) IS NULL OR em.id IN (:filtroMateriaIds) OR cm.id IN (:filtroMateriaIds)) " +
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

        @Query("SELECT a FROM Audiencia a WHERE a.abogadoComparece.id = :usuarioId OR a.expediente.abogadoResponsable.id = :usuarioId OR a.usuarioDelegado.id = :usuarioId")
        List<Audiencia> findByUsuarioId(@Param("usuarioId") Integer usuarioId);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        WHERE (e.abogadoResponsable.id = :usuarioId OR a.abogadoComparece.id = :usuarioId OR a.usuarioDelegado.id = :usuarioId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia = :hoy
                        """)
        long countMisAudienciasHoy(@Param("usuarioId") Integer usuarioId,
                        @Param("materiaId") Integer materiaId,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        WHERE (e.abogadoResponsable.id = :usuarioId OR a.abogadoComparece.id = :usuarioId OR a.usuarioDelegado.id = :usuarioId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :hoy AND :hasta
                        """)
        long countMisAudienciasProximas(@Param("usuarioId") Integer usuarioId,
                        @Param("materiaId") Integer materiaId,
                        @Param("hoy") LocalDate hoy,
                        @Param("hasta") LocalDate hasta);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        WHERE (e.abogadoResponsable.id = :usuarioId OR a.abogadoComparece.id = :usuarioId OR a.usuarioDelegado.id = :usuarioId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia < :hoy
                        """)
        long countMisAudienciasAtrasadas(@Param("usuarioId") Integer usuarioId,
                        @Param("materiaId") Integer materiaId,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT a
                        FROM Audiencia a
                        LEFT JOIN FETCH a.expediente e
                        LEFT JOIN FETCH a.contexto c
                        LEFT JOIN FETCH a.tipoAudiencia ta
                        LEFT JOIN FETCH a.abogadoComparece ac
                        LEFT JOIN FETCH a.usuarioDelegado ud
                        WHERE (e.abogadoResponsable.id = :usuarioId OR a.abogadoComparece.id = :usuarioId OR a.usuarioDelegado.id = :usuarioId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia >= :hoy
                        ORDER BY a.fechaAudiencia ASC, a.horaAudiencia ASC
                        """)
        List<Audiencia> findMisProximasAudiencias(@Param("usuarioId") Integer usuarioId,
                        @Param("materiaId") Integer materiaId,
                        @Param("hoy") LocalDate hoy,
                        Pageable pageable);

        @Query("SELECT DISTINCT a.expediente FROM Audiencia a WHERE a.abogadoComparece.id = :usuarioId AND a.estatusAudiencia != com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.CONCLUIDA")
        List<com.juridico.sistema_juridico.Entity.expediente.Expediente> findExpedientesByAbogadoInvitadoActivo(@Param("usuarioId") Integer usuarioId);

        // --- MÉTODOS PARA JEFE DE DEPTO (POR MATERIAS) ---
        @Query("SELECT COUNT(a) FROM Audiencia a LEFT JOIN a.expediente e LEFT JOIN a.contexto c WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId) AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds) AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE")
        long countByExpedienteGerenciaIdAndMateriaIdInAndEstatusPendiente(@Param("gerenciaId") Long gerenciaId, @Param("materiaIds") List<Integer> materiaIds);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia = :hoy
                        """)
        long countDepartamentoAudienciasHoy(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaIds") List<Integer> materiaIds,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :hoy AND :hasta
                        """)
        long countDepartamentoAudienciasProximas(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaIds") List<Integer> materiaIds,
                        @Param("hoy") LocalDate hoy,
                        @Param("hasta") LocalDate hasta);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia < :hoy
                        """)
        long countDepartamentoAudienciasAtrasadas(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaIds") List<Integer> materiaIds,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT a
                        FROM Audiencia a
                        LEFT JOIN FETCH a.expediente e
                        LEFT JOIN FETCH e.abogadoResponsable ar
                        LEFT JOIN FETCH a.contexto c
                        LEFT JOIN FETCH a.tipoAudiencia ta
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (e.materia.id IN :materiaIds OR c.materia.id IN :materiaIds)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia >= :hoy
                        ORDER BY a.fechaAudiencia ASC, a.horaAudiencia ASC
                        """)
        List<Audiencia> findDepartamentoProximasAudiencias(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaIds") List<Integer> materiaIds,
                        @Param("hoy") LocalDate hoy,
                        Pageable pageable);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia = :hoy
                        """)
        long countGerenciaAudienciasHoy(@Param("gerenciaId") Long gerenciaId,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :hoy AND :hasta
                        """)
        long countGerenciaAudienciasProximas(@Param("gerenciaId") Long gerenciaId,
                        @Param("hoy") LocalDate hoy,
                        @Param("hasta") LocalDate hasta);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia < :hoy
                        """)
        long countGerenciaAudienciasAtrasadas(@Param("gerenciaId") Long gerenciaId,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT a
                        FROM Audiencia a
                        LEFT JOIN FETCH a.expediente e
                        LEFT JOIN FETCH e.abogadoResponsable ar
                        LEFT JOIN FETCH a.contexto c
                        LEFT JOIN FETCH a.tipoAudiencia ta
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia >= :hoy
                        ORDER BY a.fechaAudiencia ASC, a.horaAudiencia ASC
                        """)
        List<Audiencia> findGerenciaProximasAudiencias(@Param("gerenciaId") Long gerenciaId,
                        @Param("hoy") LocalDate hoy,
                        Pageable pageable);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia = :hoy
                        """)
        long countGerenciaAudienciasHoyFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :hoy AND :hasta
                        """)
        long countGerenciaAudienciasProximasFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("hoy") LocalDate hoy,
                        @Param("hasta") LocalDate hasta);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia < :hoy
                        """)
        long countGerenciaAudienciasAtrasadasFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT a
                        FROM Audiencia a
                        LEFT JOIN FETCH a.expediente e
                        LEFT JOIN FETCH e.abogadoResponsable ar
                        LEFT JOIN FETCH a.contexto c
                        LEFT JOIN FETCH a.tipoAudiencia ta
                        LEFT JOIN FETCH a.abogadoComparece ac
                        LEFT JOIN FETCH a.usuarioDelegado ud
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR ar.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :fechaInicio AND :fechaFin
                        ORDER BY a.fechaAudiencia ASC, a.horaAudiencia ASC
                        """)
        List<Audiencia> findGerenciaAudienciasPeriodoFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin,
                        Pageable pageable);

        @Query("""
                        SELECT COALESCE(e.abogadoResponsable.nombreCompleto, ac.nombreCompleto, ud.nombreCompleto, 'Sin responsable'), COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :fechaInicio AND :fechaFin
                        GROUP BY COALESCE(e.abogadoResponsable.nombreCompleto, ac.nombreCompleto, ud.nombreCompleto, 'Sin responsable')
                        """)
        List<Object[]> contarGerenciaAudienciasPorUsuarioFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin);

        @Query("""
                        SELECT COALESCE(e.materia.nombre, c.materia.nombre, 'Sin materia'), COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :fechaInicio AND :fechaFin
                        GROUP BY COALESCE(e.materia.nombre, c.materia.nombre, 'Sin materia')
                        """)
        List<Object[]> contarGerenciaAudienciasPorMateriaFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin);

        @Query("""
                        SELECT u.nombreCompleto, COUNT(a)
                        FROM Audiencia a
                        JOIN a.expediente e
                        JOIN e.abogadoResponsable u
                        WHERE e.gerencia.id = :gerenciaId AND e.materia.id IN :materiaIds
                        GROUP BY u.nombreCompleto
                        """)
        List<Object[]> contarAudienciasPorUsuarioYGerenciaYMateriaIn(@Param("gerenciaId") Long gerenciaId, @Param("materiaIds") List<Integer> materiaIds);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia = :hoy
                        """)
        long countDirectivoAudienciasHoyFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :hoy AND :hasta
                        """)
        long countDirectivoAudienciasProximasFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("hoy") LocalDate hoy,
                        @Param("hasta") LocalDate hasta);

        @Query("""
                        SELECT COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia < :hoy
                        """)
        long countDirectivoAudienciasAtrasadasFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("hoy") LocalDate hoy);

        @Query("""
                        SELECT a
                        FROM Audiencia a
                        LEFT JOIN FETCH a.expediente e
                        LEFT JOIN FETCH e.abogadoResponsable ar
                        LEFT JOIN FETCH a.contexto c
                        LEFT JOIN FETCH a.tipoAudiencia ta
                        LEFT JOIN FETCH a.abogadoComparece ac
                        LEFT JOIN FETCH a.usuarioDelegado ud
                        WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR ar.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :fechaInicio AND :fechaFin
                        ORDER BY a.fechaAudiencia ASC, a.horaAudiencia ASC
                        """)
        List<Audiencia> findDirectivoAudienciasPeriodoFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin,
                        Pageable pageable);

        @Query("""
                        SELECT COALESCE(e.abogadoResponsable.nombreCompleto, ac.nombreCompleto, ud.nombreCompleto, 'Sin responsable'), COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :fechaInicio AND :fechaFin
                        GROUP BY COALESCE(e.abogadoResponsable.nombreCompleto, ac.nombreCompleto, ud.nombreCompleto, 'Sin responsable')
                        """)
        List<Object[]> contarDirectivoAudienciasPorUsuarioFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin);

        @Query("""
                        SELECT COALESCE(e.materia.nombre, c.materia.nombre, 'Sin materia'), COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :fechaInicio AND :fechaFin
                        GROUP BY COALESCE(e.materia.nombre, c.materia.nombre, 'Sin materia')
                        """)
        List<Object[]> contarDirectivoAudienciasPorMateriaFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin);

        @Query("""
                        SELECT COALESCE(e.gerencia.nombre, c.gerencia.nombre, 'Sin gerencia'), COUNT(a)
                        FROM Audiencia a
                        LEFT JOIN a.expediente e
                        LEFT JOIN a.contexto c
                        LEFT JOIN a.abogadoComparece ac
                        LEFT JOIN a.usuarioDelegado ud
                        WHERE (:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId OR c.gerencia.id = :gerenciaId)
                          AND (:materiaId IS NULL OR e.materia.id = :materiaId OR c.materia.id = :materiaId)
                          AND (:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId OR ac.id = :abogadoId OR ud.id = :abogadoId)
                          AND a.estatusAudiencia = com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia.PENDIENTE
                          AND a.fechaAudiencia BETWEEN :fechaInicio AND :fechaFin
                        GROUP BY COALESCE(e.gerencia.nombre, c.gerencia.nombre, 'Sin gerencia')
                        """)
        List<Object[]> contarDirectivoAudienciasPorGerenciaFiltrado(@Param("gerenciaId") Long gerenciaId,
                        @Param("materiaId") Integer materiaId,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin);

        List<Audiencia> findByExpedienteAndFechaAudienciaAndHoraAudiencia(com.juridico.sistema_juridico.Entity.expediente.Expediente expediente, java.time.LocalDate fechaAudiencia, java.time.LocalTime horaAudiencia);

        @Query("SELECT COUNT(a) > 0 FROM Audiencia a WHERE a.expediente.id = :expedienteId AND a.abogadoComparece.id = :usuarioId AND a.fechaAudiencia BETWEEN :fechaInicio AND :fechaFin")
        boolean existsByExpedienteIdAndAbogadoCompareceIdAndFechaAudienciaBetween(
                        @Param("expedienteId") UUID expedienteId,
                        @Param("usuarioId") Integer usuarioId,
                        @Param("fechaInicio") LocalDate fechaInicio,
                        @Param("fechaFin") LocalDate fechaFin);
}
