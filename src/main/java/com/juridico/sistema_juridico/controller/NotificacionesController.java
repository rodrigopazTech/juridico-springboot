package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionesController {

    @Autowired
    private NotificacionRepository notificacionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Listado general de notificaciones (Historial)
    @GetMapping
    public String index(Model model, @RequestParam(value = "page", defaultValue = "0") int page) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        // Traemos las notificaciones del sistema
        Page<Notificacion> notificaciones = notificacionRepository
                .findByUsuarioOrderByCreatedAtDesc(usuario, PageRequest.of(page, 20));

        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("pageTitle", "Centro de Notificaciones");
        model.addAttribute("activePage", "notificaciones");

        return "views/alertas/historial"; // Apunta a la vista de historial
    }

    @GetMapping("/leer/{id}")
    public String leerNotificacion(@PathVariable("id") Integer id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Notificacion notificacion = notificacionRepository.findById(id).orElse(null);

        if (notificacion != null && notificacion.getUsuario().getEmail().equals(email)) {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(LocalDateTime.now());
            notificacionRepository.save(notificacion);

            if (notificacion.getEntidadId() != null) {
                if ("AUDIENCIA".equals(notificacion.getEntidadTipo())) {
                    return "redirect:/audiencias?id=" + notificacion.getEntidadId();
                } else if ("TERMINO".equals(notificacion.getEntidadTipo())) {
                    return "redirect:/terminos?id=" + notificacion.getEntidadId();
                } else if ("EXPEDIENTE".equals(notificacion.getEntidadTipo())) {
                    return "redirect:/expedientes/" + notificacion.getEntidadId();
                }
            }
        }
        return "redirect:/notificaciones"; // Redirige al historial si no hay destino
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarNotificacion(@PathVariable("id") Integer id) {
        notificacionRepository.deleteById(id);
        return "redirect:/notificaciones";
    }
}