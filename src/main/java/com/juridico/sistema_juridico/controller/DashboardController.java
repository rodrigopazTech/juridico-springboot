package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String dashboard(Model model) {

        model.addAttribute("kpis", dashboardService.obtenerKpis());
        model.addAttribute("dashboardData", dashboardService.obtenerMetricas());

        return "views/dashboard/index";
    }
}
