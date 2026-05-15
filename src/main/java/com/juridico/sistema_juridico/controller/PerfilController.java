package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.PreferenciaNotificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.PreferenciaNotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.security.CustomUserDetails; // Importante para actualizar la sesión

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private PasswordEncoder passwordEncoder; // Inyectamos el encriptador de contraseñas

    // --- 1. VER PERFIL ---
    @GetMapping
    public String verPerfil(
            @RequestParam(value = "tab", defaultValue = "datos") String tab,
            Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        PreferenciaNotificacion prefs = preferenciaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> PreferenciaNotificacion.builder().usuario(usuario).build());

        model.addAttribute("usuario", usuario);
        model.addAttribute("preferencias", prefs);
        model.addAttribute("pageTitle", "Mi Cuenta");
        model.addAttribute("activePage", "perfil");
        model.addAttribute("tabInicial", tab); // "datos", "notif", "seguridad"

        return "views/perfil/index";
    }

    // --- 2. GUARDAR PREFERENCIAS (Solución Checkboxes) ---
    @PostMapping("/preferencias/guardar")
    public String guardarPreferencias(
            @RequestParam(value = "notificarNuevaAudiencia", defaultValue = "false") boolean notificarNuevaAudiencia,
            @RequestParam(value = "notificarTerminoVencer", defaultValue = "false") boolean notificarTerminoVencer,
            @RequestParam(value = "notificarNuevoTermino", defaultValue = "false") boolean notificarNuevoTermino,
            @RequestParam(value = "notificarReasignacion", defaultValue = "false") boolean notificarReasignacion,
            RedirectAttributes redirectAttrs) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        PreferenciaNotificacion preferencias = preferenciaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> PreferenciaNotificacion.builder().usuario(usuario).build());

        // Al usar RequestParam con defaultValue="false", forzamos a Spring a entender
        // cuando se desmarcan
        preferencias.setNotificarNuevaAudiencia(notificarNuevaAudiencia);
        preferencias.setNotificarTerminoVencer(notificarTerminoVencer);
        preferencias.setNotificarNuevoTermino(notificarNuevoTermino);
        preferencias.setNotificarReasignacion(notificarReasignacion);

        preferenciaRepository.save(preferencias);

        redirectAttrs.addFlashAttribute("mensaje", "Preferencias actualizadas correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");

        return "redirect:/perfil";
    }

    // --- 3. ACTUALIZAR CONTRASEÑA ---
    @PostMapping("/password/actualizar")
    public String actualizarPassword(
            @RequestParam("passwordActual") String passwordActual,
            @RequestParam("nuevoPassword") String nuevoPassword,
            @RequestParam("confirmarPassword") String confirmarPassword,
            RedirectAttributes redirectAttrs) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        // A. Validar que la contraseña actual sea la correcta
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            redirectAttrs.addFlashAttribute("mensaje", "La contraseña actual es incorrecta.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/perfil";
        }
        

        // B. Validar que las contraseñas nuevas coincidan
        if (!nuevoPassword.equals(confirmarPassword)) {
            redirectAttrs.addFlashAttribute("mensaje", "Las nuevas contraseñas no coinciden.");
            redirectAttrs.addFlashAttribute("tipo", "warning");
            return "redirect:/perfil";
        }

        // C. Actualizar y guardar en base de datos
        usuario.setPassword(passwordEncoder.encode(nuevoPassword));
        usuario.setForcePasswordChange(false);
        usuario.setPasswordUpdatedAt(java.time.LocalDateTime.now()); // Registro de caducidad
        usuarioRepository.save(usuario);

        // D. Actualizar la sesión de Spring Security para no desloguear al usuario
        if (auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
            CustomUserDetails updatedPrincipal = new CustomUserDetails(
                    principal.getUsername(),
                    usuario.getPassword(),
                    principal.isEnabled(),
                    principal.isAccountNonExpired(),
                    principal.isCredentialsNonExpired(),
                    principal.isAccountNonLocked(),
                    principal.getAuthorities(),
                    false);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(updatedPrincipal,
                    auth.getCredentials(), auth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }

        redirectAttrs.addFlashAttribute("mensaje", "Contraseña actualizada exitosamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/perfil?tab=seguridad";
    }
}