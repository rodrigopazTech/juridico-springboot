package com.juridico.sistema_juridico.repository.Usuarios;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    
    // Busca notificaciones NO LEÍDAS para el historial principal
    Page<Notificacion> findByUsuarioAndLeidaFalseOrderByCreatedAtDesc(Usuario usuario, Pageable pageable);

    // Método para la API de Toasts/Campana
    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(Integer usuarioId);

    // Busca notificaciones LEÍDAS para la papelera
    Page<Notificacion> findByUsuarioAndLeidaTrueOrderByCreatedAtDesc(Usuario usuario, Pageable pageable);

    // Vaciar papelera: Elimina todas las leídas del usuario
    void deleteByUsuarioAndLeidaTrue(Usuario usuario);

    long countByUsuarioAndLeidaFalse(Usuario usuario);
}