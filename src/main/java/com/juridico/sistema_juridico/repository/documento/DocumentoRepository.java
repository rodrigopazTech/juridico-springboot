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

        List<Documento> findByExpedienteIdOrderByFechaSubidaDesc(UUID expedienteId);

        List<Documento> findByCarpetaIdOrderByFechaSubidaDesc(UUID carpetaId);

        // Documentos en la raíz (sin carpeta asignada)
        List<Documento> findByExpedienteIdAndCarpetaIsNullOrderByFechaSubidaDesc(UUID expedienteId);

        long countByCarpetaId(UUID carpetaId);

        @Query("SELECT SUM(d.tamanioBytes) FROM Documento d WHERE d.expedienteId = :expedienteId")
        Long calcularEspacioOcupado(@Param("expedienteId") UUID expedienteId);

        @Query("SELECT d FROM Documento d WHERE d.expedienteId = :expedienteId AND " +
                        "(LOWER(d.nombreOriginal) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
                        "LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
        List<Documento> buscarEnExpediente(@Param("expedienteId") UUID expedienteId,
                        @Param("busqueda") String busqueda);

        @Query("SELECT d FROM Documento d WHERE d.carpeta.id = :carpetaId AND " +
                        "(LOWER(d.nombreOriginal) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
                        "LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
        List<Documento> buscarEnCarpeta(@Param("carpetaId") UUID carpetaId, @Param("busqueda") String busqueda);

        // ========================================
        // QUERIES PARA PAPELERA TEMPORAL
        // ========================================

        /** Documentos activos (no eliminados) por expediente */
        List<Documento> findByExpedienteIdAndEliminadoFalseAndEliminadoDefinitivoFalseOrderByFechaSubidaDesc(UUID expedienteId);

        /** Documentos activos por carpeta */
        List<Documento> findByCarpetaIdAndEliminadoFalseAndEliminadoDefinitivoFalseOrderByFechaSubidaDesc(UUID carpetaId);

        /** Documentos activos en raíz (sin carpeta) */
        List<Documento> findByExpedienteIdAndCarpetaIsNullAndEliminadoFalseAndEliminadoDefinitivoFalseOrderByFechaSubidaDesc(UUID expedienteId);

        /** Documentos en la papelera de un expediente */
        List<Documento> findByExpedienteIdAndEliminadoTrueAndEliminadoDefinitivoFalseOrderByFechaEliminadoDesc(UUID expedienteId);

        /** Documentos en papelera cuya fecha de eliminación es anterior a la fecha límite (para purga automática) */
        @Query("SELECT d FROM Documento d WHERE d.eliminado = true AND d.eliminadoDefinitivo = false AND d.fechaEliminado < :fechaLimite")
        List<Documento> findDocumentosVencidosEnPapelera(@Param("fechaLimite") LocalDateTime fechaLimite);

        /** Contar documentos activos en carpeta (excluye papelera) */
        long countByCarpetaIdAndEliminadoFalseAndEliminadoDefinitivoFalse(UUID carpetaId);

        /** Espacio total excluyendo documentos en papelera */
        @Query("SELECT SUM(d.tamanioBytes) FROM Documento d WHERE d.expedienteId = :expedienteId AND d.eliminado = false AND d.eliminadoDefinitivo = false")
        Long calcularEspacioOcupadoActivo(@Param("expedienteId") UUID expedienteId);

        /** Búsqueda excluyendo papelera */
        @Query("SELECT d FROM Documento d WHERE d.expedienteId = :expedienteId AND d.eliminado = false AND d.eliminadoDefinitivo = false AND " +
                        "(LOWER(d.nombreOriginal) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
                        "LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
        List<Documento> buscarEnExpedienteActivos(@Param("expedienteId") UUID expedienteId,
                        @Param("busqueda") String busqueda);

        /** Verificar si existe un documento activo con el mismo nombre en la ubicación (carpeta o raíz) */
        @Query("SELECT COUNT(d) > 0 FROM Documento d WHERE d.expedienteId = :expedienteId " +
                        "AND d.nombreOriginal = :nombreOriginal AND d.eliminado = false AND d.eliminadoDefinitivo = false " +
                        "AND ((:carpetaId IS NULL AND d.carpeta IS NULL) OR (d.carpeta.id = :carpetaId))")
        boolean existeNombreDuplicado(
                        @Param("expedienteId") UUID expedienteId,
                        @Param("carpetaId") UUID carpetaId,
                        @Param("nombreOriginal") String nombreOriginal);
}
