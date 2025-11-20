package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/audiencias")
public class AudienciasController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "Audiencias");
        model.addAttribute("currentPage", "audiencias");
        return "views/audiencias/index";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("pageTitle", "Nueva Audiencia");
        model.addAttribute("currentPage", "audiencias");
        return "views/audiencias/nueva";
    }
}