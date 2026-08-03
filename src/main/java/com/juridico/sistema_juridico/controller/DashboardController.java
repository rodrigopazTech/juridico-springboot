package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.service.DashboardService;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro;
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
    private final UsuarioRepository usuarioRepository;

    public DashboardController(DashboardService dashboardService,
            GerenciaRepository gerenciaRepository,
            MateriaRepository materiaRepository,
            UsuarioService usuarioService,
            UsuarioRepository usuarioRepository) {
        this.dashboardService = dashboardService;
        this.gerenciaRepository = gerenciaRepository;
        this.materiaRepository = materiaRepository;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model,
            @RequestParam(value = "gerenciaId", required = false) Long gerenciaId,
            @RequestParam(value = "grupoGerencia", required = false) String grupoGerencia,
            @RequestParam(value = "materiaId", required = false) Integer materiaId,
            @RequestParam(value = "abogadoId", required = false) Integer abogadoId,
            @RequestParam(value = "periodo", required = false) PeriodoFiltro periodo) {
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
            model.addAttribute("misAsuntos", dashboardService.obtenerResumenMisAsuntos(usuario, materiaId));
            model.addAttribute("hoyIso", java.time.LocalDate.now().toString());
            model.addAttribute("pageTitle", "Dashboard - Abogado");
            model.addAttribute("activePage", "dashboard");
            return "views/dashboard/index_abogado";
        }

        if (usuario.getRol() == RolUsuario.JEFE_DEPTO) {
            FiltroResult filtro = validarFiltrosPorRol(usuario, gerenciaId, grupoGerencia);

            model.addAttribute("kpis",
                    dashboardService.obtenerKpisPorLista(filtro.gerenciaId(), filtro.gerenciaIds(), filtro.materiaIds()));
            model.addAttribute("dashboardData",
                    dashboardService.obtenerMetricasPorLista(filtro.gerenciaId(), filtro.gerenciaIds(), filtro.materiaIds()));
            model.addAttribute("resumenDepartamento", dashboardService.obtenerResumenDepartamento(usuario));
            model.addAttribute("misAsuntos", dashboardService.obtenerResumenMisAsuntos(usuario, null));
            model.addAttribute("hoyIso", java.time.LocalDate.now().toString());
            model.addAttribute("gerenciaSeleccionada", filtro.gerenciaId());
            model.addAttribute("materiasSupervisadas", usuario.getMaterias());
            model.addAttribute("pageTitle", "Dashboard - Jefe de Departamento");
            model.addAttribute("activePage", "dashboard");
            return "views/dashboard/index_jefe_departamento";
        }

        // Aplicamos la lógica de restricción y obtenemos filtros
        if (usuario.getRol() == RolUsuario.GERENTE) {
            FiltroResult filtro = validarFiltrosPorRol(usuario, gerenciaId, grupoGerencia);
            List<Materia> materiasGerencia = usuario.getGerencia() != null
                    ? materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(usuario.getGerencia().getId())
                    : List.of();
            List<Usuario> abogadosGerencia = usuario.getGerencia() != null
                    ? usuarioRepository.findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(usuario.getGerencia().getId())
                            .stream()
                            .filter(u -> u.getRol() == RolUsuario.ABOGADO)
                            .toList()
                    : List.of();
            String materiaSeleccionadaNombre = materiasGerencia.stream()
                    .filter(m -> materiaId != null && m.getId().equals(materiaId))
                    .map(Materia::getNombre)
                    .findFirst()
                    .orElse(null);
            String abogadoSeleccionadoNombre = abogadosGerencia.stream()
                    .filter(a -> abogadoId != null && a.getId().equals(abogadoId))
                    .map(Usuario::getNombreCompleto)
                    .findFirst()
                    .orElse(null);

            model.addAttribute("kpis",
                    dashboardService.obtenerKpisPorLista(filtro.gerenciaId(), filtro.gerenciaIds(), filtro.materiaIds()));
            model.addAttribute("dashboardData",
                    dashboardService.obtenerMetricasPorLista(filtro.gerenciaId(), filtro.gerenciaIds(), filtro.materiaIds()));
            model.addAttribute("gerenteDashboard", dashboardService.obtenerDashboardGerente(usuario, materiaId, abogadoId, periodo));
            model.addAttribute("misAsuntos", dashboardService.obtenerResumenMisAsuntos(usuario, null));
            model.addAttribute("hoyIso", java.time.LocalDate.now().toString());
            model.addAttribute("gerenciaSeleccionada", filtro.gerenciaId());
            model.addAttribute("materiasGerencia", materiasGerencia);
            model.addAttribute("abogadosGerencia", abogadosGerencia);
            model.addAttribute("materiaSeleccionada", materiaId);
            model.addAttribute("abogadoSeleccionado", abogadoId);
            model.addAttribute("periodoSeleccionado", periodo != null ? periodo.name() : "SEMANA");
            model.addAttribute("materiaSeleccionadaNombre", materiaSeleccionadaNombre);
            model.addAttribute("abogadoSeleccionadoNombre", abogadoSeleccionadoNombre);
            model.addAttribute("pageTitle", "Dashboard - Gerente");
            model.addAttribute("activePage", "dashboard");
            return "views/dashboard/index_gerente";
        }

        if (usuario.getRol() == RolUsuario.SUBDIRECTOR) {
            List<Materia> materiasDirectivo = gerenciaId != null
                    ? materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(gerenciaId.intValue())
                    : materiaRepository.findByActivoTrueOrderByNombreAsc();
            List<Usuario> abogadosDirectivo = gerenciaId != null
                    ? usuarioRepository.findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(gerenciaId.intValue())
                            .stream()
                            .filter(u -> u.getRol() == RolUsuario.ABOGADO)
                            .toList()
                    : usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO);
            if (materiaId != null) {
                abogadosDirectivo = abogadosDirectivo.stream()
                        .filter(u -> u.tieneMateria(materiaId))
                        .toList();
            }

            String gerenciaSeleccionadaNombre = gerenciaId != null
                    ? gerenciaRepository.findById(gerenciaId.intValue()).map(g -> g.getNombre()).orElse(null)
                    : null;
            String materiaSeleccionadaNombre = materiasDirectivo.stream()
                    .filter(m -> materiaId != null && m.getId().equals(materiaId))
                    .map(Materia::getNombre)
                    .findFirst()
                    .orElse(null);
            String abogadoSeleccionadoNombre = abogadosDirectivo.stream()
                    .filter(a -> abogadoId != null && a.getId().equals(abogadoId))
                    .map(Usuario::getNombreCompleto)
                    .findFirst()
                    .orElse(null);

            model.addAttribute("subdirectorDashboard",
                    dashboardService.obtenerDashboardSubdirector(gerenciaId, materiaId, abogadoId, periodo));
            model.addAttribute("misAsuntos", dashboardService.obtenerResumenMisAsuntos(usuario, null));
            model.addAttribute("hoyIso", java.time.LocalDate.now().toString());
            model.addAttribute("gerencias", gerenciaRepository.findByActivoTrueOrderByNombreAsc());
            model.addAttribute("materiasDirectivo", materiasDirectivo);
            model.addAttribute("abogadosDirectivo", abogadosDirectivo);
            model.addAttribute("gerenciaSeleccionada", gerenciaId);
            model.addAttribute("materiaSeleccionada", materiaId);
            model.addAttribute("abogadoSeleccionado", abogadoId);
            model.addAttribute("periodoSeleccionado", periodo != null ? periodo.name() : "SEMANA");
            model.addAttribute("gerenciaSeleccionadaNombre", gerenciaSeleccionadaNombre);
            model.addAttribute("materiaSeleccionadaNombre", materiaSeleccionadaNombre);
            model.addAttribute("abogadoSeleccionadoNombre", abogadoSeleccionadoNombre);
            model.addAttribute("pageTitle", "Dashboard - Subdirector");
            model.addAttribute("activePage", "dashboard");
            return "views/dashboard/index_subdirector";
        }

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
        boolean esDirectivo = usuario.getRol() == RolUsuario.DIRECTOR ||
                usuario.getRol() == RolUsuario.DIRECTOR_PRINCIPAL ||
                usuario.getRol() == RolUsuario.SUBDIRECTOR ||
                usuario.getRol() == RolUsuario.ADMIN;

        if (esDirectivo) {
            // El director/subdirector puede usar filtros de grupo o gerencia individual
            if (grupoGerenciaSolicitado != null && !grupoGerenciaSolicitado.isEmpty()) {
                List<Long> gerenciaIds = dashboardService.obtenerListaGerencias(grupoGerenciaSolicitado);
                return new FiltroResult(null, gerenciaIds, grupoGerenciaSolicitado, null);
            }
            // Los directivos no se restringen y pueden ver la gerencia solicitada (o todas si es null)
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
