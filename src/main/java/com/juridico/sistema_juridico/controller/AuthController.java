package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "views/auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "views/auth/register";
    }

    // Redirige a dashboard (Security redirigirá a login si no está autenticado)
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}