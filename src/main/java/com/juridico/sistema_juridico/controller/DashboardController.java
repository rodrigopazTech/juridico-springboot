package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.service.DashboardService;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final GerenciaRepository gerenciaRepository;
    private final UsuarioService usuarioService;

    public DashboardController(DashboardService dashboardService, 
                               GerenciaRepository gerenciaRepository, 
                               UsuarioService usuarioService) {
        this.dashboardService = dashboardService;
        this.gerenciaRepository = gerenciaRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String dashboard(Model model, @RequestParam(required = false) Long gerenciaId) {
        // Obtenemos el nombre del usuario logueado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Buscamos el objeto Usuario completo
        Usuario usuario = usuarioService.obtenerPorUsername(username);
        
        // Aplicamos la lógica de restricción
        Long idAFiltrar = validarGerenciaPorRol(usuario, gerenciaId);

        model.addAttribute("usuario", usuario);
        model.addAttribute("gerencias", gerenciaRepository.findAll());
        model.addAttribute("kpis", dashboardService.obtenerKpis(idAFiltrar));
        model.addAttribute("dashboardData", dashboardService.obtenerMetricas(idAFiltrar));
        model.addAttribute("gerenciaSeleccionada", idAFiltrar);

        return "views/dashboard/index";
    }

    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> obtenerDatosFiltrados(@RequestParam(required = false) Long gerenciaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioService.obtenerPorUsername(auth.getName());
        
        Long idAFiltrar = validarGerenciaPorRol(usuario, gerenciaId);

        return Map.of(
            "kpis", dashboardService.obtenerKpis(idAFiltrar),
            "dashboardData", dashboardService.obtenerMetricas(idAFiltrar)
        );
    }

    /**
     * Esta función centraliza la lógica de permisos
     */
    private Long validarGerenciaPorRol(Usuario usuario, Long gerenciaIdSolicitada) {
        // Verifica si el usuario tiene rol de Dirección (puede ver todas las gerencias)
        boolean esDirectivo = usuario.getRol() == RolUsuario.DIRECCION || 
                              usuario.getRol() == RolUsuario.SUBDIRECCION;

        if (esDirectivo) {
            return gerenciaIdSolicitada; // El director puede ver todo (null) o una específica
        } else {
            // Si es Gerente o Jefe, obligatoriamente solo ve su gerencia
            // Convertimos el Integer del ID de gerencia a Long para el Service
            return (usuario.getGerencia() != null) ? usuario.getGerencia().getId().longValue() : null;
        }
    }
}