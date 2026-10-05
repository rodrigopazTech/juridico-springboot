package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.procesal.AsuntoIndependienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/asuntos-independientes")
@RequiredArgsConstructor
public class AsuntosIndependientesController {
    private final AsuntoIndependienteService asuntoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public String listado(@RequestParam(required = false) String q, Model model) {
        Usuario actor = getUsuarioActual();
        model.addAttribute("asuntos", asuntoService.listarTodos(actor, q));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("activePage", "asuntos-independientes");
        model.addAttribute("pageTitle", "Asuntos Independientes");
        return "views/asuntos-independientes/index";
    }

    @PostMapping("/vincular")
    public String vincular(@RequestParam Long asuntoId, @RequestParam UUID expedienteId,
            @RequestParam(required = false) String motivo,
            @RequestParam(defaultValue = "/terminos") String volver,
            RedirectAttributes redirectAttributes) {
        Usuario actor = getUsuarioActual();
        asuntoService.vincularAExpediente(asuntoId, expedienteId, motivo, actor);
        redirectAttributes.addFlashAttribute("mensaje", "Asunto Independiente vinculado correctamente.");
        redirectAttributes.addFlashAttribute("tipo", "success");
        String destino = "/audiencias".equals(volver) ? "/audiencias" : "/terminos";
        return "redirect:" + destino;
    }

    private Usuario getUsuarioActual() {
        String identidad = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(identidad)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario actual."));
    }
}
