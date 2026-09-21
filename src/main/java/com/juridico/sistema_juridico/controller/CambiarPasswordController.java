package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.security.CustomUserDetails;
import com.juridico.sistema_juridico.service.PasswordPolicyService;
import com.juridico.sistema_juridico.service.SessionRevocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CambiarPasswordController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    @Autowired
    private SessionRevocationService sessionRevocationService;

    @GetMapping("/cambiar-password")
    public String mostrarFormulario(
            @RequestParam(value = "razon", defaultValue = "primera-vez") String razon,
            org.springframework.ui.Model model) {
        model.addAttribute("razon", razon); // "primera-vez" o "vencida"
        return "views/auth/cambiar-password";
    }

    @PostMapping("/cambiar-password")
    public String procesarCambioPassword(@RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "razon", defaultValue = "primera-vez") String razon,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden.");
            redirectAttributes.addFlashAttribute("razon", razon);
            return "redirect:/cambiar-password?razon=" + razon;
        }

        if (!passwordPolicyService.isValid(newPassword)) {
            redirectAttributes.addFlashAttribute("error", passwordPolicyService.description());
            redirectAttributes.addFlashAttribute("razon", razon);
            return "redirect:/cambiar-password?razon=" + razon;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

            if (usuario != null) {
                usuario.setPassword(passwordEncoder.encode(newPassword));
                usuario.setForcePasswordChange(false);
                usuario.setPasswordUpdatedAt(java.time.LocalDateTime.now()); // Registro de caducidad
                usuarioRepository.save(usuario);

                sessionRevocationService.revokeAll(usuario, "PASSWORD_CHANGED");

                // Actualizar el Authentication tag en session
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

                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
                return "redirect:/?passwordChanged=true";
            }
        }

        return "redirect:/login";
    }
}
