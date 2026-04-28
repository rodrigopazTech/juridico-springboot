package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.AgendaService;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private GerenciaRepository gerenciaRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    @GetMapping
    public String index(Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(defaultValue = "audiencias") String tab,
            @RequestParam(defaultValue = "0") int page) {

        // 2. Obtener Datos PAGINADOS
        var audiencias = agendaService.getAudienciasPorFiltro(keyword, fechaInicio, fechaFin, gerenciaId, materiaId, page);
        var terminos = agendaService.getTerminosPorFiltro(keyword, fechaInicio, fechaFin, gerenciaId, materiaId, page);

        // 3. Cargar Modelo
        model.addAttribute("audiencias", audiencias);
        model.addAttribute("terminos", terminos);
        model.addAttribute("paramKeyword", keyword);

        // 4. UI y Filtros
        model.addAttribute("pageTitle", "Agenda / Historial");
        model.addAttribute("activePage", "agenda");
        model.addAttribute("activeTab", tab);
        
        model.addAttribute("paramFechaInicio", fechaInicio);
        model.addAttribute("paramFechaFin", fechaFin);
        model.addAttribute("paramGerenciaId", gerenciaId);
        model.addAttribute("paramMateriaId", materiaId);

        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findAll());

        return "views/agenda/index";
    }
}