package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/calendario")
public class CalendarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping
    public String index(Model model) {
        // 1. Definir página activa para que el sidebar resalte el botón de Calendario
        model.addAttribute("activePage", "calendario");

        // 2. Obtener el usuario autenticado del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            try {
                /* * Ajuste según tu UsuarioService.java:
                 * El método se llama obtenerPorUsername(String email)
                 */
                Usuario usuario = usuarioService.obtenerPorUsername(auth.getName());
                model.addAttribute("usuarioGlobal", usuario);
                
                /* * Ajuste según tu NotificacionService.java:
                 * El método se llama contarNoLeidas(Usuario usuario)
                 */
                long noLeidas = notificacionService.contarNoLeidas(usuario);
                model.addAttribute("notificacionesNoLeidas", noLeidas);
                
            } catch (Exception e) {
                // Si el usuario no se encuentra o hay error, inicializamos valores por defecto
                model.addAttribute("notificacionesNoLeidas", 0);
                System.err.println("Error al cargar datos globales en CalendarioController: " + e.getMessage());
            }
        }

        // 3. Retorna la vista ubicada en templates/views/calendario/index.html
        return "views/calendario/index";
    }
}