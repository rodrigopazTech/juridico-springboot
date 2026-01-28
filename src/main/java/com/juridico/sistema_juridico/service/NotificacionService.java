package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class NotificacionService {

    @Autowired private NotificacionRepository notificacionRepository;

    public void crearNotificacion(Usuario usuario, String titulo, String mensaje, String tipo, Prioridad prioridad, String entidadId) {
        if(usuario == null) return; // Evitar null pointer
        
        Notificacion notif = Notificacion.builder()
                .usuario(usuario)
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(tipo)
                .prioridad(prioridad)
                .entidadTipo(tipo)
                .entidadId(entidadId)
                .notificarEn(LocalDateTime.now())
                .leida(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificacionRepository.save(notif);
    }

    public Page<Notificacion> listarMisNotificaciones(Usuario usuario, Pageable pageable) {
        return notificacionRepository.findByUsuarioOrderByCreatedAtDesc(usuario, pageable);
    }

    public long contarNoLeidas(Usuario usuario) {
        return notificacionRepository.countByUsuarioAndLeidaFalse(usuario);
    }
}