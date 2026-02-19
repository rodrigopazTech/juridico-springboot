package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminImportController {

    @GetMapping("/admin/importar")
    public String mostrarPaginaImportar(Model model) {
        model.addAttribute("activePage", "importar");
        return "views/admin/importar"; 
    }

    @PostMapping("/admin/importar")
    public String procesarImportacion(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            
            redirectAttributes.addFlashAttribute("mensaje", "Archivo cargado con éxito");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar: " + e.getMessage());
        }
        
        return "redirect:/admin/importar"; 
    }
}