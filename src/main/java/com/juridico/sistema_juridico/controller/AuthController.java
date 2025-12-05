package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    // Muestra la vista del formulario de Login
    @GetMapping("/login")
    public String login() {
        // Busca en templates/views/auth/login.html
        return "views/auth/login";
    }

    // Redirige la raíz "/" al dashboard directamente
    // (Spring Security interceptará esto: si no estás logueado, te manda a /login primero)
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
    
    // Opcional: Si aún no tienes la vista de registro creada, 
    // puedes comentar esto para evitar errores 404, o crear el archivo html.
    @GetMapping("/register")
    public String register() {
        return "views/auth/register";
    }
}