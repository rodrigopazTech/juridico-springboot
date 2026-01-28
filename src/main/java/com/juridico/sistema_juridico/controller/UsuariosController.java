package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/usuarios")
public class UsuariosController {

    @Autowired private UsuarioService usuarioService;

    @GetMapping
    public String index(Model model) {
        // Cargar listas para las tablas y selectores
        model.addAttribute("listaUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("listaGerencias", usuarioService.listarGerencias());
        model.addAttribute("listaRoles", RolUsuario.values()); // Enum de roles
        
        // Objetos vacíos para los formularios de creación
        model.addAttribute("nuevoUsuario", new Usuario());
        model.addAttribute("nuevaGerencia", new Gerencia());
        
        return "views/usuarios/index";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, RedirectAttributes redirectAttrs) {
        try {
            usuarioService.guardarUsuario(usuario);
            redirectAttrs.addFlashAttribute("mensaje", "Usuario guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al guardar usuario: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/gerencias/guardar")
    public String guardarGerencia(@ModelAttribute Gerencia gerencia, RedirectAttributes redirectAttrs) {
        try {
            usuarioService.guardarGerencia(gerencia);
            redirectAttrs.addFlashAttribute("mensaje", "Gerencia creada correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al crear gerencia.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/usuarios?tab=gerencias";
    }

    @GetMapping("/gerencias/{id}/materias")
    public String obtenerMateriasGerencia(@PathVariable Integer id, Model model) {
        Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
        // Pasamos la lista de materias al fragmento
        model.addAttribute("materias", gerencia.getMaterias()); 
        // Retornamos SOLO el fragmento 'lista' dentro del archivo 'modal-materias'
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

    @GetMapping("/toggle/{id}")
    public String toggleStatusUsuario(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id); 
            usuario.setActivo(!usuario.getActivo()); 
            usuarioService.guardarUsuario(usuario);
            
            redirectAttrs.addFlashAttribute("mensaje", "Estatus del usuario actualizado.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al actualizar estatus.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/gerencias/toggle/{id}")
    public String toggleStatusGerencia(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        try {
            Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
            gerencia.setActivo(!gerencia.getActivo());
            usuarioService.guardarGerencia(gerencia);
            
            redirectAttrs.addFlashAttribute("mensaje", "Estatus de la gerencia actualizado.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al actualizar estatus.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/usuarios?tab=gerencias";
    }

    @DeleteMapping("/gerencias/materias/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarMateria(@PathVariable Integer id) {
        try {
            usuarioService.eliminarMateria(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("No se puede eliminar la materia (quizás tiene expedientes asignados).");
        }
    }

    // 3. ELIMINAR USUARIO (Borrado Físico)
    @DeleteMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id) {
        try {
            Usuario u = usuarioService.buscarPorId(id);
            
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("No se puede eliminar: El usuario tiene expedientes o historial activo.");
        }
    }
}