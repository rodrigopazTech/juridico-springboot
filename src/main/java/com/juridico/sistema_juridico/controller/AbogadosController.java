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

    // ====================================================================
    // --- APIS PARA EL PANEL DE DETALLE (MAESTRO-DETALLE) ---
    // ====================================================================

    @GetMapping("/api/{id}/casos")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<java.util.Map<String, Object>>> obtenerCasosMock(
            @PathVariable("id") Integer id) {

        List<java.util.Map<String, Object>> casosMock = new java.util.ArrayList<>();

        // Simulamos que si el ID es par tiene datos, si es impar no tiene (para probar
        // estados vacíos)
        if (id % 2 == 0) {
            casosMock.add(java.util.Map.of(
                    "numero", "C-2023-015", "asunto", "Adquisición TechCorp",
                    "cliente", "Innovate Solutions", "estatus", "Activo", "fecha", "25/10/2024"));
            casosMock.add(java.util.Map.of(
                    "numero", "C-2023-021", "asunto", "Patente de Software",
                    "cliente", "Quantum Leap Inc.", "estatus", "Activo", "fecha", "15/11/2024"));
            casosMock.add(java.util.Map.of(
                    "numero", "C-2022-108", "asunto", "Litigio Contractual",
                    "cliente", "Global Logistics", "estatus", "En Espera", "fecha", "-"));
            casosMock.add(java.util.Map.of(
                    "numero", "C-2021-095", "asunto", "Reestructuración Fiscal",
                    "cliente", "Alpha Investments", "estatus", "Cerrado", "fecha", "-"));
        }

        return org.springframework.http.ResponseEntity.ok(casosMock);
    }

    @GetMapping("/api/{id}/agenda")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<java.util.Map<String, Object>>> obtenerAgendaMock(
            @PathVariable("id") Integer id) {

        List<java.util.Map<String, Object>> agendaMock = new java.util.ArrayList<>();

        if (id % 2 == 0) {
            agendaMock.add(java.util.Map.of(
                    "tipo", "Audiencia", "descripcion", "Desahogo de pruebas (C-2023-015)", "fecha",
                    "25/10/2024 10:00 AM"));
            agendaMock.add(java.util.Map.of(
                    "tipo", "Término", "descripcion", "Presentación de alegatos", "fecha", "30/10/2024 23:59 PM"));
        }

        return org.springframework.http.ResponseEntity.ok(agendaMock);
    }
}