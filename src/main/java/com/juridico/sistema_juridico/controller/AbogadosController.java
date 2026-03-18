package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
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

import java.util.stream.Collectors;
import java.util.List;
import java.util.Arrays;

@Controller
@RequestMapping("/abogados")
public class AbogadosController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String index(Model model, @RequestParam(value = "page", defaultValue = "0") int page) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioActual = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado."));

        Pageable pageUs = PageRequest.of(page, 10, Sort.by("nombreCompleto").ascending());
        Page<Usuario> usuariosPage;

        if (usuarioActual.getRol() == RolUsuario.GERENTE && usuarioActual.getGerencia() != null) {
            usuariosPage = usuarioRepository.findByGerencia(usuarioActual.getGerencia(), pageUs);
        } else {
            usuariosPage = usuarioService.listarUsuariosPaginados(pageUs);
        }

        model.addAttribute("listaUsuarios", usuariosPage);

        if (usuarioActual.getRol() == RolUsuario.GERENTE) {
            model.addAttribute("allGerencias", Arrays.asList(usuarioActual.getGerencia()));
            model.addAttribute("listaRoles", Arrays.asList(RolUsuario.ABOGADO, RolUsuario.JEFE_DEPTO));
        } else {
            model.addAttribute("allGerencias", usuarioService.listarGerencias());
            model.addAttribute("listaRoles", RolUsuario.values());
        }

        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("nuevoUsuario", new Usuario());
        model.addAttribute("pageTitle", "Gestión de Abogados");
        model.addAttribute("activePage", "abogados");

        return "views/abogados/index";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam(value = "materiasIds", required = false) List<Integer> materiasIds,
            RedirectAttributes redirectAttrs) {
        usuarioService.guardarUsuario(usuario, materiasIds);
        redirectAttrs.addFlashAttribute("mensaje", "Abogado guardado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/abogados";
    }

    @GetMapping("/toggle/{id}")
    public String toggleStatusUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioService.buscarPorId(id);
        usuario.setActivo(!usuario.getActivo());
        List<Integer> materiasIds = usuario.getMaterias().stream().map(Materia::getId).collect(Collectors.toList());
        usuarioService.guardarUsuario(usuario, materiasIds);
        String estado = usuario.getActivo() ? "activado" : "desactivado";
        redirectAttrs.addFlashAttribute("mensaje", "Abogado " + estado + " correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/abogados";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        usuarioService.eliminarUsuario(id);
        redirectAttrs.addFlashAttribute("mensaje", "Abogado eliminado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/abogados";
    }
}