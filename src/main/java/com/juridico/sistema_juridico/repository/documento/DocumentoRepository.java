package com.juridico.sistema_juridico.repository.documento;

import com.juridico.sistema_juridico.Entity.documento.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

        @Query("SELECT d FROM Documento d WHERE d.expedienteId = :expedienteId AND (d.eliminado = false OR d.eliminado IS NULL) ORDER BY d.fechaSubida DESC")
        List<Documento> findActivosByExpedienteIdOrderByFechaSubidaDesc(@Param("expedienteId") UUID expedienteId);

        @Query("SELECT d FROM Documento d WHERE d.carpeta.id = :carpetaId AND (d.eliminado = false OR d.eliminado IS NULL) ORDER BY d.fechaSubida DESC")
        List<Documento> findActivosByCarpetaIdOrderByFechaSubidaDesc(@Param("carpetaId") UUID carpetaId);

        @Query("SELECT d FROM Documento d WHERE d.expedienteId = :expedienteId AND d.carpeta IS NULL AND (d.eliminado = false OR d.eliminado IS NULL) ORDER BY d.fechaSubida DESC")
        List<Documento> findActivosByExpedienteIdAndCarpetaIsNullOrderByFechaSubidaDesc(@Param("expedienteId") UUID expedienteId);

        @Query("SELECT COUNT(d) FROM Documento d WHERE d.carpeta.id = :carpetaId AND (d.eliminado = false OR d.eliminado IS NULL)")
        long countActivosByCarpetaId(@Param("carpetaId") UUID carpetaId);

        @Query("SELECT SUM(d.tamanioBytes) FROM Documento d WHERE d.expedienteId = :expedienteId AND (d.eliminado = false OR d.eliminado IS NULL)")
        Long calcularEspacioOcupado(@Param("expedienteId") UUID expedienteId);

        @Query("SELECT d FROM Documento d WHERE d.expedienteId = :expedienteId AND " +
                        "(d.eliminado = false OR d.eliminado IS NULL) AND " +
                        "(LOWER(d.nombreOriginal) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
                        "LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
        List<Documento> buscarEnExpediente(@Param("expedienteId") UUID expedienteId,
                        @Param("busqueda") String busqueda);

        @Query("SELECT d FROM Documento d WHERE d.carpeta.id = :carpetaId AND " +
                        "(d.eliminado = false OR d.eliminado IS NULL) AND " +
                        "(LOWER(d.nombreOriginal) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
                        "LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
        List<Documento> buscarEnCarpeta(@Param("carpetaId") UUID carpetaId, @Param("busqueda") String busqueda);

        @Query("SELECT d FROM Documento d WHERE d.expedienteId = :expedienteId AND d.eliminado = true ORDER BY d.fechaEliminacion DESC")
        List<Documento> findPapeleraByExpedienteIdOrderByFechaEliminacionDesc(@Param("expedienteId") UUID expedienteId);

        List<Documento> findByEliminadoTrueAndFechaEliminacionDefinitivaBefore(LocalDateTime fechaLimite);

        // ========================================
        // QUERIES PARA PAPELERA TEMPORAL
        // ========================================

        /** Documentos activos (no eliminados) por expediente */
        List<Documento> findByExpedienteIdAndEliminadoFalseOrderByFechaSubidaDesc(UUID expedienteId);

        /** Documentos activos por carpeta */
        List<Documento> findByCarpetaIdAndEliminadoFalseOrderByFechaSubidaDesc(UUID carpetaId);

        /** Todos los documentos por carpeta (incluyendo papelera) */
        List<Documento> findByCarpetaIdOrderByFechaSubidaDesc(UUID carpetaId);

        /** Documentos activos en raíz (sin carpeta) */
        List<Documento> findByExpedienteIdAndCarpetaIsNullAndEliminadoFalseOrderByFechaSubidaDesc(UUID expedienteId);

        /** Documentos en la papelera de un expediente */
        List<Documento> findByExpedienteIdAndEliminadoTrueOrderByFechaEliminacionDesc(UUID expedienteId);

        /** Documentos en papelera cuya fecha de eliminación es anterior a la fecha límite (para purga automática) */
        @Query("SELECT d FROM Documento d WHERE d.eliminado = true AND d.fechaEliminacion < :fechaLimite")
        List<Documento> findDocumentosVencidosEnPapelera(@Param("fechaLimite") LocalDateTime fechaLimite);

        /** Contar documentos activos en carpeta (excluye papelera) */
        long countByCarpetaIdAndEliminadoFalse(UUID carpetaId);

        /** Espacio total excluyendo documentos en papelera */
        @Query("SELECT SUM(d.tamanioBytes) FROM Documento d WHERE d.expedienteId = :expedienteId AND d.eliminado = false")
        Long calcularEspacioOcupadoActivo(@Param("expedienteId") UUID expedienteId);

        /** Búsqueda excluyendo papelera */
        @Query("SELECT d FROM Documento d WHERE d.expedienteId = :expedienteId AND d.eliminado = false AND " +
                        "(LOWER(d.nombreOriginal) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
                        "LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
        List<Documento> buscarEnExpedienteActivos(@Param("expedienteId") UUID expedienteId,
                        @Param("busqueda") String busqueda);
}
