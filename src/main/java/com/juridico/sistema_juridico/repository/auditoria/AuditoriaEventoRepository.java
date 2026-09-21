package com.juridico.sistema_juridico.repository.auditoria;

import com.juridico.sistema_juridico.Entity.auditoria.AuditoriaEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {
    long countByFechaBetween(LocalDateTime inicio, LocalDateTime fin);
    long countByResultadoAndFechaBetween(String resultado, LocalDateTime inicio, LocalDateTime fin);
    long countByAccionAndFechaBetween(String accion, LocalDateTime inicio, LocalDateTime fin);
    List<AuditoriaEvento> findTop20ByOrderByFechaDesc();
    Page<AuditoriaEvento> findAllByOrderByFechaDesc(Pageable pageable);
    Page<AuditoriaEvento> findByResultadoInOrderByFechaDesc(Collection<String> resultados, Pageable pageable);
    boolean existsByAccionAndEntidadIdAndFechaAfter(String accion, String entidadId, LocalDateTime fecha);

    @Query("""
            select e from AuditoriaEvento e
            where e.ruta is null or e.ruta not in :rutasExcluidas
            order by e.fecha desc
            """)
    List<AuditoriaEvento> findTop20Operativos(@Param("rutasExcluidas") Collection<String> rutasExcluidas, Pageable pageable);

    @Query("""
            select e from AuditoriaEvento e
            where e.ruta is null or e.ruta not in :rutasExcluidas
            order by e.fecha desc
            """)
    Page<AuditoriaEvento> findOperativos(@Param("rutasExcluidas") Collection<String> rutasExcluidas, Pageable pageable);
}
