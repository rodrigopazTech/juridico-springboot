package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal; 
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
    boolean existsByNumero(String numero);

   @Query("SELECT DISTINCT e FROM Expediente e " + // DISTINCT para evitar duplicados por los joins
           "LEFT JOIN e.gerencia g " +
           "LEFT JOIN e.materia m " +
           "LEFT JOIN e.tipoExpediente te " +
           "LEFT JOIN e.abogadoResponsable a " +
           "LEFT JOIN e.organoJurisdiccional o " +
           "LEFT JOIN ColaboradorExpediente c ON c.expediente.id = e.id " + // <--- JOIN NUEVO
           "WHERE " +
           // --- 1. FILTROS UI ---
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
               "COALESCE(:secMateriaIds, NULL) IS NULL " + // Si es Director (lista nula), pasa.
               "OR m.id IN :secMateriaIds " +              // Si es de mi materia, pasa.
               "OR a.id = :secUsuarioId " +                // Si soy el responsable, pasa.
               "OR (c.usuario.id = :secUsuarioId AND c.fechaExpiracion > CURRENT_TIMESTAMP)" + // <--- NUEVA REGLA: COLABORADOR VIGENTE
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
}