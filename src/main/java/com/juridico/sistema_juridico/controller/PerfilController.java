package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.PreferenciaNotificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.PreferenciaNotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PreferenciaNotificacionRepository preferenciaRepository;

    @GetMapping("/preferencias")
    public String verPreferencias(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        // Buscar preferencias o crear por defecto si es la primera vez
        PreferenciaNotificacion preferencias = preferenciaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> {
                    PreferenciaNotificacion nueva = PreferenciaNotificacion.builder()
                            .usuario(usuario)
                            .build();
                    return preferenciaRepository.save(nueva);
                });

        model.addAttribute("preferencias", preferencias);
        model.addAttribute("usuario", usuario);
        model.addAttribute("activePage", "perfil");
        return "views/perfil/preferencias";
    }

    @PostMapping("/preferencias/guardar")
    public String guardarPreferencias(@ModelAttribute PreferenciaNotificacion prefsForm,
            RedirectAttributes redirectAttrs) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        PreferenciaNotificacion preferencias = preferenciaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Preferencias no encontradas"));

        // Actualizar valores
        preferencias.setNotificarNuevaAudiencia(prefsForm.isNotificarNuevaAudiencia());
        preferencias.setNotificarTerminoVencer(prefsForm.isNotificarTerminoVencer());
        preferencias.setNotificarNuevoTermino(prefsForm.isNotificarNuevoTermino());
        preferencias.setNotificarReasignacion(prefsForm.isNotificarReasignacion());

        preferenciaRepository.save(preferencias);

        redirectAttrs.addFlashAttribute("mensaje", "Preferencias de notificación actualizadas correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/perfil/preferencias";
    }
}