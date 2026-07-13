package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.service.scheduler.RecordatorioNotificacionScheduler;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    @Autowired
    private ExpedienteRepository expedienteRepository;
    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private RecordatorioNotificacionScheduler recordatorioNotificacionScheduler;

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
    public String leerNotificacion(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Notificacion notificacion = notificacionRepository.findById(id).orElse(null);

        if (notificacion != null && notificacion.getUsuario().getEmail().equals(email)) {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(LocalDateTime.now());
            notificacionRepository.save(notificacion);

            if (notificacion.getEntidadId() != null) {
                String rawId = notificacion.getEntidadId();
                String idClean = extraerSoloId(rawId);
                String tipo = notificacion.getEntidadTipo();

                if (!tieneAccesoAEntidad(notificacion.getUsuario(), tipo, idClean)) {
                    redirectAttrs.addFlashAttribute("mensaje", "Ya no tienes acceso a esta " + (tipo != null ? tipo.toLowerCase() : "información") + " o el registro ya no existe.");
                    redirectAttrs.addFlashAttribute("tipo", "warning");
                    return "redirect:/notificaciones";
                }

                if ("AUDIENCIA".equals(tipo)) {
                    return "redirect:/audiencias?id=" + idClean;
                } else if ("TERMINO".equals(tipo)) {
                    try {
                        Integer termId = Integer.valueOf(idClean);
                        Termino termino = terminoRepository.findById(termId).orElse(null);
                        EstatusTermino estatusActual = termino != null ? termino.getEstatusTermino() : null;
                        if (estatusActual != null) {
                            return "redirect:/terminos?id=" + termId + "&estatus=" + estatusActual;
                        }
                        return "redirect:/terminos?id=" + termId;
                    } catch (NumberFormatException e) {
                        return "redirect:/terminos?keyword=" + idClean;
                    }
                } else if ("EXPEDIENTE".equals(tipo)) {
                    return "redirect:/expedientes/" + idClean;
                } else if ("OBSERVACION".equals(tipo)) {
                    return "redirect:/expedientes/" + idClean + "?openObservaciones=true";
                } else if ("SEGURIDAD".equals(tipo)) {
                    return "redirect:/perfil?tab=seguridad";
                }
            }
        }
        return "redirect:/notificaciones";
    }

    private boolean tieneAccesoAEntidad(Usuario usuario, String tipo, String idStr) {
        if (usuario == null || tipo == null || idStr == null) {
            return false;
        }

        if (usuario.getRol() == RolUsuario.ADMIN || 
            usuario.getRol() == RolUsuario.DIRECTOR || 
            usuario.getRol() == RolUsuario.SUBDIRECTOR) {
            return true;
        }

        try {
            if ("EXPEDIENTE".equals(tipo) || "OBSERVACION".equals(tipo)) {
                UUID expId = UUID.fromString(idStr);
                Expediente exp = expedienteRepository.findById(expId).orElse(null);
                if (exp == null) return false;
                return tieneAccesoAExpediente(usuario, exp);
            } else if ("TERMINO".equals(tipo)) {
                Integer termId = Integer.valueOf(idStr);
                Termino term = terminoRepository.findById(termId).orElse(null);
                if (term == null) return false;
                
                Expediente exp = term.getExpediente();
                if (exp != null) {
                    if (!tieneAccesoAExpediente(usuario, exp)) {
                        return false;
                    }
                    if (usuario.getRol() == RolUsuario.ABOGADO) {
                        boolean esResponsableTermino = term.getAbogadoResponsable() != null && term.getAbogadoResponsable().getId().equals(usuario.getId());
                        boolean esResponsableExp = exp.getAbogadoResponsable() != null && exp.getAbogadoResponsable().getId().equals(usuario.getId());
                        boolean esInvitado = audienciaRepository.findExpedientesByAbogadoInvitadoActivo(usuario.getId()).stream()
                                .anyMatch(e -> e.getId().equals(exp.getId()));
                        return esResponsableTermino || esResponsableExp || esInvitado;
                    }
                    return true;
                } else {
                    com.juridico.sistema_juridico.Entity.procesal.TerminoContexto ctx = term.getContexto();
                    if (ctx == null) return false;
                    
                    if (usuario.getRol() == RolUsuario.ABOGADO) {
                        return term.getAbogadoResponsable() != null && term.getAbogadoResponsable().getId().equals(usuario.getId());
                    }
                    
                    if (usuario.getGerencia() == null || ctx.getGerencia() == null ||
                        !usuario.getGerencia().getId().equals(ctx.getGerencia().getId())) {
                        return false;
                    }
                    
                    if (usuario.getRol() == RolUsuario.JEFE_DEPTO) {
                        if (ctx.getMateria() == null) return false;
                        Integer matId = ctx.getMateria().getId();
                        return usuario.getMaterias().stream().anyMatch(m -> m.getId().equals(matId));
                    }
                    return true;
                }
            } else if ("AUDIENCIA".equals(tipo)) {
                Integer audId = Integer.valueOf(idStr);
                Audiencia aud = audienciaRepository.findById(audId).orElse(null);
                if (aud == null) return false;
                
                Expediente exp = aud.getExpediente();
                if (exp == null) return false;
                
                if (!tieneAccesoAExpediente(usuario, exp)) {
                    return false;
                }
                
                if (usuario.getRol() == RolUsuario.ABOGADO) {
                    boolean esCompareciente = aud.getAbogadoComparece() != null && aud.getAbogadoComparece().getId().equals(usuario.getId());
                    boolean esResponsableExp = exp.getAbogadoResponsable() != null && exp.getAbogadoResponsable().getId().equals(usuario.getId());
                    boolean esInvitado = audienciaRepository.findExpedientesByAbogadoInvitadoActivo(usuario.getId()).stream()
                            .anyMatch(e -> e.getId().equals(exp.getId()));
                    return esCompareciente || esResponsableExp || esInvitado;
                }
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private boolean tieneAccesoAExpediente(Usuario usuario, Expediente exp) {
        if (usuario == null || exp == null) return false;
        
        if (usuario.getRol() == RolUsuario.ADMIN || 
            usuario.getRol() == RolUsuario.DIRECTOR || 
            usuario.getRol() == RolUsuario.SUBDIRECTOR) {
            return true;
        }
        
        if (usuario.getGerencia() != null && (exp.getGerencia() == null || !exp.getGerencia().getId().equals(usuario.getGerencia().getId()))) {
            if (usuario.getRol() != RolUsuario.ABOGADO) {
                return false;
            }
        }
        
        if (usuario.getRol() == RolUsuario.JEFE_DEPTO) {
            if (exp.getMateria() == null) return false;
            Integer matId = exp.getMateria().getId();
            return usuario.getMaterias().stream().anyMatch(m -> m.getId().equals(matId));
        }
        
        if (usuario.getRol() == RolUsuario.ABOGADO) {
            boolean esResponsable = exp.getAbogadoResponsable() != null && exp.getAbogadoResponsable().getId().equals(usuario.getId());
            boolean esInvitado = audienciaRepository.findExpedientesByAbogadoInvitadoActivo(usuario.getId()).stream()
                    .anyMatch(e -> e.getId().equals(exp.getId()));
            return esResponsable || esInvitado;
        }
        
        return true;
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