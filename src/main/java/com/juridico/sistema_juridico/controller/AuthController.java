package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    
    // Mapeamos la raíz "/" al archivo HTML de login
    @GetMapping("/")
    public String login() {
        return "views/auth/index"; 
    }
}