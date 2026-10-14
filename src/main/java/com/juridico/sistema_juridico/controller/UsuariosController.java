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
    private com.juridico.sistema_juridico.service.AuditoriaTecnicaService auditoriaTecnicaService;
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
            @RequestParam(value = "q", defaultValue = "") String q,
            @RequestParam(value = "estado", defaultValue = "activos") String estado) {
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

                boolean soloActivos = !"inactivos".equalsIgnoreCase(estado);
        boolean estadoActivo = soloActivos;
        long totalActivos = 0;
        long totalInactivos = 0;

        if (!rolesConAccesoTotal.contains(usuarioActual.getRol())) {
            List<Integer> gerenciasIds = usuarioActual.getGerencias().stream().map(g -> g.getId()).toList();
            if (usuarioActual.getRol() == RolUsuario.GERENTE) {
                if (gerenciasIds.isEmpty()) {
                    usuariosPage = Page.empty(pageUs);
                } else if (hayBusqueda) {
                    usuariosPage = usuarioRepository.buscarPorKeywordYGerenciasInAndRolInAndActivoAndIdNot(q.trim(), gerenciasIds, rolesVisibles, estadoActivo, usuarioActual.getId(), pageUs);
                } else {
                    usuariosPage = usuarioRepository.findDistinctByGerenciasIdInAndRolInAndActivoAndIdNot(gerenciasIds, rolesVisibles, estadoActivo, usuarioActual.getId(), pageUs);
                }
                if (!gerenciasIds.isEmpty()) {
                    totalActivos = usuarioRepository.countDistinctByGerenciasIdInAndRolInAndActivoAndIdNot(gerenciasIds, rolesVisibles, true, usuarioActual.getId());
                    totalInactivos = usuarioRepository.countDistinctByGerenciasIdInAndRolInAndActivoAndIdNot(gerenciasIds, rolesVisibles, false, usuarioActual.getId());
                }
            } else {
                List<Integer> materiasIds = usuarioActual.getMaterias().stream().map(m -> m.getId()).toList();
                if (gerenciasIds.isEmpty() || materiasIds.isEmpty()) {
                    usuariosPage = Page.empty(pageUs);
                } else if (hayBusqueda) {
                    usuariosPage = usuarioRepository.buscarPorKeywordYGerenciasInAndMateriasInAndRolInAndActivoAndIdNot(q.trim(), gerenciasIds, materiasIds, rolesVisibles, estadoActivo, usuarioActual.getId(), pageUs);
                } else {
                    usuariosPage = usuarioRepository.findDistinctByGerenciasIdInAndMateriasIdInAndRolInAndActivoAndIdNot(gerenciasIds, materiasIds, rolesVisibles, estadoActivo, usuarioActual.getId(), pageUs);
                }
                if (!gerenciasIds.isEmpty() && !materiasIds.isEmpty()) {
                    totalActivos = usuarioRepository.countDistinctByGerenciasIdInAndMateriasIdInAndRolInAndActivoAndIdNot(gerenciasIds, materiasIds, rolesVisibles, true, usuarioActual.getId());
                    totalInactivos = usuarioRepository.countDistinctByGerenciasIdInAndMateriasIdInAndRolInAndActivoAndIdNot(gerenciasIds, materiasIds, rolesVisibles, false, usuarioActual.getId());
                }
            }
        } else {
            if (hayBusqueda) {
                usuariosPage = usuarioRepository.buscarPorKeywordAndRolInAndActivoAndIdNot(q.trim(), rolesVisibles, estadoActivo, usuarioActual.getId(), pageUs);
            } else {
                usuariosPage = usuarioRepository.findByRolInAndActivoAndIdNot(rolesVisibles, estadoActivo, usuarioActual.getId(), pageUs);
            }
            totalActivos = usuarioRepository.countByRolInAndActivoAndIdNot(rolesVisibles, true, usuarioActual.getId());
            totalInactivos = usuarioRepository.countByRolInAndActivoAndIdNot(rolesVisibles, false, usuarioActual.getId());
        }

        model.addAttribute("listaUsuarios", usuariosPage);
        model.addAttribute("q", q);
        model.addAttribute("estado", soloActivos ? "activos" : "inactivos");
        model.addAttribute("totalActivos", totalActivos);
        model.addAttribute("totalInactivos", totalInactivos);
        model.addAttribute("coincidenciasBusqueda",
                hayBusqueda ? construirCoincidenciasBusqueda(usuariosPage.getContent(), q.trim()) : Collections.emptyMap());

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
        Usuario guardado;
        try {
            guardado = usuarioService.guardarUsuario(usuario, gerenciasIds, materiasIds);
        } catch (IllegalArgumentException ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/usuarios";
        }
        syncService.sincronizarNombreUsuario(guardado);
        if (esNuevo) {
            if (guardado.getEmail() == null || guardado.getEmail().isBlank()) {
                redirectAttrs.addFlashAttribute("mensaje",
                        "Usuario creado correctamente. El nombre de usuario generado es " + guardado.getUsername() + ".");
                redirectAttrs.addFlashAttribute("tipo", "success");
                return "redirect:/usuarios";
            }
            if (emailMaintenance) {
                redirectAttrs.addFlashAttribute("mensaje", "Usuario creado. " + emailMaintenanceMessage);
                redirectAttrs.addFlashAttribute("tipo", "warning");
                return "redirect:/usuarios";
            }
            boolean enviado = passwordResetService.sendActivationLink(guardado, request);
            redirectAttrs.addFlashAttribute("mensaje", enviado
                    ? "Usuario creado. Se envio el enlace de activacion a " + guardado.getEmail() + "."
                    : "Usuario creado, pero no se pudo enviar el enlace de activacion. Revisa SMTP y reenvia el enlace.");
            redirectAttrs.addFlashAttribute("tipo", enviado ? "success" : "warning");
        } else {
            redirectAttrs.addFlashAttribute("mensaje", "Usuario guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        }
        return "redirect:/usuarios";
    }

        @GetMapping("/toggle/{id}")
    public String toggleStatusUsuario(@PathVariable("id") Integer id,
            @RequestParam(value = "estado", required = false) String estadoParam,
            RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioService.buscarPorId(id);
        validarPuedeVerUsuario(usuario);
        boolean nuevoEstado = !Boolean.TRUE.equals(usuario.getActivo());
        usuarioService.cambiarEstadoUsuario(id, nuevoEstado);
        String estado = nuevoEstado ? "activado" : "desactivado";
        redirectAttrs.addFlashAttribute("mensaje", "Usuario " + estado + " correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        String destino = estadoParam != null ? estadoParam : (nuevoEstado ? "activos" : "inactivos");
        return "redirect:/usuarios?estado=" + destino;
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        try {
            usuarioService.eliminarUsuario(id);
            redirectAttrs.addFlashAttribute("mensaje", "Usuario eliminado.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (com.juridico.sistema_juridico.exception.ConflictException ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
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

    private Map<Integer, Map<String, String>> construirCoincidenciasBusqueda(List<Usuario> usuarios, String query) {
        Map<Integer, Map<String, String>> coincidencias = new HashMap<>();
        String buscado = query.toLowerCase(java.util.Locale.ROOT);
        for (Usuario usuario : usuarios) {
            Map<String, String> coincidencia = primeraCoincidencia(usuario, buscado);
            if (!coincidencia.isEmpty()) {
                coincidencias.put(usuario.getId(), coincidencia);
            }
        }
        return coincidencias;
    }

    private Map<String, String> primeraCoincidencia(Usuario usuario, String buscado) {
        List<Map.Entry<String, String>> campos = List.of(
                Map.entry("Nombre de usuario", valor(usuario.getUsername())),
                Map.entry("Número de empleado", valor(usuario.getNumeroEmpleado())),
                Map.entry("Correo", valor(usuario.getEmail())),
                Map.entry("Nombre", valor(usuario.getNombreCompleto())),
                Map.entry("Rol", usuario.getRol() != null ? usuario.getRol().name() : ""));
        for (Map.Entry<String, String> campo : campos) {
            String texto = campo.getValue();
            String normalizado = texto.toLowerCase(java.util.Locale.ROOT);
            if (normalizado.contains(buscado)) {
                String relevancia = normalizado.equals(buscado) || normalizado.startsWith(buscado)
                        ? "Alta" : "Media";
                return Map.of("campo", campo.getKey(), "texto", texto, "relevancia", relevancia);
            }
        }
        return Collections.emptyMap();
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
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

    @PostMapping("/api/{id}/recuperacion-manual")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> generarCodigoRecuperacionManual(
            @PathVariable("id") Integer id,
            @RequestParam("motivo") String motivo,
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
            if (!Boolean.TRUE.equals(usuarioModificado.getActivo())) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                        .body(Map.of("mensaje", "No se puede generar un codigo de recuperacion porque el usuario se encuentra inactivo. Primero reactive la cuenta."));
            }

            String origen;
            if (usuarioModificado.getEmail() == null || usuarioModificado.getEmail().isBlank()) {
                origen = "SIN_CORREO_INSTITUCIONAL";
            } else if (emailMaintenance) {
                origen = "SERVICIO_CORREO_NO_DISPONIBLE";
            } else if (auditoriaTecnicaService.existeFalloRecienteDeCorreo(usuarioModificado.getId(),
                    java.time.Duration.ofMinutes(15))) {
                origen = "ENVIO_SMTP_FALLIDO";
            } else {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                        .body(Map.of("mensaje", "Primero envia el enlace al correo institucional. La recuperacion manual se habilita solo si el envio falla."));
            }

            com.juridico.sistema_juridico.service.PasswordResetService.ManualRecoveryCodeResult codigo =
                    passwordResetService.createManualRecoveryCode(usuarioModificado, actor, motivo, origen, request);

            auditoriaService.registrarActividadGeneral(usuarioModificado, "Codigo de Recuperacion Manual Emitido",
                    "Se emitio un codigo de un uso por el usuario: " + actor.getNombreCompleto(), "KEY");

            return org.springframework.http.ResponseEntity.ok(Map.of(
                    "mensaje", "El codigo no es una contrasena. El usuario debe capturarlo en Recuperacion manual y establecer una nueva contrasena.",
                    "codigo", codigo.code(),
                    "venceEn", codigo.expiresAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    "recoveryUrl", request.getContextPath() + "/auth/recuperacion-manual",
                    "origen", origen));
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
            if (!Boolean.TRUE.equals(usuario.getActivo())) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                        .body(Map.of("mensaje",
                                "No se puede recuperar la contrasena porque el usuario se encuentra inactivo. Primero reactive la cuenta.",
                                "manualRecoveryAvailable", false));
            }
            if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                return org.springframework.http.ResponseEntity.badRequest()
                        .body(Map.of("mensaje", "Este usuario no tiene correo institucional. Se puede usar recuperacion manual con codigo.",
                                "manualRecoveryAvailable", true));
            }
            if (emailMaintenance) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("mensaje", emailMaintenanceMessage, "manualRecoveryAvailable", true));
            }
            boolean enviado = passwordResetService.sendResetLinkForUser(usuario, request);
            if (!enviado) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_GATEWAY)
                        .body(Map.of("mensaje", "No se pudo enviar el enlace de restablecimiento. Ya puedes generar un codigo de recuperacion manual.",
                                "manualRecoveryAvailable", true));
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
