package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "Agenda");
        model.addAttribute("currentPage", "agenda");
        return "views/agenda/index";
    }
}