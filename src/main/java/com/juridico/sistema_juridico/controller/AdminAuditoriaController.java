package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin/auditoria")
@RequiredArgsConstructor
public class AdminAuditoriaController {

    private final AuditoriaTecnicaService auditoriaTecnicaService;

    @GetMapping
    public String index() {
        return "redirect:/admin/auditoria/resumen";
    }

    @GetMapping("/resumen")
    public String resumen(Model model) {
        base(model, "resumen");
        model.addAttribute("resumen", auditoriaTecnicaService.obtenerResumen());
        model.addAttribute("sesionesRecientes", auditoriaTecnicaService.sesionesRecientes());
        model.addAttribute("eventosRecientes", auditoriaTecnicaService.eventosRecientes());
        return "views/admin/auditoria";
    }

    @GetMapping("/sesiones")
    public String sesiones(@RequestParam(defaultValue = "0") int page, Model model) {
        base(model, "sesiones");
        Pageable pageable = PageRequest.of(Math.max(page, 0), 25);
        model.addAttribute("sesiones", auditoriaTecnicaService.sesiones(pageable));
        return "views/admin/auditoria";
    }

    @GetMapping("/eventos")
    public String eventos(@RequestParam(defaultValue = "0") int page, Model model) {
        base(model, "eventos");
        Pageable pageable = PageRequest.of(Math.max(page, 0), 50);
        model.addAttribute("eventos", auditoriaTecnicaService.eventos(pageable));
        return "views/admin/auditoria";
    }

    @GetMapping("/seguridad")
    public String seguridad(@RequestParam(defaultValue = "0") int page, Model model) {
        base(model, "seguridad");
        Pageable pageable = PageRequest.of(Math.max(page, 0), 50);
        model.addAttribute("eventos", auditoriaTecnicaService.eventosPorResultado(List.of("DENEGADO", "FALLIDO"), pageable));
        return "views/admin/auditoria";
    }

    @GetMapping("/errores")
    public String errores(@RequestParam(defaultValue = "0") int page, Model model) {
        base(model, "errores");
        Pageable pageable = PageRequest.of(Math.max(page, 0), 50);
        model.addAttribute("eventos", auditoriaTecnicaService.eventosPorResultado(List.of("ERROR"), pageable));
        return "views/admin/auditoria";
    }

    private void base(Model model, String vista) {
        model.addAttribute("activePage", "auditoria");
        model.addAttribute("vista", vista);
    }
}
