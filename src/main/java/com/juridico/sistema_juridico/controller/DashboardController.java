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

import java.util.List;
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
    public String dashboard(Model model, 
                           @RequestParam(required = false) Long gerenciaId,
                           @RequestParam(required = false) String grupoGerencia) {
        // Obtenemos el nombre del usuario logueado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Buscamos el objeto Usuario completo
        Usuario usuario = usuarioService.obtenerPorUsername(username);
        
        // Aplicamos la lógica de restricción y obtenemos filtros
        FiltroResult filtro = validarFiltrosPorRol(usuario, gerenciaId, grupoGerencia);

        model.addAttribute("usuario", usuario);
        model.addAttribute("gerencias", gerenciaRepository.findAll());
        model.addAttribute("kpis", dashboardService.obtenerKpisPorLista(filtro.gerenciaId(), filtro.gerenciaIds()));
        model.addAttribute("dashboardData", dashboardService.obtenerMetricasPorLista(filtro.gerenciaId(), filtro.gerenciaIds()));
        model.addAttribute("gerenciaSeleccionada", filtro.gerenciaId());
        model.addAttribute("grupoGerenciaSeleccionado", filtro.grupoGerencia());

        return "views/dashboard/index";
    }

    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> obtenerDatosFiltrados(
            @RequestParam(required = false) Long gerenciaId,
            @RequestParam(required = false) String grupoGerencia) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioService.obtenerPorUsername(auth.getName());
        
        FiltroResult filtro = validarFiltrosPorRol(usuario, gerenciaId, grupoGerencia);

        return Map.of(
            "kpis", dashboardService.obtenerKpisPorLista(filtro.gerenciaId(), filtro.gerenciaIds()),
            "dashboardData", dashboardService.obtenerMetricasPorLista(filtro.gerenciaId(), filtro.gerenciaIds())
        );
    }

    /**
     * Resultado de la validación de filtros
     */
    private record FiltroResult(Long gerenciaId, List<Long> gerenciaIds, String grupoGerencia) {}

    /**
     * Esta función centraliza la lógica de permisos para filtros
     */
    private FiltroResult validarFiltrosPorRol(Usuario usuario, Long gerenciaIdSolicitada, String grupoGerenciaSolicitado) {
        // Verifica que los nombres del ENUM coincidan
        boolean esDirectivo = usuario.getRol() == RolUsuario.DIRECTOR || 
                              usuario.getRol() == RolUsuario.DIRECCION ||
                              usuario.getRol() == RolUsuario.SUBDIRECTOR ||
                              usuario.getRol() == RolUsuario.SUBDIRECCION;

        if (esDirectivo) {
            // El director/subdirector puede usar filtros de grupo o gerencia individual
            if (grupoGerenciaSolicitado != null && !grupoGerenciaSolicitado.isEmpty()) {
                List<Long> gerenciaIds = dashboardService.obtenerListaGerencias(grupoGerenciaSolicitado);
                return new FiltroResult(null, gerenciaIds, grupoGerenciaSolicitado);
            }
            return new FiltroResult(gerenciaIdSolicitada, null, null);
        } else {
            // Si es Gerente o Jefe, obligatoriamente solo ve su gerencia
            Long gerenciaId = (usuario.getGerencia() != null) ? usuario.getGerencia().getId().longValue() : null;
            return new FiltroResult(gerenciaId, null, null);
        }
    }
}
