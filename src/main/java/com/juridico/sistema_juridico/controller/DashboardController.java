package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @GetMapping
    public String dashboard(Model model) {
        // Título que aparecerá en la pestaña del navegador (<title>)
        model.addAttribute("pageTitle", "Panel Principal | Sistema Jurídico");
        
        // Variable clave para que el Sidebar resalte la opción "Dashboard"
        model.addAttribute("currentPage", "dashboard");
        
        // Retorna la plantilla Thymeleaf ubicada en:
        // src/main/resources/templates/views/dashboard/index.html
        return "views/dashboard/index";
    }
}