package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.AgendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository usuarioRepository;

    @GetMapping
    public String index(Model model,
            @RequestParam(defaultValue = "hoy") String filtro,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) Integer anio,
            @RequestParam(defaultValue = "audiencias") String tab,
            @RequestParam(defaultValue = "0") int page) { // Nuevo Param

        // 0. Seguridad RBAC
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        com.juridico.sistema_juridico.Entity.usuario.Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow();

        Integer gerenciaId = null;
        java.util.List<Integer> materiaIds = null;

        if (usuario.getRol() == com.juridico.sistema_juridico.Entity.enums.RolUsuario.DIRECCION ||
                usuario.getRol() == com.juridico.sistema_juridico.Entity.enums.RolUsuario.SUBDIRECCION) {
            // Ven todo (null filters)
        } else if (usuario.getRol() == com.juridico.sistema_juridico.Entity.enums.RolUsuario.GERENTE) {
            if (usuario.getGerencia() != null) {
                gerenciaId = usuario.getGerencia().getId();
            }
        } else {
            // JEFE_DEPTO, ABOGADO, etc -> Ven por materias
            if (usuario.getMaterias() != null && !usuario.getMaterias().isEmpty()) {
                materiaIds = usuario.getMaterias().stream().map(m -> m.getId())
                        .collect(java.util.stream.Collectors.toList());
            }
        }

        // ROD-45: Para Abogados, si no tienen materias, debemos filtrar por su ID
        Integer usuarioId = null;
        if (usuario.getRol() == com.juridico.sistema_juridico.Entity.enums.RolUsuario.ABOGADO) {
            usuarioId = usuario.getId();
        }

        int anioReal = java.time.LocalDate.now().getYear();
        int anioSeleccionado = (anio != null) ? anio : anioReal;

        // 2. Obtener Datos PAGINADOS
        // Nota: Usamos la misma variable 'page' para ambas listas.
        // Si el usuario cambia de pestaña, volverá a la pagina que indique la URL o 0.
        // MODIFICADO: Pasamos los filtros de seguridad
        com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro periodoEnum;
        try {
            periodoEnum = com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro.valueOf(filtro.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            periodoEnum = com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro.HOY;
        }

        var audiencias = agendaService.getAudienciasPorFiltro(periodoEnum, mes, anioSeleccionado, page, gerenciaId,
                materiaIds, usuarioId);
        var terminos = agendaService.getTerminosPorFiltro(periodoEnum, mes, anioSeleccionado, page, gerenciaId,
                materiaIds, usuarioId);

        // 3. Cargar Modelo
        model.addAttribute("audiencias", audiencias);
        model.addAttribute("terminos", terminos);

        // 4. UI y Filtros
        model.addAttribute("pageTitle", "Agenda General");
        model.addAttribute("activePage", "agenda");
        model.addAttribute("activeTab", tab);
        model.addAttribute("filtroActual", filtro);
        model.addAttribute("mesActual", mes);
        model.addAttribute("anioActual", anioSeleccionado);

        model.addAttribute("listaAnios", java.util.stream.IntStream
                .rangeClosed(anioReal - 5, anioReal)
                .boxed()
                .sorted(java.util.Collections.reverseOrder())
                .toList());

        return "views/agenda/index";
    }
}