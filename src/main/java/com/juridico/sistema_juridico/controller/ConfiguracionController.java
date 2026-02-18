package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    private final GerenciaRepository gerenciaRepository;
    private final MateriaRepository materiaRepository;
    private final OrganoJurisdiccionalRepository organoRepository;
    private final com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository usuarioRepository;

    public ConfiguracionController(GerenciaRepository gerenciaRepository,
            MateriaRepository materiaRepository,
            OrganoJurisdiccionalRepository organoRepository,
            com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository usuarioRepository) {
        this.gerenciaRepository = gerenciaRepository;
        this.materiaRepository = materiaRepository;
        this.organoRepository = organoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private boolean esAdmin() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        return usuarioRepository.findByEmail(email)
                .map(u -> u.getRol() == com.juridico.sistema_juridico.Entity.enums.RolUsuario.DIRECCION)
                .orElse(false);
    }

    @GetMapping
    public String index(Model model) {
        if (!esAdmin())
            return "redirect:/dashboard";
        model.addAttribute("activePage", "configuracion");
        return "views/configuracion/index";
    }

    // LISTAR
    @GetMapping("/catalogos/{tipo}")
    public String listarCatalogo(@PathVariable String tipo, Model model) {
        if (!esAdmin())
            return "redirect:/dashboard";
        model.addAttribute("tipo", tipo);
        model.addAttribute("activePage", "configuracion");

        switch (tipo) {
            case "gerencias":
                model.addAttribute("titulo", "Gerencias");
                model.addAttribute("lista", gerenciaRepository.findAll());
                break;
            case "materias":
                model.addAttribute("titulo", "Materias");
                model.addAttribute("lista", materiaRepository.findAll());
                break;
            case "organos":
                model.addAttribute("titulo", "Órganos Jurisdiccionales");
                model.addAttribute("lista", organoRepository.findAll());
                break;
            default:
                return "redirect:/configuracion";
        }
        return "views/configuracion/lista-catalogo";
    }

    // CREAR
    @GetMapping("/catalogos/{tipo}/crear")
    public String crear(@PathVariable String tipo, Model model) {
        if (!esAdmin())
            return "redirect:/dashboard";
        model.addAttribute("tipo", tipo);
        model.addAttribute("activePage", "configuracion");

        Object entidad;
        switch (tipo) {
            case "gerencias":
                model.addAttribute("titulo", "Nueva Gerencia");
                entidad = new Gerencia();
                break;
            case "materias":
                model.addAttribute("titulo", "Nueva Materia");
                model.addAttribute("gerencias", gerenciaRepository.findByActivoTrueOrderByNombreAsc());
                entidad = new Materia();
                break;
            case "organos":
                model.addAttribute("titulo", "Nuevo Órgano Jurisdiccional");
                entidad = new OrganoJurisdiccional();
                break;
            default:
                return "redirect:/configuracion";
        }
        model.addAttribute("entidad", entidad);
        return "views/configuracion/form-catalogo";
    }

    // EDITAR
    @GetMapping("/catalogos/{tipo}/editar/{id}")
    public String editar(@PathVariable String tipo, @PathVariable Integer id, Model model) {
        if (!esAdmin())
            return "redirect:/dashboard";
        if (id == null)
            return "redirect:/configuracion";
        model.addAttribute("tipo", tipo);
        model.addAttribute("activePage", "configuracion");

        Object entidad;
        switch (tipo) {
            case "gerencias":
                model.addAttribute("titulo", "Editar Gerencia");
                entidad = gerenciaRepository.findById(id).orElseThrow();
                break;
            case "materias":
                model.addAttribute("titulo", "Editar Materia");
                model.addAttribute("gerencias", gerenciaRepository.findByActivoTrueOrderByNombreAsc());
                entidad = materiaRepository.findById(id).orElseThrow();
                break;
            case "organos":
                model.addAttribute("titulo", "Editar Órgano");
                entidad = organoRepository.findById(id).orElseThrow();
                break;
            default:
                return "redirect:/configuracion";
        }
        model.addAttribute("entidad", entidad);
        return "views/configuracion/form-catalogo";
    }

    // GUARDAR
    @PostMapping("/catalogos/gerencias/guardar")
    public String guardarGerencia(@ModelAttribute Gerencia gerencia, RedirectAttributes ra) {
        if (!esAdmin())
            return "redirect:/dashboard";
        if (gerencia.getId() == null) {
            gerencia.setCreatedAt(java.time.LocalDateTime.now());
        }
        gerencia.setUpdatedAt(java.time.LocalDateTime.now());
        gerenciaRepository.save(gerencia);
        ra.addFlashAttribute("success", "Gerencia guardada correctamente");
        return "redirect:/configuracion/catalogos/gerencias";
    }

    @PostMapping("/catalogos/materias/guardar")
    public String guardarMateria(@ModelAttribute Materia materia, RedirectAttributes ra) {
        if (!esAdmin())
            return "redirect:/dashboard";
        if (materia.getId() == null) {
            materia.setCreatedAt(java.time.LocalDateTime.now());
        }
        materia.setUpdatedAt(java.time.LocalDateTime.now());
        materiaRepository.save(materia);
        ra.addFlashAttribute("success", "Materia guardada correctamente");
        return "redirect:/configuracion/catalogos/materias";
    }

    @PostMapping("/catalogos/organos/guardar")
    public String guardarOrgano(@ModelAttribute OrganoJurisdiccional organo, RedirectAttributes ra) {
        if (!esAdmin())
            return "redirect:/dashboard";
        if (organo.getId() == null) {
            organo.setCreatedAt(java.time.LocalDateTime.now());
        }
        organo.setUpdatedAt(java.time.LocalDateTime.now());
        organoRepository.save(organo);
        ra.addFlashAttribute("success", "Órgano Jurisdiccional guardado correctamente");
        return "redirect:/configuracion/catalogos/organos";
    }

    // --- AJAX MATERIAS PARA GERENCIAS ---

    @GetMapping("/gerencias/{id}/materias")
    public String obtenerMateriasGerencia(@PathVariable Integer id, Model model) {
        if (!esAdmin())
            return "fragments/configuracion/modal-materias :: lista"; // Fragment will handle empty
        Gerencia gerencia = gerenciaRepository.findById(id).orElseThrow();
        model.addAttribute("materias", gerencia.getMaterias());
        return "fragments/configuracion/modal-materias :: lista";
    }

    @PostMapping("/gerencias/materias/guardar")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> guardarMateriaAJAX(@RequestBody Materia materia) {
        if (!esAdmin())
            return org.springframework.http.ResponseEntity.status(403).body("Acceso denegado");
        try {
            if (materia.getId() == null) {
                materia.setCreatedAt(java.time.LocalDateTime.now());
            }
            materia.setUpdatedAt(java.time.LocalDateTime.now());
            materiaRepository.save(materia);
            return org.springframework.http.ResponseEntity.ok().build();
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body("Error al guardar materia: " + e.getMessage());
        }
    }

    @DeleteMapping("/gerencias/materias/eliminar/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> eliminarMateriaAJAX(@PathVariable Integer id) {
        if (!esAdmin())
            return org.springframework.http.ResponseEntity.status(403).body("Acceso denegado");
        try {
            materiaRepository.deleteById(id);
            return org.springframework.http.ResponseEntity.ok().build();
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body("No se puede eliminar la materia (posibles expedientes vinculados).");
        }
    }
}
