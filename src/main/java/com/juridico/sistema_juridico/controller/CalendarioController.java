package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/calendario")
public class CalendarioController {

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "calendario");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Valores por defecto
        String userRole = "ANONYMOUS";
        boolean puedeVerFiltroGerencia = false;
        List<String> listaGerencias = new ArrayList<>();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Usuario usuarioActual = usuarioRepository.findByEmail(auth.getName()).orElse(null);
            
            if (usuarioActual != null) {
                model.addAttribute("usuarioGlobal", usuarioActual);
                model.addAttribute("notificacionesNoLeidas", notificacionService.contarNoLeidas(usuarioActual));
                
                userRole = usuarioActual.getRol().name();

                // Lógica de Permisos para Gerencias
                if (usuarioActual.getRol() == RolUsuario.DIRECCION || usuarioActual.getRol() == RolUsuario.SUBDIRECCION) {
                    puedeVerFiltroGerencia = true;
                    listaGerencias = Arrays.asList(
                        "Gerencia Civil, Mercantil, Fiscal y Administrativo",
                        "Gerencia Laboral y Penal",
                        "Gerencia Transparencia y Amparo"
                    );
                } else if (usuarioActual.getRol() == RolUsuario.GERENTE) {
                    puedeVerFiltroGerencia = true;
                    if (usuarioActual.getGerencia() != null) {
                        listaGerencias.add(usuarioActual.getGerencia().getNombre());
                    }
                }
            }
        }

        // Pasamos todo al modelo con nombres claros
        model.addAttribute("userRole", userRole);
        model.addAttribute("puedeVerFiltroGerencia", puedeVerFiltroGerencia);
        model.addAttribute("listaGerencias", listaGerencias);

        return "views/calendario/index";
    }
}