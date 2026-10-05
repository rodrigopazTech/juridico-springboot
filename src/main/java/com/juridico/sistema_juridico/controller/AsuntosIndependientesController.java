package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.procesal.AsuntoIndependienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
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

    @PostMapping("/vincular")
    public String vincular(@RequestParam Long asuntoId, @RequestParam UUID expedienteId,
            @RequestParam(required = false) String motivo,
            @RequestParam(defaultValue = "/terminos") String volver,
            RedirectAttributes redirectAttributes) {
        String identidad = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario actor = usuarioRepository.findByEmail(identidad)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario actual."));
        asuntoService.vincularAExpediente(asuntoId, expedienteId, motivo, actor);
        redirectAttributes.addFlashAttribute("mensaje", "Asunto Independiente vinculado correctamente.");
        redirectAttributes.addFlashAttribute("tipo", "success");
        String destino = "/audiencias".equals(volver) ? "/audiencias" : "/terminos";
        return "redirect:" + destino;
    }
}
