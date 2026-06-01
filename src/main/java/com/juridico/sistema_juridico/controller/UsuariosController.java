package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.service.UsuarioService;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Controller
@RequestMapping("/usuarios")
public class UsuariosController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String index(Model model,
            @RequestParam(value = "pageUsuarios", defaultValue = "0") int pageUsuarios,
            @RequestParam(value = "pageGerencias", defaultValue = "0") int pageGerencias,
            @RequestParam(value = "pageOrganos", defaultValue = "0") int pageOrganos,
            @RequestParam(value = "pageTiposAudiencia", defaultValue = "0") int pageTiposAudiencia) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioActual = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado en la base de datos."));

        // Paginación Usuarios
        Pageable pageUs = PageRequest.of(pageUsuarios, 10, Sort.by("nombreCompleto").ascending());
        Page<Usuario> usuariosPage;
        if (usuarioActual.getRol() == RolUsuario.GERENTE && usuarioActual.getGerencia() != null) {
            usuariosPage = usuarioRepository.findByGerencia(usuarioActual.getGerencia(), pageUs);
        } else {
            usuariosPage = usuarioService.listarUsuariosPaginados(pageUs);
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

        if (usuarioActual.getRol() == RolUsuario.GERENTE) {
            model.addAttribute("allGerencias", Arrays.asList(usuarioActual.getGerencia()));
            model.addAttribute("listaRoles", Arrays.asList(RolUsuario.ABOGADO, RolUsuario.JEFE_DEPTO));
        } else {
            model.addAttribute("allGerencias", usuarioService.listarGerencias());
            model.addAttribute("listaRoles", RolUsuario.values());
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

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam(value = "materiasIds", required = false) java.util.List<Integer> materiasIds,
            RedirectAttributes redirectAttrs) {
        usuarioService.guardarUsuario(usuario, materiasIds);
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
        List<Integer> materiasIds = usuario.getMaterias().stream().map(Materia::getId).collect(Collectors.toList());
        usuarioService.guardarUsuario(usuario, materiasIds);
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

    @GetMapping("/api/materias-por-gerencia/{id}")
    @ResponseBody
    public ResponseEntity<List<Materia>> getMateriasJson(@PathVariable("id") Integer id) {
        try {
            Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
            return ResponseEntity.ok(new ArrayList<>(gerencia.getMaterias()));
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
}
