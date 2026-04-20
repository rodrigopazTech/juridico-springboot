package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
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

        List<Expediente> findTop10ByNumeroContainingIgnoreCase(String numero);


        // ==========================================
        // 🔐 BÚSQUEDA CON SEGURIDAD (HEAD)
        // ==========================================
        @Query("SELECT DISTINCT e FROM Expediente e " +
                        "LEFT JOIN e.gerencia g " +
                        "LEFT JOIN e.materia m " +
                        "LEFT JOIN e.abogadoResponsable a " +
                        "LEFT JOIN e.organoJurisdiccional o " +
                        "LEFT JOIN e.etiquetas et " +
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
                        "AND (:etiquetaId IS NULL OR et.id = :etiquetaId) " +
                        "AND (:prioridadId IS NULL OR e.prioridad.id = :prioridadId) " +
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
                        @Param("prioridadId") Integer prioridadId,
                        @Param("etapa") EtapaProcesal etapa,
                        @Param("abogadoId") Integer abogadoId,
                        @Param("etiquetaId") Integer etiquetaId,
                        @Param("secGerenciaId") Integer secGerenciaId,
                        @Param("secMateriaIds") List<Integer> secMateriaIds,
                        @Param("secUsuarioId") Integer secUsuarioId,
                        Pageable pageable);

        // ==========================================
        // 🛠️ MÉTODO PARA BUSCADOR Y FILTROS (aurora5)
        // ==========================================
        @Query("SELECT e FROM Expediente e WHERE " +
                        "(:keyword IS NULL OR :keyword = '' OR LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
                        +
                        "(:materiaId IS NULL OR e.materia.id = :materiaId) AND " +
                        "(:gerenciaId IS NULL OR e.gerencia.id = :gerenciaId) AND " +
                        "(:prioridadId IS NULL OR e.prioridad.id = :prioridadId) AND " +
                        "(:abogadoId IS NULL OR e.abogadoResponsable.id = :abogadoId)")
        Page<Expediente> buscarExpedientes(
                        @Param("keyword") String keyword,
                        @Param("materiaId") Integer materiaId,
                        @Param("gerenciaId") Integer gerenciaId,
                        @Param("prioridadId") Integer prioridadId,
                        @Param("abogadoId") Integer abogadoId,
                        Pageable pageable);

        // ==========================================
        // 📊 MÉTODOS PARA DASHBOARD (aurora5)
        // ==========================================
        long countByGerenciaId(Long gerenciaId);

        // Abogado dashboard
        long countByAbogadoResponsableId(Integer abogadoId);

        long countByAbogadoResponsableIdAndMateriaId(Integer abogadoId, Integer materiaId);

        long countByEtapaProcesal(EtapaProcesal etapa);

        long countByEtapaProcesalAndGerenciaId(EtapaProcesal etapa, Long gerenciaId);

        long countByEtapaProcesalAndAbogadoResponsableId(EtapaProcesal etapa, Integer abogadoId);

        long countByEtapaProcesalAndAbogadoResponsableIdAndMateriaId(EtapaProcesal etapa, Integer abogadoId, Integer materiaId);

        // Filtro por lista de gerencias (para grupos)
        long countByGerenciaIdIn(List<Long> gerenciaIds);

        long countByEtapaProcesalAndGerenciaIdIn(EtapaProcesal etapa, List<Long> gerenciaIds);

        @Query("SELECT e.etapaProcesal, COUNT(e) FROM Expediente e GROUP BY e.etapaProcesal")
        List<Object[]> contarExpedientesPorEstatus();

        @Query("SELECT e.etapaProcesal, COUNT(e) FROM Expediente e WHERE e.gerencia.id = :gerenciaId GROUP BY e.etapaProcesal")
        List<Object[]> contarExpedientesPorEstatusYGerencia(@Param("gerenciaId") Long gerenciaId);

        @Query("SELECT e.etapaProcesal, COUNT(e) FROM Expediente e WHERE e.gerencia.id IN :gerenciaIds GROUP BY e.etapaProcesal")
        List<Object[]> contarExpedientesPorEstatusYGerencias(@Param("gerenciaIds") List<Long> gerenciaIds);

        @Query("SELECT e.etapaProcesal, COUNT(e) FROM Expediente e WHERE e.abogadoResponsable.id = :abogadoId GROUP BY e.etapaProcesal")
        List<Object[]> contarExpedientesPorEstatusYAbogado(@Param("abogadoId") Integer abogadoId);

        @Query("SELECT e.etapaProcesal, COUNT(e) FROM Expediente e WHERE e.abogadoResponsable.id = :abogadoId AND e.materia.id = :materiaId GROUP BY e.etapaProcesal")
        List<Object[]> contarExpedientesPorEstatusYAbogadoYMateria(@Param("abogadoId") Integer abogadoId, @Param("materiaId") Integer materiaId);

        @Query("SELECT e.prioridad.nombre, COUNT(e) FROM Expediente e WHERE e.abogadoResponsable.id = :abogadoId AND e.prioridad IS NOT NULL GROUP BY e.prioridad.nombre")
        List<Object[]> contarExpedientesPorPrioridadYAbogado(@Param("abogadoId") Integer abogadoId);

        @Query("SELECT e.prioridad.nombre, COUNT(e) FROM Expediente e WHERE e.abogadoResponsable.id = :abogadoId AND e.materia.id = :materiaId AND e.prioridad IS NOT NULL GROUP BY e.prioridad.nombre")
        List<Object[]> contarExpedientesPorPrioridadYAbogadoYMateria(@Param("abogadoId") Integer abogadoId, @Param("materiaId") Integer materiaId);

        @Query("SELECT e.abogadoResponsable.nombreCompleto, COUNT(e) FROM Expediente e GROUP BY e.abogadoResponsable.nombreCompleto")
        List<Object[]> contarExpedientesPorUsuario();

        @Query("SELECT e.abogadoResponsable.nombreCompleto, COUNT(e) FROM Expediente e WHERE e.gerencia.id = :gerenciaId GROUP BY e.abogadoResponsable.nombreCompleto")
        List<Object[]> contarExpedientesPorUsuarioYGerencia(@Param("gerenciaId") Long gerenciaId);

        @Query("SELECT e.abogadoResponsable.nombreCompleto, COUNT(e) FROM Expediente e WHERE e.gerencia.id IN :gerenciaIds GROUP BY e.abogadoResponsable.nombreCompleto")
        List<Object[]> contarExpedientesPorUsuarioYGerencias(@Param("gerenciaIds") List<Long> gerenciaIds);

        @Query("SELECT e.gerencia.nombre, COUNT(e) FROM Expediente e GROUP BY e.gerencia.nombre")
        List<Object[]> contarExpedientesPorGerencia();

        @Query(value = "SELECT TO_CHAR(created_at, 'Month'), COUNT(*) FROM expedientes GROUP BY TO_CHAR(created_at, 'Month')", nativeQuery = true)
        List<Object[]> contarExpedientesPorMes();

        @Query(value = "SELECT TO_CHAR(created_at, 'Month'), COUNT(*) FROM expedientes WHERE etapa_procesal = 'FIRME' GROUP BY TO_CHAR(created_at, 'Month')", nativeQuery = true)
        List<Object[]> contarExpedientesCompletadosPorMes();

        @Query(value = "SELECT TO_CHAR(created_at, 'Month'), COUNT(*) FROM expedientes WHERE gerencia_id IN :gerenciaIds AND etapa_procesal = 'FIRME' GROUP BY TO_CHAR(created_at, 'Month')", nativeQuery = true)
        List<Object[]> contarExpedientesCompletadosPorMesYGerencias(@Param("gerenciaIds") List<Long> gerenciaIds);

        List<Expediente> findAllByAbogadoResponsableId(Integer abogadoId);
}
