package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.service.DashboardService;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
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
    private final MateriaRepository materiaRepository;
    private final UsuarioService usuarioService;

    public DashboardController(DashboardService dashboardService,
            GerenciaRepository gerenciaRepository,
            MateriaRepository materiaRepository,
            UsuarioService usuarioService) {
        this.dashboardService = dashboardService;
        this.gerenciaRepository = gerenciaRepository;
        this.materiaRepository = materiaRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model,
            @RequestParam(value = "gerenciaId", required = false) Long gerenciaId,
            @RequestParam(value = "grupoGerencia", required = false) String grupoGerencia,
            @RequestParam(value = "materiaId", required = false) Integer materiaId) {
        // Obtenemos el nombre del usuario logueado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Buscamos el objeto Usuario completo
        Usuario usuario = usuarioService.obtenerPorUsername(username);

        // AQUÍ ESTÁ LA SOLUCIÓN: Agregamos ambas variables para que tanto el dashboard
        // como la navbar funcionen
        model.addAttribute("usuario", usuario);
        model.addAttribute("usuarioGlobal", usuario);

        if (usuario.getRol() == RolUsuario.ABOGADO) {
            // Load materias for the lawyer's gerencia
            List<?> materias = (usuario.getGerencia() != null)
                    ? materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(usuario.getGerencia().getId())
                    : java.util.Collections.emptyList();

            model.addAttribute("materias", materias);
            model.addAttribute("materiaSeleccionada", materiaId);
            model.addAttribute("kpis", dashboardService.obtenerKpisPersonales(usuario.getId(), materiaId));
            model.addAttribute("dashboardData", dashboardService.obtenerMetricasPersonales(usuario.getId(), materiaId));
            model.addAttribute("pageTitle", "Dashboard - Abogado");
            model.addAttribute("activePage", "dashboard");
            return "views/dashboard/index_abogado";
        }

        // Aplicamos la lógica de restricción y obtenemos filtros
        FiltroResult filtro = validarFiltrosPorRol(usuario, gerenciaId, grupoGerencia);

        model.addAttribute("gerencias", gerenciaRepository.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("kpis", dashboardService.obtenerKpisPorLista(filtro.gerenciaId(), filtro.gerenciaIds(), filtro.materiaIds()));
        model.addAttribute("dashboardData",
                dashboardService.obtenerMetricasPorLista(filtro.gerenciaId(), filtro.gerenciaIds(), filtro.materiaIds()));
        model.addAttribute("gerenciaSeleccionada", filtro.gerenciaId());
        model.addAttribute("grupoGerenciaSeleccionado", filtro.grupoGerencia());
        model.addAttribute("pageTitle", "Dashboard - Gerencia");
        model.addAttribute("activePage", "dashboard");

        return "views/dashboard/index";
    }

    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> obtenerDatosFiltrados(
            @RequestParam(value = "gerenciaId", required = false) Long gerenciaId,
            @RequestParam(value = "grupoGerencia", required = false) String grupoGerencia) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioService.obtenerPorUsername(auth.getName());

        FiltroResult filtro = validarFiltrosPorRol(usuario, gerenciaId, grupoGerencia);

        return Map.of(
                "kpis", dashboardService.obtenerKpisPorLista(filtro.gerenciaId(), filtro.gerenciaIds(), filtro.materiaIds()),
                "dashboardData", dashboardService.obtenerMetricasPorLista(filtro.gerenciaId(), filtro.gerenciaIds(), filtro.materiaIds()));
    }

    /**
     * Resultado de la validación de filtros
     */
    private record FiltroResult(Long gerenciaId, List<Long> gerenciaIds, String grupoGerencia, List<Integer> materiaIds) {
    }

    /**
     * Esta función centraliza la lógica de permisos para filtros
     */
    private FiltroResult validarFiltrosPorRol(Usuario usuario, Long gerenciaIdSolicitada,
            String grupoGerenciaSolicitado) {
        // Verifica que los nombres del ENUM coincidan
        boolean esDirectivo = usuario.getRol() == RolUsuario.DIRECCION ||
                usuario.getRol() == RolUsuario.SUBDIRECCION;

        if (esDirectivo) {
            // El director/subdirector puede usar filtros de grupo o gerencia individual
            if (grupoGerenciaSolicitado != null && !grupoGerenciaSolicitado.isEmpty()) {
                List<Long> gerenciaIds = dashboardService.obtenerListaGerencias(grupoGerenciaSolicitado);
                return new FiltroResult(null, gerenciaIds, grupoGerenciaSolicitado, null);
            }
            // Si el director tiene gerencia asignada, lo restringimos a esa (ROD-JERARQUIA)
            if (usuario.getGerencia() != null) {
                return new FiltroResult(usuario.getGerencia().getId().longValue(), null, null, null);
            }
            return new FiltroResult(gerenciaIdSolicitada, null, null, null);
        } else {
            // Si es Gerente o Jefe, obligatoriamente solo ve su gerencia
            Long gerenciaId = (usuario.getGerencia() != null) ? usuario.getGerencia().getId().longValue() : null;
            
            List<Integer> materiaIds = null;
            if (usuario.getRol() == RolUsuario.JEFE_DEPTO && usuario.getMaterias() != null && !usuario.getMaterias().isEmpty()) {
                materiaIds = usuario.getMaterias().stream().map(m -> m.getId()).collect(java.util.stream.Collectors.toList());
            }
            
            return new FiltroResult(gerenciaId, null, null, materiaIds);
        }
    }
}
