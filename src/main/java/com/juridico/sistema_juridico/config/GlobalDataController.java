package com.juridico.sistema_juridico.config;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalDataController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private NotificacionService notificacionService;

    // 1. INYECTAR USUARIO ACTUAL (Para Nombre, Rol e Inicial en Sidebar)
    @ModelAttribute("usuarioGlobal")
    public Usuario agregarUsuarioGlobal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return usuarioRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    // 2. INYECTAR CONTADOR DE NOTIFICACIONES (Para la burbuja roja)
    @ModelAttribute("notificacionesNoLeidas")
    public long agregarConteoNotificaciones() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return usuarioRepository.findByEmail(auth.getName())
                    .map(usuario -> notificacionService.contarNoLeidas(usuario))
                    .orElse(0L);
        }
        return 0L;
    }
}