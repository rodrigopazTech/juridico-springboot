package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.DashboardService;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final GerenciaRepository gerenciaRepository;

    public DashboardController(DashboardService dashboardService, GerenciaRepository gerenciaRepository) {
        this.dashboardService = dashboardService;
        this.gerenciaRepository = gerenciaRepository;
    }

    @GetMapping
    public String dashboard(Model model, @RequestParam(required = false) Long gerenciaId) {
        // Cargamos las gerencias para el select del filtro
        model.addAttribute("gerencias", gerenciaRepository.findAll());
        
        // Datos iniciales
        model.addAttribute("kpis", dashboardService.obtenerKpis(gerenciaId));
        model.addAttribute("dashboardData", dashboardService.obtenerMetricas(gerenciaId));
        model.addAttribute("gerenciaSeleccionada", gerenciaId);

        return "views/dashboard/index";
    }

    // Nuevo endpoint para actualización asíncrona (AJAX)
    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> obtenerDatosFiltrados(@RequestParam(required = false) Long gerenciaId) {
        return Map.of(
            "kpis", dashboardService.obtenerKpis(gerenciaId),
            "dashboardData", dashboardService.obtenerMetricas(gerenciaId)
        );
    }
}