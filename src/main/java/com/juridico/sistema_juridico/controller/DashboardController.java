package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.dto.response.dashboard.EstadisticasResponse;
import com.juridico.sistema_juridico.dto.response.dashboard.MetricasResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @GetMapping
    public String dashboard(Model model) {
        
        // 1. Configuración básica de la página
        model.addAttribute("pageTitle", "Panel Principal");
        model.addAttribute("activePage", "dashboard"); // IMPORTANTE: Debe coincidir con el sidebar

        // 2. DATOS MOCK (CIMIENTOS PARA TU COMPAÑERA)
        // Ella podrá ver estos números en pantalla y luego tú conectarás la BD real.
        
        // A) KPIs Generales
        EstadisticasResponse stats = EstadisticasResponse.builder()
                .totalExpedientes(150)
                .expedientesActivos(45)
                .audienciasProgramadas(3)
                .terminosActivos(12)
                .build();
        model.addAttribute("kpis", stats);

        // B) Datos para Gráfica (Ej: Carga de trabajo)
        MetricasResponse metrica = new MetricasResponse();
        metrica.setEtiquetas(Arrays.asList("Enero", "Febrero", "Marzo", "Abril"));
        metrica.setValores(Arrays.asList(10, 25, 15, 30));
        metrica.setMetricaNombre("Expedientes Nuevos 2026");
        model.addAttribute("graficaData", metrica);

        return "views/dashboard/index";
    }
}