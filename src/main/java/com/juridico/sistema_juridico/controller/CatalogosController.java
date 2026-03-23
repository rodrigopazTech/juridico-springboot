package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/catalogos")
public class CatalogosController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String index(Model model,
            @RequestParam(value = "pageGerencias", defaultValue = "0") int pageGerencias,
            @RequestParam(value = "pageOrganos", defaultValue = "0") int pageOrganos,
            @RequestParam(value = "pageTiposAudiencia", defaultValue = "0") int pageTiposAudiencia) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioActual = usuarioRepository.findByEmail(email).orElseThrow();

        Page<Gerencia> gerenciasPage = usuarioService
                .listarGerenciasPaginadas(PageRequest.of(pageGerencias, 10, Sort.by("nombre").ascending()));
        Page<OrganoJurisdiccional> organosPage = usuarioService
                .listarOrganosPaginados(PageRequest.of(pageOrganos, 10, Sort.by("nombre").ascending()));
        Page<TipoAudiencia> tiposAudienciaPage = usuarioService
                .listarTiposAudienciaPaginados(PageRequest.of(pageTiposAudiencia, 10, Sort.by("nombre").ascending()));

        model.addAttribute("listaGerencias", gerenciasPage);
        model.addAttribute("listaOrganos", organosPage);
        model.addAttribute("listaTiposAudiencia", tiposAudienciaPage);
        model.addAttribute("usuarioActual", usuarioActual);

        model.addAttribute("nuevaGerencia", new Gerencia());
        model.addAttribute("nuevoOrgano", new OrganoJurisdiccional());
        model.addAttribute("nuevoTipoAudiencia", new TipoAudiencia());

        model.addAttribute("pageTitle", "Catálogos Generales");
        model.addAttribute("activePage", "catalogos");

        return "views/catalogos/index";
    }

    @PostMapping("/gerencias/guardar")
    public String guardarGerencia(@ModelAttribute Gerencia gerencia, RedirectAttributes redirectAttrs) {
        usuarioService.guardarGerencia(gerencia);
        redirectAttrs.addFlashAttribute("mensaje", "Gerencia guardada.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/catalogos?tab=gerencias";
    }

    @PostMapping("/organos/guardar")
    public String guardarOrgano(@ModelAttribute OrganoJurisdiccional organo, RedirectAttributes redirectAttrs) {
        usuarioService.guardarOrgano(organo);
        redirectAttrs.addFlashAttribute("mensaje", "Órgano guardado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/catalogos?tab=organos";
    }

    @PostMapping("/tipos-audiencia/guardar")
    public String guardarTipoAudiencia(@ModelAttribute TipoAudiencia tipo, RedirectAttributes redirectAttrs) {
        usuarioService.guardarTipoAudiencia(tipo);
        redirectAttrs.addFlashAttribute("mensaje", "Tipo de Audiencia guardado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/catalogos?tab=tipos-audiencia";
    }

    @GetMapping("/gerencias/toggle/{id}")
    public String toggleGerencia(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Gerencia g = usuarioService.buscarGerenciaPorId(id);
        g.setActivo(!g.getActivo());
        usuarioService.guardarGerencia(g);
        return "redirect:/catalogos?tab=gerencias";
    }

    @GetMapping("/organos/toggle/{id}")
    public String toggleOrgano(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        OrganoJurisdiccional o = usuarioService.buscarOrganoPorId(id);
        o.setActivo(!o.getActivo());
        usuarioService.guardarOrgano(o);
        return "redirect:/catalogos?tab=organos";
    }

    @GetMapping("/tipos-audiencia/toggle/{id}")
    public String toggleTipoAudiencia(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        TipoAudiencia t = usuarioService.buscarTipoAudienciaPorId(id);
        t.setActivo(!t.getActivo());
        usuarioService.guardarTipoAudiencia(t);
        return "redirect:/catalogos?tab=tipos-audiencia";
    }

    @GetMapping("/organos/eliminar/{id}")
    public String eliminarOrgano(@PathVariable("id") Integer id) {
        usuarioService.eliminarOrgano(id);
        return "redirect:/catalogos?tab=organos";
    }

    @GetMapping("/tipos-audiencia/eliminar/{id}")
    public String eliminarTipoAudiencia(@PathVariable("id") Integer id) {
        usuarioService.eliminarTipoAudiencia(id);
        return "redirect:/catalogos?tab=tipos-audiencia";
    }

    // --- API JSON MATERIAS ---
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

    @DeleteMapping("/gerencias/materias/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarMateria(@PathVariable("id") Integer id) {
        try {
            usuarioService.eliminarMateria(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar materia");
        }
    }
}