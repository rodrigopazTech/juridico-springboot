package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.dto.request.recordatorios.RecordatorioRequest;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.TareaService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/tareas")
public class TareasController {

    @Autowired
    private RecordatorioRepository recordatorioRepository;
    @Autowired
    private NotificacionRepository notificacionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TareaService tareaService;

    @GetMapping
    public String index(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        // Lista única de tareas: evita duplicar los mismos registros en tarjetas y tabla.
        List<Recordatorio> todos = recordatorioRepository.findByUsuarioOrderByFechaRecordatorioDesc(usuario);
        Map<String, java.time.LocalDateTime> avisosPorEntidad = new HashMap<>();
        for (Notificacion notificacion : notificacionRepository.findByUsuarioAndEventoOrderByNotificarEnDesc(
                usuario, NotificacionEvento.TAREA_RECORDATORIO)) {
            avisosPorEntidad.putIfAbsent(notificacion.getEntidadId(), notificacion.getNotificarEn());
        }
        Map<Integer, java.time.LocalDateTime> avisosEnviadosPorTarea = new HashMap<>();
        for (Recordatorio recordatorio : todos) {
            java.time.LocalDateTime notificadoEn = avisosPorEntidad.get(entidadIdRecordatorio(recordatorio));
            if (notificadoEn != null) {
                avisosEnviadosPorTarea.put(recordatorio.getId(), notificadoEn);
            }
        }

        model.addAttribute("recordatoriosTodos", todos);
        model.addAttribute("avisosEnviadosPorTarea", avisosEnviadosPorTarea);
        model.addAttribute("prioridades", tareaService.listarPrioridadesActivas());
        model.addAttribute("minutosAnticipacion", TareaService.MINUTOS_ANTICIPACION);
        model.addAttribute("pageTitle", "Mis Tareas");
        model.addAttribute("activePage", "tareas"); // Cambiamos el activePage

        return "views/tareas/index";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute RecordatorioRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            return redirigirConError(bindingResult.getAllErrors().get(0).getDefaultMessage(), redirectAttrs);
        }

        try {
            tareaService.crear(request, getUsuarioActual());
        } catch (BadRequestException ex) {
            return redirigirConError(ex.getMessage(), redirectAttrs);
        }

        redirectAttrs.addFlashAttribute("mensaje", "Tarea guardada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/tareas";
    }

    @PostMapping("/editar/{id}")
    public String editar(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute RecordatorioRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            return redirigirConError(bindingResult.getAllErrors().get(0).getDefaultMessage(), redirectAttrs);
        }

        try {
            tareaService.actualizar(id, request, getUsuarioActual());
        } catch (BadRequestException | ResourceNotFoundException ex) {
            return redirigirConError(ex.getMessage(), redirectAttrs);
        }

        redirectAttrs.addFlashAttribute("mensaje", "Tarea actualizada. El aviso se ajustó a la nueva fecha y hora.");
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
    public String eliminar(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Usuario usuario = getUsuarioActual();
        recordatorioRepository.findByIdAndUsuario(id, usuario).ifPresent(recordatorioRepository::delete);
        return "redirect:/tareas";
    }

    private Usuario getUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email).orElseThrow();
    }

    private String redirigirConError(String mensaje, RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("mensaje", mensaje);
        redirectAttrs.addFlashAttribute("tipo", "error");
        return "redirect:/tareas";
    }

    private String entidadIdRecordatorio(Recordatorio recordatorio) {
        int revision = recordatorio.getRecordatorioRevision() == null ? 0 : recordatorio.getRecordatorioRevision();
        return revision == 0 ? String.valueOf(recordatorio.getId()) : recordatorio.getId() + ":" + revision;
    }

}
