package com.juridico.sistema_juridico.repository.Usuarios;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario; 
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    
    boolean existsByEventoAndEntidadId(NotificacionEvento evento, String entidadId);
    
    boolean existsByUsuarioAndEventoAndEntidadId(Usuario usuario, NotificacionEvento evento, String entidadId);
    
    boolean existsByUsuarioAndEventoAndEntidadIdAndTitulo(Usuario usuario, NotificacionEvento evento, String entidadId, String titulo);
    
    // Busca notificaciones NO LEÍDAS para el historial principal
    Page<Notificacion> findByUsuarioAndLeidaFalseOrderByCreatedAtDesc(Usuario usuario, Pageable pageable);

    // Busca todas las notificaciones de un usuario ordenadas por fecha
    Page<Notificacion> findByUsuarioOrderByCreatedAtDesc(Usuario usuario, Pageable pageable);

    // Método para la API de Toasts/Campana
    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(Integer usuarioId);

    /** Notificaciones de tareas del usuario, incluidas las que ya fueron leídas. */
    List<Notificacion> findByUsuarioAndEventoOrderByNotificarEnDesc(
            Usuario usuario, NotificacionEvento evento);

    List<Notificacion> findByUsuarioAndEventoAndLeidaFalseAndEntidadId(
            Usuario usuario, NotificacionEvento evento, String entidadId);

    List<Notificacion> findByUsuarioAndEventoAndLeidaFalseAndEntidadIdStartingWith(
            Usuario usuario, NotificacionEvento evento, String entidadIdPrefix);

    // Busca notificaciones LEÍDAS para la papelera
    Page<Notificacion> findByUsuarioAndLeidaTrueOrderByCreatedAtDesc(Usuario usuario, Pageable pageable);

    // Vaciar papelera: Elimina todas las leídas del usuario
    void deleteByUsuarioAndLeidaTrue(Usuario usuario);

    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Integer usuarioId);

    long countByUsuarioAndLeidaFalse(Usuario usuario);
}
