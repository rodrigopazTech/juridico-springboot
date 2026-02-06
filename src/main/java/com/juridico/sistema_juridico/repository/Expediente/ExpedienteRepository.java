package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
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

    // ==========================================
    // 🔍 MÉTODOS DE BÚSQUEDA BÁSICA (HEAD)
    // ==========================================
    Optional<Expediente> findByNumeroIgnoreCase(String numero);
    boolean existsByNumero(String numero);

    // ==========================================
    // 🔐 BÚSQUEDA CON SEGURIDAD (HEAD)
    // ==========================================
   @Query("SELECT DISTINCT e FROM Expediente e " +
           "LEFT JOIN e.gerencia g " +
           "LEFT JOIN e.materia m " +
           "LEFT JOIN e.tipoExpediente te " +
           "LEFT JOIN e.abogadoResponsable a " +
           "LEFT JOIN e.organoJurisdiccional o " +
           "LEFT JOIN ColaboradorExpediente c ON c.expediente.id = e.id " +
           "WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.nombre) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:gerenciaId IS NULL OR g.id = :gerenciaId) " +
           "AND (:materiaId IS NULL OR m.id = :materiaId) " +
           "AND (:tipoId IS NULL OR te.id = :tipoId) " +
           "AND (:prioridad IS NULL OR e.prioridad = :prioridad) " +
           "AND (:etapa IS NULL OR e.etapaProcesal = :etapa) " +
           "AND (:abogadoId IS NULL OR a.id = :abogadoId) " +
           "AND (:secGerenciaId IS NULL OR g.id = :secGerenciaId) " +
           "AND (" +
               "COALESCE(:secMateriaIds, NULL) IS NULL " +
               "OR m.id IN :secMateriaIds " +
               "OR a.id = :secUsuarioId " +
               "OR (c.usuario.id = :secUsuarioId AND c.fechaExpiracion > CURRENT_TIMESTAMP)" +
           ")")
    Page<Expediente> buscarConSeguridad(
            @Param("keyword") String keyword,
            @Param("gerenciaId") Integer gerenciaId,
            @Param("materiaId") Integer materiaId,
            @Param("tipoId") Integer tipoId,
            @Param("prioridad") Prioridad prioridad,
            @Param("etapa") EtapaProcesal etapa,
            @Param("abogadoId") Integer abogadoId,
            @Param("secGerenciaId") Integer secGerenciaId,
            @Param("secMateriaIds") List<Integer> secMateriaIds,
            @Param("secUsuarioId") Integer secUsuarioId,
            Pageable pageable);

    // ==========================================
    // 🛠️ MÉTODO PARA BUSCADOR Y FILTROS (aurora5)
    // ==========================================
    @Query("SELECT e FROM Expediente e WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:materiaId IS NULL OR e.materia.id = :materiaId) AND " +
           "(:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId) AND " +
           "(:tipoExpedienteId IS NULL OR e.tipoExpediente.id = :tipoExpedienteId) AND " +
           "(:prioridad IS NULL OR e.prioridad = :prioridad) AND " +
           "(:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId)")
    Page<Expediente> buscarExpedientes(
        @Param("keyword") String keyword,
        @Param("materiaId") Integer materiaId,
        @Param("gerenciaId") Integer gerenciaId,
        @Param("tipoExpedienteId") Integer tipoExpedienteId,
        @Param("prioridad") Prioridad prioridad,
        @Param("abogadoId") Integer abogadoId,
        Pageable pageable
    );

    // ==========================================
    // 📊 MÉTODOS PARA DASHBOARD (aurora5)
    // ==========================================
    long countByGerenciaId(Long gerenciaId);
    long countByEtapaProcesal(EtapaProcesal etapa);
    long countByEtapaProcesalAndGerenciaId(EtapaProcesal etapa, Long gerenciaId);

    @Query("SELECT e.etapaProcesal, COUNT(e) FROM Expediente e GROUP BY e.etapaProcesal")
    List<Object[]> contarExpedientesPorEstatus();

    @Query("SELECT e.etapaProcesal, COUNT(e) FROM Expediente e WHERE e.gerencia.id = :gerenciaId GROUP BY e.etapaProcesal")
    List<Object[]> contarExpedientesPorEstatusYGerencia(@Param("gerenciaId") Long gerenciaId);

    @Query("SELECT e.abogadoResponsable.nombreCompleto, COUNT(e) FROM Expediente e GROUP BY e.abogadoResponsable.nombreCompleto")
    List<Object[]> contarExpedientesPorUsuario();

    @Query("SELECT e.gerencia.nombre, COUNT(e) FROM Expediente e GROUP BY e.gerencia.nombre")
    List<Object[]> contarExpedientesPorGerencia();

    @Query(value = "SELECT TO_CHAR(created_at, 'Month'), COUNT(*) FROM expedientes GROUP BY TO_CHAR(created_at, 'Month')", nativeQuery = true)
    List<Object[]> contarExpedientesPorMes();

    @Query(value = "SELECT TO_CHAR(created_at, 'Month'), COUNT(*) FROM expedientes WHERE etapa_procesal = 'FIRME' GROUP BY TO_CHAR(created_at, 'Month')", nativeQuery = true)
    List<Object[]> contarExpedientesCompletadosPorMes();
}