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

    // Este método se ejecuta antes de CUALQUIER vista
    @ModelAttribute("notificacionesNoLeidas")
    public long agregarDatosGlobales() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Si el usuario está logueado y no es anónimo
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return usuarioRepository.findByEmail(email)
                    .map(usuario -> notificacionService.contarNoLeidas(usuario))
                    .orElse(0L);
        }
        return 0L;
    }
}