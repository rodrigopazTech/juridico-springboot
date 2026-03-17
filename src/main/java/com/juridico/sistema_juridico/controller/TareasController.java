package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
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
        model.addAttribute("pageTitle", "Mis Tareas");
        model.addAttribute("activePage", "tareas"); // Cambiamos el activePage

        return "views/tareas/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Recordatorio recordatorio, RedirectAttributes redirectAttrs) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        recordatorio.setUsuario(usuario);
        recordatorio.setCreatedAt(LocalDateTime.now());
        recordatorio.setCompletado(false);
        recordatorioRepository.save(recordatorio);

        redirectAttrs.addFlashAttribute("mensaje", "Tarea guardada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/tareas";
    }

    @GetMapping("/completar/{id}")
    public String completar(@PathVariable("id") Integer id) {
        recordatorioRepository.findById(id).ifPresent(r -> {
            r.setCompletado(true);
            recordatorioRepository.save(r);
        });
        return "redirect:/tareas";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id) {
        recordatorioRepository.deleteById(id);
        return "redirect:/tareas";
    }
}