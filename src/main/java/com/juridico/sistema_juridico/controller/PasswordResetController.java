package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;
import com.juridico.sistema_juridico.dto.request.auth.PasswordResetConfirmRequest;
import com.juridico.sistema_juridico.dto.request.auth.PasswordResetRequest;
import com.juridico.sistema_juridico.dto.response.auth.PasswordResetResponse;
import com.juridico.sistema_juridico.service.PasswordResetService;
import com.juridico.sistema_juridico.service.PasswordResetService.ConfirmResetResult;
import com.juridico.sistema_juridico.service.PasswordResetService.ResetRequestResult;
import com.juridico.sistema_juridico.service.PasswordPolicyService;
import com.juridico.sistema_juridico.service.UsernameRecoveryService;
import com.juridico.sistema_juridico.service.UsernameRecoveryService.UsernameRecoveryResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String GENERIC_RESET_MESSAGE =
            "Si la cuenta existe, enviaremos un enlace al correo institucional registrado por el administrador.";
    private static final String GENERIC_USERNAME_MESSAGE =
            "Si el correo institucional esta registrado, enviaremos tu usuario a ese correo.";

    private final PasswordResetService passwordResetService;
    private final PasswordPolicyService passwordPolicyService;
    private final UsernameRecoveryService usernameRecoveryService;
    private final MicrosoftAuthProperties microsoftAuthProperties;

    @Value("${app.email.maintenance:false}")
    private boolean emailMaintenance;

    @Value("${app.email.maintenance-message:El servicio de correo esta en mantenimiento.}")
    private String emailMaintenanceMessage;

    @GetMapping("/auth/solicitar-reset")
    public String mostrarSolicitudReset(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new PasswordResetRequest());
        }
        agregarEstadoCorreo(model);
        return "views/auth/solicitar-reset";
    }

    @PostMapping("/auth/solicitar-reset")
    public String solicitarResetWeb(@ModelAttribute("request") PasswordResetRequest resetRequest,
            HttpServletRequest servletRequest, RedirectAttributes redirectAttributes) {
        if (emailMaintenance) {
            redirectAttributes.addFlashAttribute("error", emailMaintenanceMessage);
            return "redirect:/auth/solicitar-reset";
        }
        ResetRequestResult result = passwordResetService.requestPasswordReset(resetRequest.getIdentifier(), servletRequest);
        if (result == ResetRequestResult.RATE_LIMITED) {
            redirectAttributes.addFlashAttribute("error",
                    "Se alcanzaron demasiadas solicitudes. Intenta nuevamente mas tarde.");
        } else {
            redirectAttributes.addFlashAttribute("success", GENERIC_RESET_MESSAGE);
        }
        return "redirect:/auth/solicitar-reset";
    }

    @GetMapping("/auth/recordar-usuario")
    public String mostrarRecordarUsuario(Model model) {
        agregarEstadoCorreo(model);
        return "views/auth/recordar-usuario";
    }

    @PostMapping("/auth/recordar-usuario")
    public String recordarUsuario(@RequestParam("email") String email,
            HttpServletRequest servletRequest, RedirectAttributes redirectAttributes) {
        if (emailMaintenance) {
            redirectAttributes.addFlashAttribute("error", emailMaintenanceMessage);
            return "redirect:/auth/recordar-usuario";
        }
        UsernameRecoveryResult result = usernameRecoveryService.requestUsernameReminder(email, servletRequest);
        if (result == UsernameRecoveryResult.RATE_LIMITED) {
            redirectAttributes.addFlashAttribute("error",
                    "Se alcanzaron demasiadas solicitudes. Intenta nuevamente mas tarde.");
        } else {
            redirectAttributes.addFlashAttribute("success", GENERIC_USERNAME_MESSAGE);
        }
        return "redirect:/auth/recordar-usuario";
    }

    @GetMapping("/auth/reset-password")
    public String mostrarReset(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenValido", passwordResetService.isTokenValid(token));
        model.addAttribute("passwordPolicyMessage", passwordPolicyService.description());
        return "views/auth/reset-password";
    }

    @GetMapping("/auth/activar")
    public String mostrarActivacion(@RequestParam("token") String token,
            @RequestParam(value = "microsoftError", required = false) String microsoftError,
            Model model) {
        boolean tokenValido = passwordResetService.isTokenValid(token);
        model.addAttribute("token", token);
        model.addAttribute("tokenValido", tokenValido);
        model.addAttribute("microsoftConfigured", microsoftAuthProperties.isVisibleInUi());
        model.addAttribute("microsoftError", microsoftError);

        if (tokenValido) {
            Usuario usuario = passwordResetService.findUserForValidToken(token).orElse(null);
            model.addAttribute("usuarioActivacion", usuario);
            model.addAttribute("correoActivacion", usuario != null ? usuario.getEmail() : "");
            model.addAttribute("usernameActivacion", usuario != null ? usuario.getUsername() : "");
        }
        return "views/auth/activar";
    }

    @PostMapping("/auth/reset-password")
    public String confirmarResetWeb(@RequestParam("token") String token,
            @RequestParam("nuevaContrasena") String nuevaContrasena,
            @RequestParam("confirmacionContrasena") String confirmacionContrasena,
            HttpServletRequest servletRequest, RedirectAttributes redirectAttributes) {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(token);
        request.setNuevaContrasena(nuevaContrasena);
        request.setConfirmacionContrasena(confirmacionContrasena);

        ConfirmResetResult result = passwordResetService.confirmReset(request, servletRequest);
        if (result == ConfirmResetResult.SUCCESS) {
            redirectAttributes.addFlashAttribute("logoutSuccess", false);
            redirectAttributes.addFlashAttribute("passwordResetSuccess",
                    "Contrasena actualizada. Ya puedes iniciar sesion.");
            return "redirect:/";
        }

        redirectAttributes.addFlashAttribute("error", messageFor(result));
        return "redirect:/auth/reset-password?token=" + token;
    }

    @PostMapping("/api/auth/solicitar-reset")
    @ResponseBody
    public ResponseEntity<PasswordResetResponse> solicitarResetApi(@Valid @RequestBody PasswordResetRequest resetRequest,
            HttpServletRequest servletRequest) {
        if (emailMaintenance) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new PasswordResetResponse("EMAIL_MAINTENANCE", emailMaintenanceMessage));
        }
        ResetRequestResult result = passwordResetService.requestPasswordReset(resetRequest.getIdentifier(), servletRequest);
        if (result == ResetRequestResult.RATE_LIMITED) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new PasswordResetResponse("RATE_LIMITED",
                            "Demasiadas solicitudes. Intenta nuevamente mas tarde."));
        }
        return ResponseEntity.accepted()
                .body(new PasswordResetResponse("ACCEPTED", GENERIC_RESET_MESSAGE));
    }

    @GetMapping("/api/auth/validar-token")
    @ResponseBody
    public Map<String, Boolean> validarToken(@RequestParam("token") String token) {
        return Map.of("valid", passwordResetService.isTokenValid(token));
    }

    @PostMapping("/api/auth/confirmar-reset")
    @ResponseBody
    public ResponseEntity<PasswordResetResponse> confirmarResetApi(
            @Valid @RequestBody PasswordResetConfirmRequest resetRequest,
            HttpServletRequest servletRequest) {
        ConfirmResetResult result = passwordResetService.confirmReset(resetRequest, servletRequest);
        if (result == ConfirmResetResult.SUCCESS) {
            return ResponseEntity.ok(new PasswordResetResponse("SUCCESS",
                    "Contrasena actualizada correctamente."));
        }
        return ResponseEntity.badRequest()
                .body(new PasswordResetResponse(result.name(), messageFor(result)));
    }

    private String messageFor(ConfirmResetResult result) {
        return switch (result) {
            case INVALID_TOKEN -> "El enlace es invalido o ha expirado.";
            case PASSWORD_MISMATCH -> "Las contrasenas no coinciden.";
            case WEAK_PASSWORD -> passwordPolicyService.description();
            case SUCCESS -> "Contrasena actualizada correctamente.";
        };
    }

    private void agregarEstadoCorreo(Model model) {
        model.addAttribute("emailMaintenance", emailMaintenance);
        model.addAttribute("emailMaintenanceMessage", emailMaintenanceMessage);
    }
}
