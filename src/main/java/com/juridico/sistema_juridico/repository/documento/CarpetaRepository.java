package com.juridico.sistema_juridico.repository.documento;

import com.juridico.sistema_juridico.Entity.documento.Carpeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarpetaRepository extends JpaRepository<Carpeta, UUID> {

    /**
     * Obtener carpetas raíz de un expediente (sin carpeta padre)
     */
    List<Carpeta> findByExpedienteIdAndCarpetaPadreIsNullOrderByOrdenAsc(UUID expedienteId);

    /**
     * Obtener subcarpetas de una carpeta específica
     */
    List<Carpeta> findByCarpetaPadreIdOrderByOrdenAsc(UUID carpetaPadreId);

    /**
     * Obtener todas las carpetas de un expediente
     */
    List<Carpeta> findByExpedienteIdOrderByOrdenAsc(UUID expedienteId);

    /**
     * Verificar si existe una carpeta con el mismo nombre en el mismo nivel
     */
    @Query("SELECT COUNT(c) > 0 FROM Carpeta c WHERE c.expedienteId = :expedienteId " +
            "AND c.nombre = :nombre " +
            "AND (:carpetaPadreId IS NULL AND c.carpetaPadre IS NULL OR c.carpetaPadre.id = :carpetaPadreId)")
    boolean existsByNombreInSameLevel(@Param("expedienteId") UUID expedienteId,
            @Param("nombre") String nombre,
            @Param("carpetaPadreId") UUID carpetaPadreId);

    /**
     * Obtener carpeta por expediente, nombre y carpeta padre
     */
    Optional<Carpeta> findByExpedienteIdAndNombreAndCarpetaPadreId(UUID expedienteId, String nombre,
            UUID carpetaPadreId);

    /**
     * Obtener carpeta por expediente y nombre (nivel raíz)
     */
    Optional<Carpeta> findByExpedienteIdAndNombreAndCarpetaPadreIsNull(UUID expedienteId, String nombre);

    /**
     * Contar subcarpetas de una carpeta
     */
    long countByCarpetaPadreId(UUID carpetaPadreId);

    /**
     * Verificar si una carpeta tiene subcarpetas
     */
    boolean existsByCarpetaPadreId(UUID carpetaPadreId);

    /**
     * Eliminar todas las carpetas de un expediente
     */
    void deleteByExpedienteId(UUID expedienteId);
}
