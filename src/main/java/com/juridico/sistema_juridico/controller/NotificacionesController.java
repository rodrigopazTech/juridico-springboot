package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.NotificacionService; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest; 
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/alertas")
public class NotificacionesController {

    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private RecordatorioRepository recordatorioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private NotificacionService notificacionService;

    // VISTA PRINCIPAL UNIFICADA
   @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        // Ahora sí funcionará porque notificacionService ya existe
        var notificacionesPage = notificacionService.listarMisNotificaciones(usuario, PageRequest.of(page, 10));
        
        model.addAttribute("notificaciones", notificacionesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notificacionesPage.getTotalPages());

        List<Recordatorio> recordatorios = recordatorioRepository.findByUsuarioAndCompletadoFalseOrderByFechaRecordatorioAsc(usuario);
        model.addAttribute("recordatorios", recordatorios);

        return "views/alertas/index";
    }

    // GUARDAR RECORDATORIO (Y GENERAR NOTIFICACIÓN AUTOMÁTICA)
    @PostMapping("/recordatorios/guardar")
    public String guardarRecordatorio(@ModelAttribute Recordatorio recordatorio, RedirectAttributes redirectAttrs) {
         try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
            
            recordatorio.setUsuario(usuario);
            recordatorio.setCreatedAt(LocalDateTime.now());
            recordatorio.setCompletado(false); 
            
            Recordatorio guardado = recordatorioRepository.save(recordatorio);

            Notificacion notif = Notificacion.builder()
                    .usuario(usuario)
                    .titulo("Recordatorio: " + guardado.getTitulo())
                    .mensaje(guardado.getDetalles())
                    .tipo("RECORDATORIO")
                    .prioridad(guardado.getPrioridad())
                    .entidadTipo("RECORDATORIO")
                    .entidadId(guardado.getId().toString())
                    .notificarEn(LocalDateTime.of(guardado.getFechaRecordatorio(), LocalTime.of(8, 0))) 
                    .leida(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificacionRepository.save(notif);

            redirectAttrs.addFlashAttribute("mensaje", "Recordatorio programado exitosamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Error al guardar recordatorio.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/alertas?tab=recordatorios";
    }

    // Marcar como leída y redirigir al origen
    @GetMapping("/notificaciones/leer/{id}")
    public String leerNotificacion(@PathVariable Integer id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        Notificacion notificacion = notificacionRepository.findById(id).orElse(null);
        
        if (notificacion != null && notificacion.getUsuario().getEmail().equals(email)) {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(LocalDateTime.now());
            notificacionRepository.save(notificacion);
            
            if ("AUDIENCIA".equals(notificacion.getEntidadTipo()) && notificacion.getEntidadId() != null) {
                return "redirect:/audiencias?keyword=" + notificacion.getEntidadId();                
            }
        }
        return "redirect:/alertas";
    }

    // ELIMINAR NOTIFICACIÓN
   @GetMapping("/notificaciones/eliminar/{id}")
    public String eliminarNotificacion(@PathVariable Integer id) {
        notificacionRepository.deleteById(id);
        return "redirect:/alertas";
    }

    // COMPLETAR RECORDATORIO
    @GetMapping("/recordatorios/completar/{id}")
    public String completarRecordatorio(@PathVariable Integer id) {
        Recordatorio r = recordatorioRepository.findById(id).orElse(null);
        if(r != null) {
            r.setCompletado(true);
            recordatorioRepository.save(r);
        }
        return "redirect:/alertas?tab=recordatorios";
    }
    
    // ELIMINAR RECORDATORIO
    @GetMapping("/recordatorios/eliminar/{id}")
    public String eliminarRecordatorio(@PathVariable Integer id) {
        recordatorioRepository.deleteById(id);
        return "redirect:/alertas?tab=recordatorios";
    }

    
}