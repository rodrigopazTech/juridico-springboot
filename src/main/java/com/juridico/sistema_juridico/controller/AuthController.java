package com.juridico.sistema_juridico.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    // Mapeamos la raíz "/" al archivo HTML de login
    @GetMapping("/")
    public String login(HttpSession session, Model model) {
        if (Boolean.TRUE.equals(session.getAttribute("logoutSuccess"))) {
            model.addAttribute("logoutSuccess", true);
            session.removeAttribute("logoutSuccess"); // se consume una sola vez
        }
        return "views/auth/index";
    }
}