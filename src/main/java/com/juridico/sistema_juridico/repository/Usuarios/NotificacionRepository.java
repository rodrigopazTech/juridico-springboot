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
    
    // Método existente
    List<Notificacion> findByUsuarioIdAndLeidaFalse(Integer usuarioId);

    // Busca todas las notificaciones de un objeto Usuario y las ordena por fecha de creación (más nuevas primero)
    Page<Notificacion> findByUsuarioOrderByCreatedAtDesc(Usuario usuario, Pageable pageable);    
    
    long countByUsuarioAndLeidaFalse(Usuario usuario);
}