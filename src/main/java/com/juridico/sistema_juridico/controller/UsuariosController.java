package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.service.AuditoriaService;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping("/usuarios")
public class UsuariosController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private com.juridico.sistema_juridico.service.PasswordResetService passwordResetService;
    @Autowired
    private ExpedienteRepository expedienteRepository;
    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private TerminoRepository terminoRepository;
    @Autowired
    private ActividadExpedienteRepository actividadRepository;
    @Autowired
    private com.juridico.sistema_juridico.service.CatalogosSyncService syncService;

    @Value("${app.email.maintenance:false}")
    private boolean emailMaintenance;

    @Value("${app.email.maintenance-message:El servicio de correo esta en mantenimiento.}")
    private String emailMaintenanceMessage;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    @GetMapping
    public String index(Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "q", defaultValue = "") String q) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioActual = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado."));

        Pageable pageUs = PageRequest.of(page, 10, Sort.by("nombreCompleto").ascending());
        Page<Usuario> usuariosPage;

        // Roles con acceso total (ven todos los usuarios sin filtro por gerencia)
        java.util.Set<RolUsuario> rolesConAccesoTotal = java.util.Set.of(
                RolUsuario.ADMIN, RolUsuario.DIRECTOR_PRINCIPAL,
                RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR);

        List<RolUsuario> rolesVisibles = usuarioService.getRolesVisiblesPara(usuarioActual.getRol());
        if (usuarioActual.getRol() != RolUsuario.ADMIN) {
            rolesVisibles = rolesVisibles.stream()
                    .filter(rol -> rol != RolUsuario.ADMIN)
                    .toList();
        }
        boolean hayBusqueda = q != null && !q.trim().isEmpty();

        if (hayBusqueda) {
            if (!rolesConAccesoTotal.contains(usuarioActual.getRol())) {
                List<Integer> gerenciasIds = usuarioActual.getGerencias().stream().map(g -> g.getId()).toList();
                if (usuarioActual.getRol() == RolUsuario.GERENTE) {
                    if (gerenciasIds.isEmpty()) {
                        usuariosPage = Page.empty(pageUs);
                    } else {
                        usuariosPage = usuarioRepository.buscarPorKeywordYGerenciasInAndRolInAndIdNot(q.trim(), gerenciasIds, rolesVisibles, usuarioActual.getId(), pageUs);
                    }
                } else {
                    List<Integer> materiasIds = usuarioActual.getMaterias().stream().map(m -> m.getId()).toList();
                    if (gerenciasIds.isEmpty() || materiasIds.isEmpty()) {
                        usuariosPage = Page.empty(pageUs);
                    } else {
                        usuariosPage = usuarioRepository.buscarPorKeywordYGerenciasInAndMateriasInAndRolInAndIdNot(q.trim(), gerenciasIds, materiasIds, rolesVisibles, usuarioActual.getId(), pageUs);
                    }
                }
            } else {
                usuariosPage = usuarioRepository.buscarPorKeywordAndRolInAndIdNot(q.trim(), rolesVisibles, usuarioActual.getId(), pageUs);
            }
        } else {
            if (!rolesConAccesoTotal.contains(usuarioActual.getRol())) {
                List<Integer> gerenciasIds = usuarioActual.getGerencias().stream().map(g -> g.getId()).toList();
                if (usuarioActual.getRol() == RolUsuario.GERENTE) {
                    if (gerenciasIds.isEmpty()) {
                        usuariosPage = Page.empty(pageUs);
                    } else {
                        usuariosPage = usuarioRepository.findDistinctByGerenciasIdInAndRolInAndIdNot(gerenciasIds, rolesVisibles, usuarioActual.getId(), pageUs);
                    }
                } else {
                    List<Integer> materiasIds = usuarioActual.getMaterias().stream().map(m -> m.getId()).toList();
                    if (gerenciasIds.isEmpty() || materiasIds.isEmpty()) {
                        usuariosPage = Page.empty(pageUs);
                    } else {
                        usuariosPage = usuarioRepository.findDistinctByGerenciasIdInAndMateriasIdInAndRolInAndIdNot(gerenciasIds, materiasIds, rolesVisibles, usuarioActual.getId(), pageUs);
                    }
                }
            } else {
                usuariosPage = usuarioRepository.findByRolInAndIdNot(rolesVisibles, usuarioActual.getId(), pageUs);
            }
        }

        model.addAttribute("listaUsuarios", usuariosPage);
        model.addAttribute("q", q);

        if (!rolesConAccesoTotal.contains(usuarioActual.getRol()) && !usuarioActual.getGerencias().isEmpty()) {
            model.addAttribute("allGerencias", usuarioActual.getGerencias());
        } else {
            model.addAttribute("allGerencias", usuarioService.listarGerencias());
        }

        List<RolUsuario> rolesPermitidos = new ArrayList<>();
        if (usuarioActual.getRol() == RolUsuario.ADMIN) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.values()));
        } else if (usuarioActual.getRol() == RolUsuario.DIRECTOR_PRINCIPAL) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO));
        } else if (usuarioActual.getRol() == RolUsuario.DIRECTOR) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.SUBDIRECTOR, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO));
        } else if (usuarioActual.getRol() == RolUsuario.SUBDIRECTOR) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO));
        } else if (usuarioActual.getRol() == RolUsuario.GERENTE) {
            rolesPermitidos.addAll(Arrays.asList(RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO));
        } else if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            rolesPermitidos.add(RolUsuario.ABOGADO);
        }
        model.addAttribute("listaRoles", rolesPermitidos);

        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("puedeAdministrarUsuarios", puedeAdministrarUsuarios(usuarioActual));
        model.addAttribute("emailMaintenance", emailMaintenance);
        model.addAttribute("emailMaintenanceMessage", emailMaintenanceMessage);
        model.addAttribute("nuevoUsuario", new Usuario());
        model.addAttribute("pageTitle", "Gestión de Usuarios");
        model.addAttribute("activePage", "usuarios");

        return "views/usuarios/index";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
            @RequestParam(value = "gerenciasIds", required = false) List<Integer> gerenciasIds,
            @RequestParam(value = "materiasIds", required = false) List<Integer> materiasIds,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes redirectAttrs) {
        boolean esNuevo = usuario.getId() == null;
        Usuario guardado = usuarioService.guardarUsuario(usuario, gerenciasIds, materiasIds);
        syncService.sincronizarNombreUsuario(guardado);
        if (esNuevo) {
            if (emailMaintenance) {
                redirectAttrs.addFlashAttribute("mensaje", "Usuario creado. " + emailMaintenanceMessage);
                redirectAttrs.addFlashAttribute("tipo", "warning");
                return "redirect:/usuarios";
            }
            boolean enviado = passwordResetService.sendActivationLink(guardado, request);
            redirectAttrs.addFlashAttribute("mensaje", enviado
                    ? "Usuario creado. Se envio el enlace de activacion a " + guardado.getEmail() + "."
                    : "Usuario creado, pero no se pudo enviar el enlace de activacion. Revisa SMTP y reenvia el enlace.");
        } else {
            redirectAttrs.addFlashAttribute("mensaje", "Usuario guardado correctamente.");
        }
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios";
    }

    @GetMapping("/toggle/{id}")
    public String toggleStatusUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioService.buscarPorId(id);
        validarPuedeVerUsuario(usuario);
        usuario.setActivo(!usuario.getActivo());
        List<Integer> gerenciasIds = usuario.getGerencias().stream().map(Gerencia::getId).collect(Collectors.toList());
        List<Integer> materiasIds = usuario.getMaterias().stream().map(Materia::getId).collect(Collectors.toList());
        usuarioService.guardarUsuario(usuario, gerenciasIds, materiasIds);
        String estado = usuario.getActivo() ? "activado" : "desactivado";
        redirectAttrs.addFlashAttribute("mensaje", "Usuario " + estado + " correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        usuarioService.eliminarUsuario(id);
        redirectAttrs.addFlashAttribute("mensaje", "Usuario eliminado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios";
    }

    // ====================================================================
    // --- APIS PARA EL PANEL DE DETALLE (MAESTRO-DETALLE) ---
    // ====================================================================

    @GetMapping("/api/{id}/casos")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<Map<String, Object>>> obtenerCasosReal(
            @PathVariable("id") Integer id) {

        validarPuedeVerUsuario(usuarioService.buscarPorId(id));
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

        validarPuedeVerUsuario(usuarioService.buscarPorId(id));
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
            item.put("_raw_date", t.getFechaVencimiento());
            agenda.add(item);
        }

        // Ordenar por fecha cronológica
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

    @Autowired
    private com.juridico.sistema_juridico.repository.Usuarios.ActividadUsuarioRepository actividadUsuarioRepository;

    private String categorizarAccion(String titulo) {
        if (titulo == null) return "ACCIONES";
        String t = titulo.toUpperCase();
        if (t.contains("INICIO") || t.contains("CIERRE") || t.contains("SESIÓN") || t.contains("SESION") ||
            t.contains("CONTRASEÑA") || t.contains("CONTRASENA") || t.contains("ROL") || t.contains("PERMISOS") ||
            t.contains("ACTIVAR") || t.contains("DESACTIVAR") || t.contains("FALLIDO") || t.contains("BLOQUEO")) {
            return "SEGURIDAD";
        }
        if (t.contains("ASIGNADO") || t.contains("ASIGNACION") || t.contains("ASIGNACIÓN") || t.contains("RESPONSABLE") ||
            t.contains("DESASIGNADO") || t.contains("REASIGNADO")) {
            return "ASIGNACION";
        }
        if (t.contains("ARCHIVO") || t.contains("DOCUMENTO") || t.contains("PAPELERA") ||
            t.contains("ELIMINADO") || t.contains("RESTAURADO") || t.contains("SUBIDA") ||
            t.contains("UPLOAD") || t.contains("CARGADO")) {
            return "DOCUMENTAL";
        }
        return "ACCIONES";
    }

    @GetMapping("/api/{id}/historial")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<Map<String, Object>>> obtenerHistorialReal(
            @PathVariable("id") Integer id) {

        validarPuedeVerUsuario(usuarioService.buscarPorId(id));
        List<ActividadExpediente> actExp = actividadRepository.findTop100ByUsuarioIdOrderByFechaRegistroDesc(id);
        List<com.juridico.sistema_juridico.Entity.usuario.ActividadUsuario> actUs = actividadUsuarioRepository.findTop100ByUsuarioIdOrderByFechaRegistroDesc(id);

        List<Map<String, Object>> listaCombinada = new ArrayList<>();

        for (ActividadExpediente act : actExp) {
            Map<String, Object> map = new HashMap<>();
            map.put("accion", act.getTitulo());
            map.put("detalles", act.getDescripcion());
            map.put("expediente", act.getExpediente() != null ? act.getExpediente().getNumero() : "N/A");
            map.put("fecha", act.getFechaRegistro() != null ? act.getFechaRegistro().format(dateTimeFormatter) : "-");
            map.put("_raw_date", act.getFechaRegistro());
            map.put("categoria", categorizarAccion(act.getTitulo()));
            listaCombinada.add(map);
        }

        for (com.juridico.sistema_juridico.Entity.usuario.ActividadUsuario act : actUs) {
            Map<String, Object> map = new HashMap<>();
            map.put("accion", act.getTitulo());
            map.put("detalles", act.getDescripcion());
            map.put("expediente", "General");
            map.put("fecha", act.getFechaRegistro() != null ? act.getFechaRegistro().format(dateTimeFormatter) : "-");
            map.put("_raw_date", act.getFechaRegistro());
            map.put("categoria", categorizarAccion(act.getTitulo()));
            listaCombinada.add(map);
        }

        listaCombinada.sort((m1, m2) -> {
            java.time.LocalDateTime d1 = (java.time.LocalDateTime) m1.get("_raw_date");
            java.time.LocalDateTime d2 = (java.time.LocalDateTime) m2.get("_raw_date");
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
        });

        List<Map<String, Object>> response = listaCombinada.stream().limit(100).collect(Collectors.toList());

        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PostMapping("/api/password-temporal")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> actualizarPasswordTemporal(
            @RequestParam("id") Integer id,
            @RequestParam(value = "passwordTemporal", required = false) String passwordTemporal,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actor = usuarioRepository.findByEmail(actorEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado."));

            if (!puedeAdministrarUsuarios(actor)) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(Map.of("mensaje", "No tienes permisos para realizar esta acción."));
            }

            Usuario usuarioModificado = usuarioService.buscarPorId(id);
            validarPuedeVerUsuario(actor, usuarioModificado);
            usuarioService.validarPuedeAdministrarUsuario(actor, usuarioModificado);
            String temporal = passwordResetService.createTemporaryPassword(usuarioModificado, passwordTemporal, request);

            auditoriaService.registrarActividadGeneral(usuarioModificado, "Contrasena Temporal Generada",
                    "Se genero una contrasena temporal por el usuario: " + actor.getNombreCompleto(), "KEY");

            return org.springframework.http.ResponseEntity.ok(Map.of(
                    "mensaje", "Contrasena temporal creada. El usuario debera cambiarla al iniciar sesion.",
                    "passwordTemporal", temporal));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PostMapping("/api/{id}/enviar-restablecimiento")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> enviarRestablecimiento(@PathVariable("id") Integer id,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actor = usuarioRepository.findByEmail(actorEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado."));
            Usuario usuario = usuarioService.buscarPorId(id);
            validarPuedeVerUsuario(actor, usuario);
            if (emailMaintenance) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("mensaje", emailMaintenanceMessage));
            }
            boolean enviado = passwordResetService.sendResetLinkForUser(usuario, request);
            if (!enviado) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_GATEWAY)
                        .body(Map.of("mensaje", "No se pudo enviar el enlace de restablecimiento."));
            }

            auditoriaService.registrarActividadGeneral(usuario, "Enlace de Restablecimiento Enviado",
                    "Se envio enlace de restablecimiento por el usuario: " + actor.getNombreCompleto(), "KEY");

            return org.springframework.http.ResponseEntity.ok(Map.of(
                    "mensaje", "Enlace de restablecimiento enviado a " + usuario.getEmail() + "."));
        } catch (Exception ex) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "No se pudo enviar el enlace de restablecimiento."));
        }
    }

    @PostMapping("/api/{id}/reenviar-activacion")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> reenviarActivacion(@PathVariable("id") Integer id,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actor = usuarioRepository.findByEmail(actorEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado."));
            Usuario usuario = usuarioService.buscarPorId(id);
            validarPuedeVerUsuario(actor, usuario);
            if (emailMaintenance) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("mensaje", emailMaintenanceMessage));
            }
            boolean enviado = passwordResetService.sendActivationLink(usuario, request);
            if (enviado) {
                return org.springframework.http.ResponseEntity.ok(Map.of(
                        "mensaje", "Enlace de activacion enviado a " + usuario.getEmail() + "."));
            }
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_GATEWAY)
                    .body(Map.of("mensaje", "No se pudo enviar el enlace de activacion."));
        } catch (Exception ex) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "No se pudo reenviar el enlace de activacion."));
        }
    }

    private void validarPuedeVerUsuario(Usuario destino) {
        String actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario actor = usuarioRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado."));
        validarPuedeVerUsuario(actor, destino);
    }

    private void validarPuedeVerUsuario(Usuario actor, Usuario destino) {
        if (destino != null && destino.getRol() == RolUsuario.ADMIN && actor.getRol() != RolUsuario.ADMIN) {
            throw new ResourceNotFoundException("Usuario no encontrado.");
        }
    }

    private boolean puedeAdministrarUsuarios(Usuario usuario) {
        return usuario != null && List.of(
                RolUsuario.ADMIN,
                RolUsuario.DIRECTOR_PRINCIPAL,
                RolUsuario.DIRECTOR,
                RolUsuario.SUBDIRECTOR,
                RolUsuario.GERENTE,
                RolUsuario.JEFE_DEPTO).contains(usuario.getRol());
    }

    @GetMapping("/api/materias-por-gerencia/{ids}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<List<Materia>> getMateriasJson(@PathVariable("ids") List<Integer> ids) {
        try {
            java.util.Set<Materia> materiasCombinadas = new java.util.HashSet<>();
            for (Integer id : ids) {
                Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
                materiasCombinadas.addAll(gerencia.getMaterias());
            }
            return org.springframework.http.ResponseEntity.ok(new ArrayList<>(materiasCombinadas));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }
}
