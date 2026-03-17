package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionService notificacionService;

    // Este método inyecta "usuarioGlobal" en TODAS las vistas HTML automáticamente
    @ModelAttribute("usuarioGlobal")
    public Usuario getUsuarioGlobal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return usuarioRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    // Este método inyecta el número de "notificacionesNoLeidas" en TODAS las vistas
    @ModelAttribute("notificacionesNoLeidas")
    public long getNotificacionesNoLeidas() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Usuario usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
            if (usuario != null) {
                return notificacionService.contarNoLeidas(usuario);
            }
        }
        return 0;
    }
}