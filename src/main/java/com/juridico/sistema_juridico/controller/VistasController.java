package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller 
public class VistasController {

   
    @GetMapping("/")
    public String mostrarLogin() {

        
        return "views/auth/index"; 
    }
}