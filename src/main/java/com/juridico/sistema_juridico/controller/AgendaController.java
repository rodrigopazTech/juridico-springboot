package com.juridico.sistema_juridico.controller;


import com.juridico.sistema_juridico.service.AgendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


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
                        @RequestParam(defaultValue = "audiencias") String tab,
                        @RequestParam(defaultValue = "0") int page) { // Nuevo Param

        int anioReal = java.time.LocalDate.now().getYear();
        int anioSeleccionado = (anio != null) ? anio : anioReal;

        // 2. Obtener Datos PAGINADOS
        // Nota: Usamos la misma variable 'page' para ambas listas. 
        // Si el usuario cambia de pestaña, volverá a la pagina que indique la URL o 0.
        var audiencias = agendaService.getAudienciasPorFiltro(filtro, mes, anioSeleccionado, page);
        var terminos = agendaService.getTerminosPorFiltro(filtro, mes, anioSeleccionado, page);

        // 3. Cargar Modelo
        model.addAttribute("audiencias", audiencias);
        model.addAttribute("terminos", terminos);
        
        // 4. UI y Filtros
        model.addAttribute("pageTitle", "Agenda General");
        model.addAttribute("activePage", "agenda");
        model.addAttribute("activeTab", tab);
        model.addAttribute("filtroActual", filtro);
        model.addAttribute("mesActual", mes);
        model.addAttribute("anioActual", anioSeleccionado); 

        model.addAttribute("listaAnios", java.util.stream.IntStream
                .rangeClosed(anioReal - 5, anioReal)
                .boxed()
                .sorted(java.util.Collections.reverseOrder())
                .toList());

        return "views/agenda/index";
    }
}