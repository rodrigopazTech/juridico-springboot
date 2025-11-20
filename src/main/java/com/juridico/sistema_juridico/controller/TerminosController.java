package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/terminos")
public class TerminosController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "Términos");
        model.addAttribute("currentPage", "terminos");
        return "views/terminos/index";
    }
}