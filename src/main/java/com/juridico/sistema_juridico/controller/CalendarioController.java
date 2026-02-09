package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String index(Model model) {
        // 1. Definir página activa para que el sidebar resalte el botón de Calendario
        model.addAttribute("activePage", "calendario");

        // 2. Obtener el usuario autenticado del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioActual = null;
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            try {
                usuarioActual = usuarioService.obtenerPorUsername(auth.getName());
                model.addAttribute("usuarioGlobal", usuarioActual);
                
                long noLeidas = notificacionService.contarNoLeidas(usuarioActual);
                model.addAttribute("notificacionesNoLeidas", noLeidas);
                
                // 3. Pasar información de permisos al modelo
                model.addAttribute("userRole", usuarioActual.getRol().name());
                model.addAttribute("userGerenciaId", usuarioActual.getGerencia() != null ? 
                    usuarioActual.getGerencia().getId() : null);
                
                // Determinar qué filtros son visibles
                boolean puedeVerFiltroGerencia = puedeVerFiltroGerencia(usuarioActual.getRol());
                boolean puedeVerFiltroUsuario = puedeVerFiltroUsuario(usuarioActual.getRol());
                
                model.addAttribute("puedeVerFiltroGerencia", puedeVerFiltroGerencia);
                model.addAttribute("puedeVerFiltroUsuario", puedeVerFiltroUsuario);
                
            } catch (Exception e) {
                model.addAttribute("notificacionesNoLeidas", 0);
                model.addAttribute("userRole", "ANONYMOUS");
                System.err.println("Error al cargar datos globales en CalendarioController: " + e.getMessage());
            }
        }

        // 4. Retorna la vista ubicada en templates/views/calendario/index.html
        return "views/calendario/index";
    }

    /**
     * Determina si el rol puede ver el filtro de gerencia
     * Roles con permiso: DIRECCION, SUBDIRECCION
     * Roles SIN permiso: GERENTE (solo ve su gerencia), ABOGADO (no ve filtro)
     */
    private boolean puedeVerFiltroGerencia(RolUsuario rol) {
        return rol == RolUsuario.DIRECCION || rol == RolUsuario.SUBDIRECCION;
    }

    /**
     * Determina si el rol puede ver el filtro de usuario
     * Roles con permiso: DIRECCION, SUBDIRECCION, GERENTE
     * Roles SIN permiso: ABOGADO
     */
    private boolean puedeVerFiltroUsuario(RolUsuario rol) {
        return rol == RolUsuario.DIRECCION || 
               rol == RolUsuario.SUBDIRECCION || 
               rol == RolUsuario.GERENTE;
    }
}

