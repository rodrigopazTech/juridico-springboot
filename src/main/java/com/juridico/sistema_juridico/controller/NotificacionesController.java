package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionesController {

    @Autowired
    private NotificacionRepository notificacionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TerminoRepository terminoRepository;

    // Listado general de notificaciones (Solo No Leídas)
    @GetMapping
    public String index(Model model, @RequestParam(value = "page", defaultValue = "0") int page) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        Page<Notificacion> notificaciones = notificacionRepository
                .findByUsuarioAndLeidaFalseOrderByCreatedAtDesc(usuario, PageRequest.of(page, 20));

        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("esPapelera", false);
        model.addAttribute("pageTitle", "Centro de Notificaciones");
        model.addAttribute("activePage", "notificaciones");

        return "views/alertas/historial";
    }

    // Papelera de reciclaje (Solo Leídas)
    @GetMapping("/papelera")
    public String papelera(Model model, @RequestParam(value = "page", defaultValue = "0") int page) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        Page<Notificacion> notificaciones = notificacionRepository
                .findByUsuarioAndLeidaTrueOrderByCreatedAtDesc(usuario, PageRequest.of(page, 20));

        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("esPapelera", true);
        model.addAttribute("pageTitle", "Papelera de Notificaciones");
        model.addAttribute("activePage", "notificaciones");

        return "views/alertas/historial";
    }

    @Transactional
    @GetMapping("/vaciar")
    public String vaciarPapelera() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        
        notificacionRepository.deleteByUsuarioAndLeidaTrue(usuario);
        
        return "redirect:/notificaciones/papelera";
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
                String rawId = notificacion.getEntidadId();
                String idClean = extraerSoloId(rawId);

                if ("AUDIENCIA".equals(notificacion.getEntidadTipo())) {
                    return "redirect:/audiencias?id=" + idClean;
                } else if ("TERMINO".equals(notificacion.getEntidadTipo())) {
                    Termino termino = terminoRepository.findById(Integer.valueOf(idClean)).orElse(null);
                    EstatusTermino estatusActual = termino != null ? termino.getEstatusTermino() : null;
                    if (estatusActual != null) {
                        return "redirect:/terminos?id=" + idClean + "&estatus=" + estatusActual;
                    }
                    return "redirect:/terminos?id=" + idClean;
                } else if ("EXPEDIENTE".equals(notificacion.getEntidadTipo())) {
                    return "redirect:/expedientes/" + idClean;
                } else if ("SEGURIDAD".equals(notificacion.getEntidadTipo())) {
                    return "redirect:/perfil?tab=seguridad";
                }
            }
        }
        return "redirect:/notificaciones";
    }

    @GetMapping("/archivar/{id}")
    public String archivarNotificacion(@PathVariable("id") Integer id, HttpServletRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Notificacion notificacion = notificacionRepository.findById(id).orElse(null);

        if (notificacion != null && notificacion.getUsuario().getEmail().equals(email)) {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(LocalDateTime.now());
            notificacionRepository.save(notificacion);
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.contains("/leer/")) {
            return "redirect:" + referer;
        }
        return "redirect:/notificaciones";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurarNotificacion(@PathVariable("id") Integer id, HttpServletRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Notificacion notificacion = notificacionRepository.findById(id).orElse(null);

        if (notificacion != null && notificacion.getUsuario().getEmail().equals(email)) {
            notificacion.setLeida(false);
            notificacion.setFechaLeida(null);
            notificacionRepository.save(notificacion);
        }

        String referer = request.getHeader("Referer");
        if (referer != null) {
            return "redirect:" + referer;
        }
        return "redirect:/notificaciones/papelera";
    }

    /**
     * Limpia el ID en caso de que se haya guardado una URL malformada.
     * Ejemplo: "/audiencias?keyword=3" -> "3"
     */
    private String extraerSoloId(String rawId) {
        if (rawId == null) return "";
        if (rawId.contains("=")) {
            return rawId.substring(rawId.lastIndexOf("=") + 1);
        }
        if (rawId.contains("/")) {
            return rawId.substring(rawId.lastIndexOf("/") + 1);
        }
        return rawId;
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarNotificacion(@PathVariable("id") Integer id, HttpServletRequest request) {
        notificacionRepository.deleteById(id);
        
        // Intentar volver a la página anterior (Papelera o Historial)
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/papelera")) {
            return "redirect:/notificaciones/papelera";
        }
        return "redirect:/notificaciones";
    }

    /**
     * API REST: Devuelve las notificaciones NO leídas del usuario autenticado (máx. 5).
     * Usado por el frontend para polling y mostrar el popup de campana.
     */
    @GetMapping("/api/recientes")
    @ResponseBody
    public ResponseEntity<?> getNotificacionesRecientes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
        }

        List<Notificacion> noLeidas = notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(usuario.getId());

        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter fmtHora  = DateTimeFormatter.ofPattern("HH:mm");

        List<Map<String, Object>> resultado = noLeidas.stream()
            .limit(5)
            .map(n -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id",       n.getId());
                item.put("titulo",   n.getTitulo());
                item.put("mensaje",  n.getMensaje());
                item.put("tipo",     n.getTipo() != null ? n.getTipo() : "SISTEMA");
                item.put("prioridad",n.getPrioridad() != null ? n.getPrioridad().getNombre() : null);
                item.put("fecha",    n.getNotificarEn() != null ? n.getNotificarEn().format(fmtFecha) : "");
                item.put("hora",     n.getNotificarEn() != null ? n.getNotificarEn().format(fmtHora)  : "");
                item.put("leida",    n.getLeida());
                return item;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("total",          noLeidas.size());
        response.put("notificaciones", resultado);
        return ResponseEntity.ok(response);
    }
}