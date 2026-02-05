package com.juridico.sistema_juridico.repository.documento;

import com.juridico.sistema_juridico.Entity.documento.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
