package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.service.AgendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    @Autowired
    private AgendaService agendaService;

  @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "hoy") String filtro,
                        @RequestParam(required = false) String mes,
                        @RequestParam(required = false) Integer anio, 
                        @RequestParam(defaultValue = "audiencias") String tab) {

        // 1. AÑO REAL (Del calendario) vs AÑO SELECCIONADO (Del filtro)
        int anioReal = java.time.LocalDate.now().getYear();
        int anioSeleccionado = (anio != null) ? anio : anioReal;

        // 2. Obtener Datos (Usando el año seleccionado por el usuario)
        List<AudienciaDesahogada> audiencias = agendaService.getAudienciasPorFiltro(filtro, mes, anioSeleccionado);
        List<Termino> terminos = agendaService.getTerminosPorFiltro(filtro, mes, anioSeleccionado);

        // 3. Cargar Modelo
        model.addAttribute("audiencias", audiencias);
        model.addAttribute("terminos", terminos);
        
        // 4. UI y Filtros
        model.addAttribute("pageTitle", "Agenda General");
        model.addAttribute("activePage", "agenda");
        model.addAttribute("activeTab", tab);
        model.addAttribute("filtroActual", filtro);
        model.addAttribute("mesActual", mes);
        
        // Este es el valor que tendrá el select seleccionado
        model.addAttribute("anioActual", anioSeleccionado); 

        // CORRECCIÓN AQUÍ: 
        // La lista siempre se genera desde el año REAL (2026), no el seleccionado.
        // Así el 2026 nunca desaparece de la lista.
        model.addAttribute("listaAnios", java.util.stream.IntStream
                .rangeClosed(anioReal - 5, anioReal) // De 2021 a 2026 (si hoy es 2026)
                .boxed()
                .sorted(java.util.Collections.reverseOrder())
                .toList());

        return "views/agenda/index";
    }
}