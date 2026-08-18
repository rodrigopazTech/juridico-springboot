package com.juridico.sistema_juridico.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;

@Controller
public class AuthController {

    private final MicrosoftAuthProperties microsoftAuthProperties;

    public AuthController(MicrosoftAuthProperties microsoftAuthProperties) {
        this.microsoftAuthProperties = microsoftAuthProperties;
    }

    // Mapeamos la raíz "/" al archivo HTML de login
    @GetMapping({ "/", "/login" })
    public String login(HttpSession session, Model model, Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        if (Boolean.TRUE.equals(session.getAttribute("logoutSuccess"))) {
            model.addAttribute("logoutSuccess", true);
            session.removeAttribute("logoutSuccess"); // se consume una sola vez
        }
        model.addAttribute("microsoftLoginConfigured", microsoftAuthProperties.isVisibleInUi());
        return "views/auth/index";
    }
}
