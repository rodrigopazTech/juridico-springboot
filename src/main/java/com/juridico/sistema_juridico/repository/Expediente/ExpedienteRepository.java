package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, UUID> {

    // Buscar por número exacto (ignorando mayúsculas/minúsculas)
    Optional<Expediente> findByNumeroIgnoreCase(String numero);

    // Filtrar por abogado responsable
    Page<Expediente> findByAbogadoResponsableId(Integer abogadoId, Pageable pageable);

    @Query("SELECT e FROM Expediente e WHERE " +
           "(LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND e.gerencia.id = :gerenciaId")
    Page<Expediente> buscarPorPalabraClaveYGerencia(
            @Param("keyword") String keyword, 
            @Param("gerenciaId") Integer gerenciaId, 
            Pageable pageable);
}