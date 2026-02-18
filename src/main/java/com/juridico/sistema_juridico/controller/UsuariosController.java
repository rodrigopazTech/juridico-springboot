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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import org.springframework.http.ResponseEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/usuarios")
public class UsuariosController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String index(Model model,
            @RequestParam(defaultValue = "0") int pageUsuarios) {

        Page<Usuario> usuariosPage = usuarioService
                .listarUsuariosPaginados(PageRequest.of(pageUsuarios, 10, Sort.by("nombreCompleto").ascending()));

        model.addAttribute("listaUsuarios", usuariosPage);

        // Listas completas para los Selects de asignación
        model.addAttribute("allGerencias", usuarioService.listarGerencias());

        model.addAttribute("listaRoles", RolUsuario.values());
        model.addAttribute("nuevoUsuario", new Usuario());
        model.addAttribute("activePage", "usuarios");

        return "views/usuarios/index";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam(required = false) java.util.List<Integer> materiasIds, // Recibe checkboxes
            RedirectAttributes redirectAttrs) {
        try {
            usuarioService.guardarUsuario(usuario, materiasIds);
            redirectAttrs.addFlashAttribute("mensaje", "Usuario guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al guardar usuario: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/api/materias-por-gerencia/{id}")
    @ResponseBody
    public ResponseEntity<java.util.List<Materia>> getMateriasJson(@PathVariable Integer id) {
        try {
            Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
            return ResponseEntity.ok(new ArrayList<>(gerencia.getMaterias()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/gerencias/{id}/materias")
    public String obtenerMateriasGerencia(@PathVariable Integer id, Model model) {
        Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
        // Pasamos la lista de materias al fragmento
        model.addAttribute("materias", gerencia.getMaterias());
        // Retornamos SOLO el fragmento 'lista' dentro del archivo 'modal-materias'
        return "fragments/usuarios/modal-materias :: lista";
    }

    @GetMapping("/toggle/{id}")
    public String toggleStatusUsuario(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            usuario.setActivo(!usuario.getActivo());

            List<Integer> materiasIds = usuario.getMaterias().stream()
                    .map(Materia::getId)
                    .collect(Collectors.toList());

            usuarioService.guardarUsuario(usuario, materiasIds);

            redirectAttrs.addFlashAttribute("mensaje", "Estatus del usuario actualizado.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Error al actualizar estatus.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/usuarios";
    }

    @DeleteMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id) {
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("No se puede eliminar: El usuario tiene expedientes o historial activo.");
        }
    }
}