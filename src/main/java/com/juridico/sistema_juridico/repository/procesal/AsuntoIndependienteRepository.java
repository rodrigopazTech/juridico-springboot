package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.enums.EstadoAsuntoIndependiente;
import com.juridico.sistema_juridico.Entity.procesal.AsuntoIndependiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AsuntoIndependienteRepository extends JpaRepository<AsuntoIndependiente, Long> {
    Optional<AsuntoIndependiente> findByFolioSiaj(String folioSiaj);

    Optional<AsuntoIndependiente> findFirstByGerenciaIdAndReferenciaIgnoreCase(Integer gerenciaId, String referencia);

    List<AsuntoIndependiente> findByEstadoOrderByCreatedAtDesc(EstadoAsuntoIndependiente estado);

    @Query("SELECT COUNT(a) FROM AsuntoIndependiente a WHERE a.folioSiaj LIKE CONCAT('AI-', :anio, '-%')")
    long contarPorAnio(@Param("anio") int anio);
}
