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

    @GetMapping
    public String verPerfil(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        // Obtenemos o creamos las preferencias si no existen
        PreferenciaNotificacion prefs = preferenciaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> PreferenciaNotificacion.builder().usuario(usuario).build());

        model.addAttribute("usuario", usuario);
        model.addAttribute("preferencias", prefs);
        model.addAttribute("pageTitle", "Mi Cuenta");
        model.addAttribute("activePage", "perfil");

        return "views/perfil/index"; // Una sola vista para todo
    }

    @PostMapping("/preferencias/guardar")
    public String guardarPreferencias(@ModelAttribute PreferenciaNotificacion prefsForm,
            RedirectAttributes redirectAttrs) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        // CORRECCIÓN: Si no existen, creamos un objeto nuevo asociado al usuario en
        // lugar de lanzar error
        PreferenciaNotificacion preferencias = preferenciaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> PreferenciaNotificacion.builder().usuario(usuario).build());

        // Actualizar valores
        preferencias.setNotificarNuevaAudiencia(prefsForm.isNotificarNuevaAudiencia());
        preferencias.setNotificarTerminoVencer(prefsForm.isNotificarTerminoVencer());
        preferencias.setNotificarNuevoTermino(prefsForm.isNotificarNuevoTermino());
        preferencias.setNotificarReasignacion(prefsForm.isNotificarReasignacion());

        preferenciaRepository.save(preferencias);

        redirectAttrs.addFlashAttribute("mensaje", "Preferencias de notificación actualizadas correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");

        return "redirect:/perfil";
    }
}