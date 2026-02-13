package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
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

    @Autowired
    private GerenciaRepository gerenciaRepository;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "calendario");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Valores por defecto
        String userRole = "ANONYMOUS";
        boolean puedeVerFiltroGerencia = false;
        List<Gerencia> listaGerencias = new ArrayList<>();
        boolean puedeVerFiltroUsuario = false;
        List<Usuario> listaUsuarios = new ArrayList<>();
        boolean puedeVerFiltroMateria = false;
        List<String> listaMaterias = Arrays.asList(
            "Civil", "Mercantil", "Fiscal", "Administrativo", 
            "Laboral", "Penal", "Amparo", "Transparencia"
        );

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Usuario usuarioActual = usuarioRepository.findByEmail(auth.getName()).orElse(null);
            
            if (usuarioActual != null) {
                model.addAttribute("usuarioGlobal", usuarioActual);
                model.addAttribute("notificacionesNoLeidas", notificacionService.contarNoLeidas(usuarioActual));
                
                userRole = usuarioActual.getRol().name();

                // Lógica de Permisos para Gerencias
                if (usuarioActual.getRol() == RolUsuario.DIRECCION || usuarioActual.getRol() == RolUsuario.SUBDIRECCION) {
                    puedeVerFiltroGerencia = true;
                    // Cargar gerencias desde la base de datos (solo activas)
                    listaGerencias = gerenciaRepository.findByActivoTrueOrderByNombreAsc();
                    
                    // Filtro de usuario visible para DIRECCION y SUBDIRECCION
                    puedeVerFiltroUsuario = true;
                    listaUsuarios = usuarioRepository.findAll();
                    
                } else if (usuarioActual.getRol() == RolUsuario.GERENTE) {
                    // GERENTE no ve el filtro de gerencia (solo ve su propia gerencia)
                    puedeVerFiltroGerencia = false;
                    if (usuarioActual.getGerencia() != null) {
                        listaGerencias.add(usuarioActual.getGerencia());
                    }
                    
                    // Filtro de usuario visible para GERENTE (solo usuarios de su gerencia)
                    puedeVerFiltroUsuario = true;
                    listaUsuarios = usuarioRepository.findByGerenciaId(
                        usuarioActual.getGerencia().getId().longValue());
                    
                } else if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
                    // ABOGADO no ve el filtro de usuario
                    puedeVerFiltroUsuario = false;
                    listaUsuarios = new ArrayList<>();
                }
                
                // GERENTE, JEFE_DEPTO pueden ver el filtro de materia
                if (usuarioActual.getRol() == RolUsuario.GERENTE || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
                    puedeVerFiltroMateria = true;
                }
                
                // GERENTE y JEFE_DEPTO pueden ver el filtro de usuario
                if (usuarioActual.getRol() == RolUsuario.GERENTE || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
                    puedeVerFiltroUsuario = true;
                    // JEFE_DEPTO ve todos los usuarios (o según la lógica de negocio)
                    if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
                        listaUsuarios = usuarioRepository.findAll();
                    }
                }
            }
        }

        // Pasamos todo al modelo con nombres claros
        model.addAttribute("userRole", userRole);
        model.addAttribute("puedeVerFiltroGerencia", puedeVerFiltroGerencia);
        model.addAttribute("listaGerencias", listaGerencias);
        model.addAttribute("puedeVerFiltroUsuario", puedeVerFiltroUsuario);
        model.addAttribute("listaUsuarios", listaUsuarios);
        model.addAttribute("puedeVerFiltroMateria", puedeVerFiltroMateria);
        model.addAttribute("listaMaterias", listaMaterias);

        return "views/calendario/index";
    }
}
