package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.Expediente.ActividadExpedienteRepository;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.expediente.ActividadExpediente;

import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;

@Controller
@RequestMapping("/abogados")
public class AbogadosController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ExpedienteRepository expedienteRepository;
    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private TerminoRepository terminoRepository;
    @Autowired
    private ActividadExpedienteRepository actividadRepository;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    @GetMapping
    public String index(Model model, @RequestParam(value = "page", defaultValue = "0") int page) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioActual = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado."));

        Pageable pageUs = PageRequest.of(page, 10, Sort.by("nombreCompleto").ascending());
        Page<Usuario> usuariosPage;

        if (usuarioActual.getRol() == RolUsuario.GERENTE && usuarioActual.getGerencia() != null) {
            usuariosPage = usuarioRepository.findByGerencia(usuarioActual.getGerencia(), pageUs);
        } else {
            usuariosPage = usuarioService.listarUsuariosPaginados(pageUs);
        }

        model.addAttribute("listaUsuarios", usuariosPage);

        if (usuarioActual.getRol() == RolUsuario.GERENTE) {
            model.addAttribute("allGerencias", Arrays.asList(usuarioActual.getGerencia()));
            model.addAttribute("listaRoles", Arrays.asList(RolUsuario.ABOGADO, RolUsuario.JEFE_DEPTO));
        } else {
            model.addAttribute("allGerencias", usuarioService.listarGerencias());
            model.addAttribute("listaRoles", RolUsuario.values());
        }

        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("nuevoUsuario", new Usuario());
        model.addAttribute("pageTitle", "Gestión de Abogados");
        model.addAttribute("activePage", "abogados");

        return "views/abogados/index";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam(value = "materiasIds", required = false) List<Integer> materiasIds,
            RedirectAttributes redirectAttrs) {
        usuarioService.guardarUsuario(usuario, materiasIds);
        redirectAttrs.addFlashAttribute("mensaje", "Abogado guardado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/abogados";
    }

    @GetMapping("/toggle/{id}")
    public String toggleStatusUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioService.buscarPorId(id);
        usuario.setActivo(!usuario.getActivo());
        List<Integer> materiasIds = usuario.getMaterias().stream().map(Materia::getId).collect(Collectors.toList());
        usuarioService.guardarUsuario(usuario, materiasIds);
        String estado = usuario.getActivo() ? "activado" : "desactivado";
        redirectAttrs.addFlashAttribute("mensaje", "Abogado " + estado + " correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/abogados";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        usuarioService.eliminarUsuario(id);
        redirectAttrs.addFlashAttribute("mensaje", "Abogado eliminado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/abogados";
    }

    // ====================================================================
    // --- APIS PARA EL PANEL DE DETALLE (MAESTRO-DETALLE) ---
    // ====================================================================

    @GetMapping("/api/{id}/casos")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<Map<String, Object>>> obtenerCasosReal(
            @PathVariable("id") Integer id) {

        List<Expediente> expedientes = expedienteRepository.findAllByAbogadoResponsableId(id);
        List<Map<String, Object>> response = expedientes.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("numero", e.getNumero());
            map.put("asunto", e.getDescripcion() != null ? (e.getDescripcion().length() > 50 ? e.getDescripcion().substring(0, 47) + "..." : e.getDescripcion()) : "Sin asunto");
            map.put("cliente", e.getPartes() != null ? e.getPartes() : "No especificado");
            map.put("estatus", e.getEtapaProcesal() != null ? e.getEtapaProcesal().toString() : "N/A");
            map.put("fecha", e.getCreatedAt() != null ? e.getCreatedAt().format(dateFormatter) : "-");
            return map;
        }).collect(Collectors.toList());

        return org.springframework.http.ResponseEntity.ok(response);
    }

    @GetMapping("/api/{id}/agenda")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<Map<String, Object>>> obtenerAgendaReal(
            @PathVariable("id") Integer id) {

        List<Map<String, Object>> agenda = new ArrayList<>();

        // Cargar Audiencias
        List<Audiencia> audiencias = audienciaRepository.findByUsuarioId(id);
        for (Audiencia a : audiencias) {
            Map<String, Object> item = new HashMap<>();
            item.put("tipo", "Audiencia");
            item.put("descripcion", (a.getTipoAudiencia() != null ? a.getTipoAudiencia().getNombre() : "Audiencia") + 
                                   " - Exp: " + (a.getExpediente() != null ? a.getExpediente().getNumero() : "S/N"));
            
            String fechaStr = "";
            if (a.getFechaAudiencia() != null) {
                fechaStr = a.getFechaAudiencia().format(dateFormatter);
                if (a.getHoraAudiencia() != null) {
                    fechaStr += " " + a.getHoraAudiencia().toString();
                }
            }
            item.put("fecha", fechaStr);
            item.put("estatus", a.getEstatusAudiencia() != null ? a.getEstatusAudiencia().toString() : "PENDIENTE");
            item.put("_raw_date", a.getFechaAudiencia());
            agenda.add(item);
        }

        // Cargar Términos
        List<Termino> terminos = terminoRepository.findByUsuarioId(id);
        for (Termino t : terminos) {
            Map<String, Object> item = new HashMap<>();
            item.put("tipo", "Término");
            item.put("descripcion", (t.getAsunto() != null ? t.getAsunto() : "Término") + 
                                   " - Exp: " + (t.getExpediente() != null ? t.getExpediente().getNumero() : "S/N"));
            item.put("fecha", t.getFechaVencimiento() != null ? t.getFechaVencimiento().format(dateFormatter) : "-");
            item.put("estatus", t.getEstatusTermino() != null ? t.getEstatusTermino().toString() : "ACTIVO");
            // Guardamos la fecha cruda para ordenar
            item.put("_raw_date", t.getFechaVencimiento());
            agenda.add(item);
        }

        // Ordenar por fecha (más reciente/próximo primero o cronológico)
        agenda.sort((m1, m2) -> {
            Object d1 = m1.get("_raw_date");
            Object d2 = m2.get("_raw_date");
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            if (d1 instanceof java.time.LocalDate && d2 instanceof java.time.LocalDate) {
                return ((java.time.LocalDate) d1).compareTo((java.time.LocalDate) d2);
            }
            return 0;
        });

        return org.springframework.http.ResponseEntity.ok(agenda);
    }

    @GetMapping("/api/{id}/historial")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<Map<String, Object>>> obtenerHistorialReal(
            @PathVariable("id") Integer id) {

        List<ActividadExpediente> actividades = actividadRepository.findTop15ByUsuarioIdOrExpedienteAbogadoResponsableIdOrderByFechaRegistroDesc(id, id);
        List<Map<String, Object>> response = actividades.stream().map(act -> {
            Map<String, Object> map = new HashMap<>();
            map.put("accion", act.getTitulo());
            map.put("detalles", act.getDescripcion());
            map.put("expediente", act.getExpediente() != null ? act.getExpediente().getNumero() : "General");
            map.put("fecha", act.getFechaRegistro() != null ? act.getFechaRegistro().format(dateTimeFormatter) : "-");
            return map;
        }).collect(Collectors.toList());

        return org.springframework.http.ResponseEntity.ok(response);
    }
}