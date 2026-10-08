package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.configuracion.ConfiguracionCorreoSmtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import com.juridico.sistema_juridico.dto.response.configuracion.ConfiguracionCorreoSmtpView;

@Controller
@RequestMapping("/admin/configuracion-correo")
@RequiredArgsConstructor
public class AdminConfiguracionCorreoController {

    private final ConfiguracionCorreoSmtpService service;

    @GetMapping
    public String index(Model model) {
        List<ConfiguracionCorreoSmtpView> history = service.history();
        model.addAttribute("activePage", "configuracion-correo");
        model.addAttribute("activeConfiguration", service.active());
        model.addAttribute("rollbackCandidate", service.rollbackCandidate());
        model.addAttribute("history", history);
        model.addAttribute("latestFailedTest", history.stream()
                .filter(item -> Boolean.FALSE.equals(item.pruebaExitosa()))
                .findFirst()
                .orElse(null));
        model.addAttribute("encryptionReady", service.encryptionReady());
        model.addAttribute("connectionSummary", service.connectionSummary());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("suggestedNextChangeDate", LocalDate.now().plusDays(30));
        return "views/admin/configuracion-correo";
    }

    @PostMapping("/candidatas")
    public String createCandidate(
            @RequestParam String password,
            @RequestParam String testEmail,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextChangeDate,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            service.createCandidate(password, testEmail, nextChangeDate, actor(principal));
            redirectAttributes.addFlashAttribute("success",
                    "Credencial guardada de forma cifrada. Al superar la prueba se activara automaticamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirect();
    }

    @PostMapping("/{id}/probar")
    public String testCandidate(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            ConfiguracionCorreoSmtpService.TestResult result = service.testCandidate(id, actor(principal));
            redirectAttributes.addFlashAttribute(result.success() ? "success" : "error", result.message());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirect();
    }

    @PostMapping("/{id}/activar")
    public String activate(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            service.activate(id, actor(principal));
            redirectAttributes.addFlashAttribute("success",
                    "La nueva contrasena SMTP quedo activa sin reiniciar la aplicacion.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirect();
    }

    @PostMapping("/rollback")
    public String rollback(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            service.rollback(actor(principal));
            redirectAttributes.addFlashAttribute("success", "Se restauro la credencial anterior.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirect();
    }

    private String actor(Principal principal) {
        return principal != null ? principal.getName() : "administrador";
    }

    private String redirect() {
        return "redirect:/admin/configuracion-correo";
    }
}
