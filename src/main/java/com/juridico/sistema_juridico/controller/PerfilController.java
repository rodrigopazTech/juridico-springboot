package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.usuario.PreferenciaNotificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.response.notificaciones.GrupoPreferenciaNotificacionResponse;
import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;
import com.juridico.sistema_juridico.repository.Usuarios.PreferenciaNotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.security.CustomUserDetails;
import com.juridico.sistema_juridico.service.PasswordPolicyService;
import com.juridico.sistema_juridico.service.PreferenciaNotificacionEventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PreferenciaNotificacionRepository preferenciaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    @Autowired
    private PreferenciaNotificacionEventoService preferenciaEventoService;

    @Autowired
    private MicrosoftAuthProperties microsoftAuthProperties;

    @GetMapping
    public String verPerfil(
            @RequestParam(value = "tab", defaultValue = "datos") String tab,
            Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        PreferenciaNotificacion prefs = preferenciaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> PreferenciaNotificacion.builder().usuario(usuario).build());
        List<GrupoPreferenciaNotificacionResponse> gruposPreferencias = preferenciaEventoService.construirVista(usuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("preferencias", prefs);
        model.addAttribute("gruposPreferencias", gruposPreferencias);
        model.addAttribute("pageTitle", "Mi Cuenta");
        model.addAttribute("activePage", "perfil");
        model.addAttribute("tabInicial", tab);
        model.addAttribute("microsoftConfigured", microsoftAuthProperties.isVisibleInUi());

        return "views/perfil/index";
    }

    @PostMapping("/preferencias/guardar")
    public String guardarPreferencias(
            @RequestParam(value = "eventos", required = false) List<NotificacionEvento> eventos,
            RedirectAttributes redirectAttrs) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        preferenciaEventoService.guardarPreferencias(usuario, eventos);

        redirectAttrs.addFlashAttribute("mensaje", "Preferencias actualizadas correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        redirectAttrs.addFlashAttribute("origenMensaje", "preferencias");

        return "redirect:/perfil?tab=notif";
    }

    @PostMapping("/password/actualizar")
    public String actualizarPassword(
            @RequestParam("passwordActual") String passwordActual,
            @RequestParam("nuevoPassword") String nuevoPassword,
            @RequestParam("confirmarPassword") String confirmarPassword,
            RedirectAttributes redirectAttrs) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            redirectAttrs.addFlashAttribute("mensaje", "La contrasena actual es incorrecta.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            redirectAttrs.addFlashAttribute("origenMensaje", "password");
            return "redirect:/perfil";
        }

        if (!nuevoPassword.equals(confirmarPassword)) {
            redirectAttrs.addFlashAttribute("mensaje", "Las nuevas contrasenas no coinciden.");
            redirectAttrs.addFlashAttribute("tipo", "warning");
            redirectAttrs.addFlashAttribute("origenMensaje", "password");
            return "redirect:/perfil";
        }

        if (!passwordPolicyService.isValid(nuevoPassword)) {
            redirectAttrs.addFlashAttribute("mensaje", passwordPolicyService.description());
            redirectAttrs.addFlashAttribute("tipo", "warning");
            redirectAttrs.addFlashAttribute("origenMensaje", "password");
            return "redirect:/perfil";
        }

        usuario.setPassword(passwordEncoder.encode(nuevoPassword));
        usuario.setForcePasswordChange(false);
        usuario.setPasswordUpdatedAt(java.time.LocalDateTime.now());
        usuarioRepository.save(usuario);

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

        redirectAttrs.addFlashAttribute("mensaje", "Contrasena actualizada exitosamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        redirectAttrs.addFlashAttribute("origenMensaje", "password");
        return "redirect:/perfil?tab=seguridad";
    }
}
