package com.juridico.sistema_juridico.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.service.configuracion.PerfilAtencionService;

@Controller
@RequestMapping("/administracion/plazos-alertas")
@PreAuthorize("isAuthenticated()")
public class PlazosAlertasController {

    private final PerfilAtencionService perfiles;

    public PlazosAlertasController(PerfilAtencionService perfiles) {
        this.perfiles = perfiles;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("perfilesTermino", perfiles.listarTerminos());
        model.addAttribute("perfilesAudiencia", perfiles.listarAudiencias());
        model.addAttribute("nuevoPerfilTermino", PerfilAlertaTermino.builder()
                .modo(ModoPerfilAlerta.DIAS_RESTANTES)
                .amarilloDias(5).rojoDias(2)
                .amarilloPorcentaje(50).amarilloMinimoDias(3).amarilloMaximoDias(10)
                .rojoPorcentaje(20).rojoMinimoDias(1).rojoMaximoDias(4)
                .version(1).estado(EstadoPerfil.BORRADOR).build());
        model.addAttribute("nuevoPerfilAudiencia", PerfilRecordatorioAudiencia.builder()
                .anticipacionAmarillaHoras(72).anticipacionRojaHoras(24)
                .recordatoriosMinutos("10080,4320,1440,120")
                .version(1).estado(EstadoPerfil.BORRADOR).build());
        model.addAttribute("modosPerfil", ModoPerfilAlerta.values());
        model.addAttribute("pageTitle", "Plazos y alertas");
        model.addAttribute("activePage", "plazos-alertas");
        return "views/admin/plazos-alertas";
    }

    @PostMapping("/terminos")
    public String guardarTermino(@ModelAttribute PerfilAlertaTermino perfil, RedirectAttributes flash) {
        try {
            perfiles.guardarTermino(perfil);
            flash.addFlashAttribute("mensajeExito", "Perfil de Término guardado como borrador.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#terminos";
    }

    @PostMapping("/audiencias")
    public String guardarAudiencia(@ModelAttribute PerfilRecordatorioAudiencia perfil, RedirectAttributes flash) {
        try {
            perfiles.guardarAudiencia(perfil);
            flash.addFlashAttribute("mensajeExito", "Perfil de Audiencia guardado como borrador.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#audiencias";
    }

    @PostMapping("/terminos/{id}/publicar")
    public String publicarTermino(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> perfiles.publicarTermino(id), "Perfil de Término publicado.", flash);
    }

    @PostMapping("/audiencias/{id}/publicar")
    public String publicarAudiencia(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> perfiles.publicarAudiencia(id), "Perfil de Audiencia publicado.", flash);
    }

    @PostMapping("/terminos/{id}/inactivar")
    public String inactivarTermino(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> perfiles.inactivarTermino(id), "Perfil de Término inactivado.", flash);
    }

    @PostMapping("/audiencias/{id}/inactivar")
    public String inactivarAudiencia(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> perfiles.inactivarAudiencia(id), "Perfil de Audiencia inactivado.", flash);
    }

    private String ejecutar(Runnable accion, String mensaje, RedirectAttributes flash) {
        try {
            accion.run();
            flash.addFlashAttribute("mensajeExito", mensaje);
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas";
    }
}
