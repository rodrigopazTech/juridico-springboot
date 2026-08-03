package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.OutlookCalendarService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tareas")
public class TareasController {

    @Autowired
    private RecordatorioRepository recordatorioRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CatPrioridadRepository catPrioridadRepository;
    @Autowired
    private OutlookCalendarService outlookCalendarService;

    @GetMapping
    public String index(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        // 1. Tarjetas de arriba (Próximos 3 pendientes)
        List<Recordatorio> proximos = recordatorioRepository
                .findByUsuarioAndCompletadoFalseOrderByFechaRecordatorioAsc(usuario)
                .stream().limit(3).collect(Collectors.toList());

        // 2. Lista de abajo (Historial completo)
        List<Recordatorio> todos = recordatorioRepository.findByUsuarioOrderByFechaRecordatorioDesc(usuario);

        model.addAttribute("recordatorios", proximos);
        model.addAttribute("recordatoriosTodos", todos);
        model.addAttribute("prioridades", catPrioridadRepository.findAll());
        model.addAttribute("pageTitle", "Mis Tareas");
        model.addAttribute("activePage", "tareas"); // Cambiamos el activePage
        model.addAttribute("outlookCalendarLinked", outlookCalendarService.tieneCalendarioVinculado(usuario));

        return "views/tareas/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Recordatorio recordatorio,
            @RequestParam(value = "prioridadId", required = false) Integer prioridadId,
            @RequestParam(value = "syncOutlook", defaultValue = "false") boolean syncOutlook,
            RedirectAttributes redirectAttrs) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        recordatorio.setUsuario(usuario);
        recordatorio.setCreatedAt(LocalDateTime.now());
        recordatorio.setCompletado(false);

        // Buscar la entidad CatPrioridad por ID
        if (prioridadId != null) {
            catPrioridadRepository.findById(prioridadId).ifPresent(recordatorio::setPrioridad);
        }

        Recordatorio guardado = recordatorioRepository.save(recordatorio);
        sincronizarOutlookSiConfirmado(syncOutlook, usuario, guardado, redirectAttrs);

        redirectAttrs.addFlashAttribute("mensaje", "Tarea guardada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/tareas";
    }

    @GetMapping("/completar/{id}")
    public String completar(@PathVariable("id") Integer id) {
        Usuario usuario = getUsuarioActual();
        recordatorioRepository.findByIdAndUsuario(id, usuario).ifPresent(r -> {
            r.setCompletado(true);
            recordatorioRepository.save(r);
        });
        return "redirect:/tareas";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id,
            @RequestParam(value = "deleteOutlook", defaultValue = "false") boolean deleteOutlook,
            RedirectAttributes redirectAttrs) {
        Usuario usuario = getUsuarioActual();
        recordatorioRepository.findByIdAndUsuario(id, usuario).ifPresent(recordatorio -> {
            if (deleteOutlook && Boolean.TRUE.equals(recordatorio.getOutlookLinked())) {
                outlookCalendarService.eliminarEventoRecordatorio(usuario, recordatorio);
            }
            recordatorioRepository.delete(recordatorio);
        });
        return "redirect:/tareas";
    }

    private Usuario getUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email).orElseThrow();
    }

    private void sincronizarOutlookSiConfirmado(boolean syncOutlook, Usuario usuario, Recordatorio recordatorio,
            RedirectAttributes redirectAttrs) {
        if (!syncOutlook) {
            return;
        }
        try {
            outlookCalendarService.crearOActualizarEventoRecordatorio(usuario, recordatorio);
            redirectAttrs.addFlashAttribute("mensajeOutlook", "Recordatorio sincronizado con Outlook.");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensajeOutlook",
                    "El recordatorio se guardo en SIAJ, pero Outlook no pudo sincronizarse: " + ex.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "warning");
        }
    }
}
