package com.juridico.sistema_juridico.repository.auditoria;

import com.juridico.sistema_juridico.Entity.auditoria.AuditoriaSesion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditoriaSesionRepository extends JpaRepository<AuditoriaSesion, Long> {
    Optional<AuditoriaSesion> findTopBySessionIdOrderByFechaInicioDesc(String sessionId);
    long countByEstado(String estado);
    long countByEstadoAndFechaUltimaActividadAfter(String estado, LocalDateTime fecha);
    long countByFechaInicioBetween(LocalDateTime inicio, LocalDateTime fin);
    long countByFechaCierreBetween(LocalDateTime inicio, LocalDateTime fin);
    List<AuditoriaSesion> findByEstadoAndFechaUltimaActividadBefore(String estado, LocalDateTime fecha);
    List<AuditoriaSesion> findByEstadoAndUsuarioIdAndSessionIdNot(String estado, Integer usuarioId, String sessionId);
    List<AuditoriaSesion> findByEstadoAndUsuarioId(String estado, Integer usuarioId);
    @Query("select s from AuditoriaSesion s left join s.usuario u where s.estado = :estado order by u.id asc, s.fechaUltimaActividad desc")
    List<AuditoriaSesion> findActivasOrdenadasParaNormalizar(@Param("estado") String estado);
    List<AuditoriaSesion> findTop12ByEstadoAndFechaUltimaActividadAfterOrderByFechaUltimaActividadDesc(String estado, LocalDateTime fecha);
    List<AuditoriaSesion> findTop12ByOrderByFechaUltimaActividadDesc();
    Page<AuditoriaSesion> findAllByOrderByFechaInicioDesc(Pageable pageable);
    Page<AuditoriaSesion> findByEstadoAndFechaUltimaActividadAfterOrderByFechaUltimaActividadDesc(String estado, LocalDateTime fecha, Pageable pageable);
}
