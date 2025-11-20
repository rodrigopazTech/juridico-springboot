package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/expedientes")
public class ExpedientesController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "Expedientes");
        model.addAttribute("currentPage", "expedientes");
        return "views/expedientes/index";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("pageTitle", "Nuevo Expediente");
        model.addAttribute("currentPage", "expedientes");
        return "views/expedientes/nuevo";
    }
}