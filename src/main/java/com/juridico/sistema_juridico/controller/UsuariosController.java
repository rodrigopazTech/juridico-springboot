package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.PermisoSistema;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.dto.usuarios.PermisoCategoriaView;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.service.PermisoUsuarioService;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

@Controller
@RequestMapping("/usuarios")
public class UsuariosController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PermisoUsuarioService permisoUsuarioService;

    @GetMapping
    public String index(Model model,
            @RequestParam(value = "pageUsuarios", defaultValue = "0") int pageUsuarios,
            @RequestParam(value = "pageGerencias", defaultValue = "0") int pageGerencias,
            @RequestParam(value = "pageOrganos", defaultValue = "0") int pageOrganos,
            @RequestParam(value = "pageTiposAudiencia", defaultValue = "0") int pageTiposAudiencia) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioActual = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado en la base de datos."));

        // Roles con acceso total (ven todos los usuarios sin filtro por gerencia)
        Set<RolUsuario> rolesConAccesoTotal = Set.of(
                RolUsuario.ADMIN, RolUsuario.DIRECTOR_PRINCIPAL,
                RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR);

        // Paginación Usuarios
        Pageable pageUs = PageRequest.of(pageUsuarios, 10, Sort.by("nombreCompleto").ascending());
        Page<Usuario> usuariosPage;
        List<RolUsuario> rolesVisibles = usuarioService.getRolesVisiblesPara(usuarioActual.getRol());
        if (!rolesConAccesoTotal.contains(usuarioActual.getRol())) {
            List<Integer> gerIds = usuarioActual.getGerencias().stream()
                    .map(Gerencia::getId).collect(Collectors.toList());
            if (usuarioActual.getRol() == RolUsuario.GERENTE) {
                if (gerIds.isEmpty()) {
                    usuariosPage = Page.empty(pageUs);
                } else {
                    usuariosPage = usuarioRepository.findDistinctByGerenciasIdInAndRolInAndIdNot(gerIds, rolesVisibles, usuarioActual.getId(), pageUs);
                }
            } else {
                List<Integer> matIds = usuarioActual.getMaterias().stream()
                        .map(Materia::getId).collect(Collectors.toList());
                if (gerIds.isEmpty() || matIds.isEmpty()) {
                    usuariosPage = Page.empty(pageUs);
                } else {
                    usuariosPage = usuarioRepository.findDistinctByGerenciasIdInAndMateriasIdInAndRolInAndIdNot(gerIds, matIds, rolesVisibles, usuarioActual.getId(), pageUs);
                }
            }
        } else {
            usuariosPage = usuarioRepository.findByRolInAndIdNot(rolesVisibles, usuarioActual.getId(), pageUs);
        }

        // Paginación Gerencias
        Page<Gerencia> gerenciasPage = usuarioService
                .listarGerenciasPaginadas(PageRequest.of(pageGerencias, 10, Sort.by("nombre").ascending()));

        // Paginación Órganos Jurisdiccionales
        Page<OrganoJurisdiccional> organosPage = usuarioService
                .listarOrganosPaginados(PageRequest.of(pageOrganos, 10, Sort.by("nombre").ascending()));

        // Paginación Tipos de Audiencia
        Page<TipoAudiencia> tiposAudienciaPage = usuarioService
                .listarTiposAudienciaPaginados(PageRequest.of(pageTiposAudiencia, 10, Sort.by("nombre").ascending()));

        model.addAttribute("listaUsuarios", usuariosPage);
        model.addAttribute("listaGerencias", gerenciasPage);
        model.addAttribute("listaOrganos", organosPage);
        model.addAttribute("listaTiposAudiencia", tiposAudienciaPage);

        List<RolUsuario> rolesPermitidos = new ArrayList<>();
        if (usuarioActual.getRol() == RolUsuario.ADMIN) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.values()));
        } else if (usuarioActual.getRol() == RolUsuario.DIRECTOR_PRINCIPAL) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO));
        } else if (usuarioActual.getRol() == RolUsuario.DIRECTOR) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.SUBDIRECTOR, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO));
        } else if (usuarioActual.getRol() == RolUsuario.SUBDIRECTOR) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO));
        } else if (usuarioActual.getRol() == RolUsuario.GERENTE) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO));
        } else if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            rolesPermitidos.add(RolUsuario.ABOGADO);
        }
        model.addAttribute("listaRoles", rolesPermitidos);

        if (!rolesConAccesoTotal.contains(usuarioActual.getRol()) && !usuarioActual.getGerencias().isEmpty()) {
            model.addAttribute("allGerencias", new ArrayList<>(usuarioActual.getGerencias()));
        } else {
            model.addAttribute("allGerencias", usuarioService.listarGerencias());
        }

        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("nuevoUsuario", new Usuario());
        model.addAttribute("nuevaGerencia", new Gerencia());
        model.addAttribute("nuevoOrgano", new OrganoJurisdiccional());
        model.addAttribute("nuevoTipoAudiencia", new TipoAudiencia());
        model.addAttribute("pageTitle", "Usuarios");
        model.addAttribute("activePage", "usuarios");

        return "views/usuarios/index";
    }

    @GetMapping("/permisos")
    public String panelPermisos(
            @RequestParam(value = "usuarioId", required = false) Integer usuarioId,
            Model model) {

        Usuario actor = obtenerUsuarioActual();
        List<Usuario> usuariosVisibles = listarUsuariosVisibles(actor);

        Usuario usuarioSeleccionado = null;
        if (!usuariosVisibles.isEmpty()) {
            usuarioSeleccionado = usuariosVisibles.stream()
                    .filter(usuario -> usuarioId != null && usuario.getId().equals(usuarioId))
                    .findFirst()
                    .orElse(usuariosVisibles.get(0));
        }

        List<PermisoCategoriaView> permisosVista = usuarioSeleccionado != null
                ? permisoUsuarioService.construirVistaPermisos(actor, usuarioSeleccionado)
                : List.of();

        model.addAttribute("pageTitle", "Panel de Permisos");
        model.addAttribute("activePage", "usuarios");
        model.addAttribute("usuarioActual", actor);
        model.addAttribute("usuariosVisibles", usuariosVisibles);
        model.addAttribute("usuarioSeleccionado", usuarioSeleccionado);
        model.addAttribute("permisosVista", permisosVista);
        model.addAttribute("permisosBaseSeleccionado", usuarioSeleccionado != null
                ? permisoUsuarioService.obtenerPermisosBase(usuarioSeleccionado.getRol()).size()
                : 0);
        model.addAttribute("permisosExtrasSeleccionado", usuarioSeleccionado != null
                ? usuarioSeleccionado.getPermisosAdicionales().size()
                : 0);
        model.addAttribute("permisosEfectivosSeleccionado", usuarioSeleccionado != null
                ? permisoUsuarioService.obtenerPermisosEfectivos(usuarioSeleccionado).size()
                : 0);

        return "views/usuarios/permisos";
    }

    @PostMapping("/permisos/guardar")
    public String guardarPermisosUsuario(
            @RequestParam("usuarioId") Integer usuarioId,
            @RequestParam(value = "permisosAdicionales", required = false) List<PermisoSistema> permisosAdicionales,
            RedirectAttributes redirectAttrs) {

        Usuario actor = obtenerUsuarioActual();
        Usuario objetivo = usuarioService.buscarPorId(usuarioId);

        if (!esUsuarioVisibleParaActor(actor, objetivo)) {
            throw new ResourceNotFoundException("No tienes permisos para administrar este usuario.");
        }

        permisoUsuarioService.guardarPermisosAdicionales(usuarioId, permisosAdicionales, actor);

        redirectAttrs.addFlashAttribute("mensaje", "Permisos actualizados correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios/permisos?usuarioId=" + usuarioId;
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam(value = "gerenciasIds", required = false) java.util.List<Integer> gerenciasIds,
            @RequestParam(value = "materiasIds", required = false) java.util.List<Integer> materiasIds,
            RedirectAttributes redirectAttrs) {
        usuarioService.guardarUsuario(usuario, gerenciasIds, materiasIds);
        redirectAttrs.addFlashAttribute("mensaje", "Usuario guardado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios";
    }

    @PostMapping("/gerencias/guardar")
    public String guardarGerencia(@ModelAttribute Gerencia gerencia, RedirectAttributes redirectAttrs) {
        try {
            usuarioService.guardarGerencia(gerencia);
            redirectAttrs.addFlashAttribute("mensaje", "Gerencia procesada correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (IllegalArgumentException ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/usuarios?tab=gerencias";
    }

    @PostMapping("/organos/guardar")
    public String guardarOrgano(@ModelAttribute OrganoJurisdiccional organo, RedirectAttributes redirectAttrs) {
        usuarioService.guardarOrgano(organo);
        redirectAttrs.addFlashAttribute("mensaje", "Órgano Jurisdiccional procesado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios?tab=organos";
    }

    @PostMapping("/tipos-audiencia/guardar")
    public String guardarTipoAudiencia(@ModelAttribute TipoAudiencia tipo, RedirectAttributes redirectAttrs) {
        usuarioService.guardarTipoAudiencia(tipo);
        redirectAttrs.addFlashAttribute("mensaje", "Tipo de Audiencia procesado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios?tab=tipos-audiencia";
    }

    // --- ACCIONES DE ESTADO ---

    @GetMapping("/toggle/{id}")
    public String toggleStatusUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioService.buscarPorId(id);
        usuario.setActivo(!usuario.getActivo());
        List<Integer> gerenciasIds = usuario.getGerencias().stream().map(Gerencia::getId).collect(Collectors.toList());
        List<Integer> materiasIds = usuario.getMaterias().stream().map(Materia::getId).collect(Collectors.toList());
        usuarioService.guardarUsuario(usuario, gerenciasIds, materiasIds);
        String estado = usuario.getActivo() ? "activado" : "desactivado";
        redirectAttrs.addFlashAttribute("mensaje", "Usuario " + estado + " correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios";
    }

    @GetMapping("/gerencias/toggle/{id}")
    public String toggleStatusGerencia(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Gerencia g = usuarioService.buscarGerenciaPorId(id);
        g.setActivo(!g.getActivo());
        usuarioService.guardarGerencia(g);
        redirectAttrs.addFlashAttribute("mensaje", "Estatus de gerencia actualizado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios?tab=gerencias";
    }

    @GetMapping("/organos/toggle/{id}")
    public String toggleStatusOrgano(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        OrganoJurisdiccional o = usuarioService.buscarOrganoPorId(id);
        o.setActivo(!o.getActivo());
        usuarioService.guardarOrgano(o);
        redirectAttrs.addFlashAttribute("mensaje", "Estatus del órgano actualizado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios?tab=organos";
    }

    @GetMapping("/tipos-audiencia/toggle/{id}")
    public String toggleStatusTipoAudiencia(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        TipoAudiencia t = usuarioService.buscarTipoAudienciaPorId(id);
        t.setActivo(!t.getActivo());
        usuarioService.guardarTipoAudiencia(t);
        redirectAttrs.addFlashAttribute("mensaje", "Estatus del tipo de audiencia actualizado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios?tab=tipos-audiencia";
    }

    // --- ELIMINACIÓN ---

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        usuarioService.eliminarUsuario(id);
        redirectAttrs.addFlashAttribute("mensaje", "Usuario eliminado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios";
    }

    @GetMapping("/organos/eliminar/{id}")
    public String eliminarOrgano(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        usuarioService.eliminarOrgano(id);
        redirectAttrs.addFlashAttribute("mensaje", "Órgano eliminado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios?tab=organos";
    }

    @GetMapping("/tipos-audiencia/eliminar/{id}")
    public String eliminarTipoAudiencia(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        usuarioService.eliminarTipoAudiencia(id);
        redirectAttrs.addFlashAttribute("mensaje", "Tipo de audiencia eliminado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios?tab=tipos-audiencia";
    }

    // --- API JSON ---

    @GetMapping("/api/materias-por-gerencia/{ids}")
    @ResponseBody
    public ResponseEntity<List<Materia>> getMateriasJson(@PathVariable("ids") List<Integer> ids) {
        try {
            Set<Materia> materiasCombinadas = new HashSet<>();
            for (Integer id : ids) {
                Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
                materiasCombinadas.addAll(gerencia.getMaterias());
            }
            return ResponseEntity.ok(new ArrayList<>(materiasCombinadas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/gerencias/{id}/materias")
    public String obtenerMateriasGerencia(@PathVariable("id") Integer id, Model model) {
        Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
        model.addAttribute("materias", gerencia.getMaterias());
        return "fragments/usuarios/modal-materias :: lista";
    }

    @PostMapping("/gerencias/materias/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarMateria(@RequestBody Materia materia) {
        try {
            usuarioService.guardarMateria(materia);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al guardar materia");
        }
    }

    private Usuario obtenerUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado en la base de datos."));
    }

    private List<Usuario> listarUsuariosVisibles(Usuario actor) {
        return usuarioService.listarUsuarios().stream()
                .filter(usuario -> esUsuarioVisibleParaActor(actor, usuario))
                .sorted(Comparator.comparing(Usuario::getNombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean esUsuarioVisibleParaActor(Usuario actor, Usuario usuarioObjetivo) {
        if (actor.getId().equals(usuarioObjetivo.getId())) {
            return false;
        }
        List<RolUsuario> rolesVisibles = usuarioService.getRolesVisiblesPara(actor.getRol());
        if (!rolesVisibles.contains(usuarioObjetivo.getRol())) {
            return false;
        }

        RolUsuario rol = actor.getRol();
        // Roles directivos tienen visibilidad total
        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR) {
            return true;
        }

        // GERENTE: solo ve usuarios con gerencia en común
        if (rol == RolUsuario.GERENTE) {
            boolean tieneGerenciaEnComun = false;
            if (actor.getGerencias() != null && !actor.getGerencias().isEmpty() && usuarioObjetivo.getGerencias() != null) {
                tieneGerenciaEnComun = actor.getGerencias().stream().anyMatch(g ->
                    usuarioObjetivo.getGerencias().stream().anyMatch(go -> go.getId().equals(g.getId()))
                );
            }
            return tieneGerenciaEnComun;
        }

        // JEFE_DEPTO: solo ve usuarios con gerencias en común Y materias en común
        if (rol == RolUsuario.JEFE_DEPTO) {
            boolean tieneGerenciaEnComun = false;
            if (actor.getGerencias() != null && !actor.getGerencias().isEmpty() && usuarioObjetivo.getGerencias() != null) {
                tieneGerenciaEnComun = actor.getGerencias().stream().anyMatch(g ->
                    usuarioObjetivo.getGerencias().stream().anyMatch(go -> go.getId().equals(g.getId()))
                );
            }

            boolean tieneMateriaEnComun = false;
            if (actor.getMaterias() != null && !actor.getMaterias().isEmpty() && usuarioObjetivo.getMaterias() != null) {
                tieneMateriaEnComun = actor.getMaterias().stream().anyMatch(m ->
                    usuarioObjetivo.getMaterias().stream().anyMatch(mo -> mo.getId().equals(m.getId()))
                );
            }

            return tieneGerenciaEnComun && tieneMateriaEnComun;
        }

        return false;
    }
}
