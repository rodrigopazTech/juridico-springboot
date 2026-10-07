package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.catalogo.*;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.Catalogo.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/catalogos")
public class CatalogosController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
    @Autowired private TipoAudienciaRepository tipoAudienciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private EstadoRepository estadoRepository;
    @Autowired private TipoExpedienteRepository tipoExpedienteRepository;
    @Autowired private EtapaCatalogoRepository etapaCatalogoRepository;
    @Autowired private com.juridico.sistema_juridico.service.CatalogosSyncService syncService;
    @Autowired private com.juridico.sistema_juridico.service.catalogo.TipoTerminoService tipoTerminoService;
    @Autowired private com.juridico.sistema_juridico.service.catalogo.TipoAudienciaService tipoAudienciaService;
    @Autowired private com.juridico.sistema_juridico.service.configuracion.PerfilAtencionService perfilAtencionService;

    // ===============================================================
    // VISTA PRINCIPAL
    // ===============================================================
    @GetMapping
    public String index(Model model, @RequestParam(value = "gerenciaId", required = false) Integer gerenciaId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioActual = usuarioRepository.findByEmail(email).orElseThrow();
        RolUsuario rol = usuarioActual.getRol();

        // --- Determinar quÃ© gerencias puede ver ---
        boolean esNivelAlto = (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR);
        boolean puedeEditar = usuarioActual.getGerencia() != null || esNivelAlto;

        List<Gerencia> gerenciasDisponibles;
        Gerencia gerenciaSeleccionada = null;

        if (esNivelAlto) {
            gerenciasDisponibles = gerenciaRepository.findAll();
        } else {
            // Gerente, Jefe o Abogado: Solo su gerencia
            Gerencia miGerencia = usuarioActual.getGerencia();
            if (miGerencia != null) {
                gerenciasDisponibles = List.of(miGerencia);
                gerenciaId = miGerencia.getId(); // Forzar selecciÃ³n a su gerencia
            } else {
                gerenciasDisponibles = List.of();
            }
        }

        // Si se seleccionÃ³ una gerencia (o fue forzada)
        if (gerenciaId != null) {
            gerenciaSeleccionada = gerenciaRepository.findById(gerenciaId).orElse(null);
        } else if (!gerenciasDisponibles.isEmpty()) {
            // Auto-seleccionar la primera gerencia
            gerenciaSeleccionada = gerenciasDisponibles.get(0);
        }

        // --- Cargar catÃ¡logos filtrados por la gerencia seleccionada ---
        List<OrganoJurisdiccional> organos = new ArrayList<>();
        List<TipoAudiencia> tiposAudiencia = new ArrayList<>();
        List<Usuario> abogados = new ArrayList<>();
        List<Estado> sedes = new ArrayList<>();
        List<Materia> materias = new ArrayList<>();
        List<TipoExpediente> tiposExpediente = new ArrayList<>();
        List<EtapaCatalogo> etapasCatalogo = new ArrayList<>();

        if (gerenciaSeleccionada != null) {
            organos = organoRepository.findByGerenciaIdAndActivoTrue(gerenciaSeleccionada.getId());
            // Incluir Ã³rganos sin gerencia asignada
            organos.addAll(organoRepository.findByGerenciaIsNullAndActivoTrue());
            // Eliminar duplicados por id
            organos = organos.stream()
                .collect(Collectors.collectingAndThen(
                    Collectors.toMap(OrganoJurisdiccional::getId, o -> o, (a, b) -> a),
                    m -> new ArrayList<>(m.values())
                ));
            organos.sort(Comparator.comparing(OrganoJurisdiccional::getNombre, String.CASE_INSENSITIVE_ORDER));

            tiposAudiencia = tipoAudienciaRepository.findByGerenciaIdAndActivoTrue(gerenciaSeleccionada.getId());
            // Incluir tipos sin gerencia asignada (globales)
            tiposAudiencia.addAll(tipoAudienciaRepository.findByGerenciaIsNullAndActivoTrue());
            tiposAudiencia = tiposAudiencia.stream()
                .collect(Collectors.collectingAndThen(
                    Collectors.toMap(TipoAudiencia::getId, t -> t, (a, b) -> a),
                    m -> new ArrayList<>(m.values())
                ));
            tiposAudiencia.sort(Comparator.comparing(TipoAudiencia::getNombre, String.CASE_INSENSITIVE_ORDER));

            final Integer gIdFinal = gerenciaSeleccionada.getId();
            abogados = usuarioRepository.findAll().stream()
                .filter(u -> u.getGerencia() != null && u.getGerencia().getId().equals(gIdFinal))
                .sorted(Comparator.comparing(Usuario::getNombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

            sedes = estadoRepository.findByGerenciaIdAndActivoTrue(gerenciaSeleccionada.getId());
            // Incluir sedes sin gerencia
            sedes.addAll(estadoRepository.findByGerenciaIsNullAndActivoTrue());
            sedes = sedes.stream()
                .collect(Collectors.collectingAndThen(
                    Collectors.toMap(Estado::getId, e -> e, (a, b) -> a),
                    m -> new ArrayList<>(m.values())
                ));
            sedes.sort(Comparator.comparing(Estado::getNombre, String.CASE_INSENSITIVE_ORDER));
            
            materias = materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(gerenciaSeleccionada.getId());
            if (!materias.isEmpty()) {
                etapasCatalogo = etapaCatalogoRepository.findByMateriaIdInOrderByMateriaNombreAscOrdenAscNombreAsc(
                        materias.stream().map(Materia::getId).toList());
                etapasCatalogo = ordenarEtapasJerarquicas(etapasCatalogo);
            }
            
            tiposExpediente = tipoExpedienteRepository.findByGerenciaIdAndActivoTrue(gerenciaSeleccionada.getId());
            tiposExpediente.addAll(tipoExpedienteRepository.findByGerenciaIsNullAndActivoTrue());
            tiposExpediente = tiposExpediente.stream()
                .collect(Collectors.collectingAndThen(
                    Collectors.toMap(TipoExpediente::getId, t -> t, (a, b) -> a),
                    m -> new ArrayList<>(m.values())
                ));
            tiposExpediente.sort(Comparator.comparing(TipoExpediente::getNombre, String.CASE_INSENSITIVE_ORDER));
        }

        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("esNivelAlto", esNivelAlto);
        model.addAttribute("puedeEditar", puedeEditar);
        model.addAttribute("gerenciasDisponibles", gerenciasDisponibles);
        model.addAttribute("gerenciaSeleccionada", gerenciaSeleccionada);
        model.addAttribute("listaOrganos", organos);
        model.addAttribute("listaTiposAudiencia", tiposAudiencia);
        model.addAttribute("listaAbogados", abogados);
        model.addAttribute("listaSedes", sedes);
        model.addAttribute("listaMaterias", materias);
        model.addAttribute("listaTiposExpediente", tiposExpediente);
        model.addAttribute("listaEtapasCatalogo", etapasCatalogo);
        model.addAttribute("listaTiposTermino", tipoTerminoService.listarTodos());
        model.addAttribute("perfilesTerminoDias", perfilAtencionService.publicados(ModoPerfilAlerta.DIAS_RESTANTES));
        model.addAttribute("perfilesTerminoProporcionales", perfilAtencionService.publicados(ModoPerfilAlerta.PROPORCIONAL_LIMITADO));
        model.addAttribute("perfilesAudiencia", perfilAtencionService.audienciasPublicadas());
        model.addAttribute("modosPlazoTermino", ModoPlazoTermino.values());
        
        model.addAttribute("fechaLimiteNuevo", java.time.LocalDateTime.now().minusDays(7));

        model.addAttribute("pageTitle", "Cat\u00e1logos");
        model.addAttribute("activePage", "catalogos");

        return "views/catalogos/index";
    }

    // ===============================================================
    // CRUD - Ã“RGANOS JURISDICCIONALES
    // ===============================================================
    private Usuario getUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email).orElseThrow();
    }

    private boolean esNivelAlto(Usuario usuario) {
        RolUsuario rol = usuario.getRol();
        return rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR;
    }

    private void validarPuedeEditarCatalogos(Usuario usuarioActual) {
        if (!esNivelAlto(usuarioActual) && usuarioActual.getGerencia() == null) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes una gerencia asignada para modificar catÃ¡logos.");
        }
    }

    private void validarAlcanceGerencia(Gerencia gerenciaCatalogo, Usuario usuarioActual, String catalogo) {
        validarPuedeEditarCatalogos(usuarioActual);
        if (esNivelAlto(usuarioActual)) {
            return;
        }
        if (gerenciaCatalogo == null || usuarioActual.getGerencia() == null
                || !gerenciaCatalogo.getId().equals(usuarioActual.getGerencia().getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No puedes modificar " + catalogo + " fuera de tu gerencia.");
        }
    }

    private void validarPuedeModificarGerencia(Usuario usuarioActual, Gerencia gerencia) {
        validarPuedeEditarCatalogos(usuarioActual);
        if (!esNivelAlto(usuarioActual)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo ADMIN y directivos pueden modificar gerencias.");
        }
    }

    private Gerencia resolverGerenciaPermitida(String gerenciaIdStr, Usuario usuarioActual) {
        validarPuedeEditarCatalogos(usuarioActual);
        if (!esNivelAlto(usuarioActual)) {
            return usuarioActual.getGerencia();
        }
        if (gerenciaIdStr == null || gerenciaIdStr.isBlank()) {
            return null;
        }
        return gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null);
    }

    private Materia validarMateriaPermitida(Integer materiaId, Usuario usuarioActual) {
        validarPuedeEditarCatalogos(usuarioActual);
        Materia materia = materiaRepository.findById(materiaId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada."));
        validarAlcanceGerencia(materia.getGerencia(), usuarioActual, "etapas procesales");
        return materia;
    }

    private String normalizarClaveEtapa(String nombre) {
        if (nombre == null) {
            return "ETAPA";
        }
        String clave = java.text.Normalizer.normalize(nombre.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return clave.isBlank() ? "ETAPA" : clave;
    }

    private ResponseEntity<?> forbidden(String message) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(Map.of("error", message));
    }

    @PostMapping("/organos/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarOrgano(@RequestBody Map<String, String> payload) {
        try {
            String nombre = payload.get("nombre");
            String idStr = payload.get("id");
            String gerenciaIdStr = payload.get("gerenciaId");

            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio."));
            }

            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuarioActual = usuarioRepository.findByEmail(email).orElseThrow();
            Gerencia gerenciaPermitida = resolverGerenciaPermitida(gerenciaIdStr, usuarioActual);

            OrganoJurisdiccional organo;
            if (idStr != null && !idStr.isEmpty()) {
                // EDICIÃ“N
                organo = usuarioService.buscarOrganoPorId(Integer.parseInt(idStr));
                validarAlcanceGerencia(organo.getGerencia(), usuarioActual, "organos jurisdiccionales");
                organo.setNombre(nombre.trim());
            } else {
                // CREACIÃ“N: Primero verificar si ya existe uno ACTIVO
                Optional<OrganoJurisdiccional> activoOpt = organoRepository.findByNombreIgnoreCase(nombre.trim())
                    .filter(o -> Boolean.TRUE.equals(o.getActivo()));
                if (activoOpt.isPresent()) {
                    return ResponseEntity.ok(Map.of("status", "exists", "id", activoOpt.get().getId(), "mensaje", "El Ã“rgano Jurisdiccional ya existe."));
                }

                // CREACIÃ“N: buscar si ya existe uno inactivo con el mismo nombre
                organo = organoRepository.findByNombreIgnoreCase(nombre.trim())
                    .filter(o -> Boolean.FALSE.equals(o.getActivo()))
                    .orElse(null);

                if (organo != null) {
                    // Reactivar el existente
                    organo.setActivo(true);
                    organo.setUsuario(usuarioActual);
                    organo.setGerencia(gerenciaPermitida);
                } else {
                    organo = new OrganoJurisdiccional();
                    organo.setNombre(nombre.trim());
                    organo.setActivo(true);
                    organo.setUsuario(usuarioActual);
                    organo.setGerencia(gerenciaPermitida);
                }
            }

            usuarioService.guardarOrgano(organo);
            syncService.sincronizarNombreOrgano(organo);
            return ResponseEntity.ok(Map.of("status", "ok", "id", organo.getId(), "mensaje", "Ã“rgano guardado correctamente."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/organos/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarOrgano(@PathVariable("id") Integer id) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            OrganoJurisdiccional organoActual = usuarioService.buscarOrganoPorId(id);
            validarAlcanceGerencia(organoActual.getGerencia(), usuarioActual, "organos jurisdiccionales");
            usuarioService.eliminarOrgano(id);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Ã“rgano eliminado correctamente."));
        } catch (DataIntegrityViolationException e) {
            OrganoJurisdiccional o = usuarioService.buscarOrganoPorId(id);
            o.setActivo(false);
            organoRepository.save(o);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "El valor estaba en uso. Ha sido ocultado del catÃ¡logo."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }

    // ===============================================================
    // CRUD - TIPOS DE AUDIENCIA
    // ===============================================================
    @PostMapping("/tipos-audiencia/guardar-completo")
    public String guardarTipoAudienciaCompleto(@ModelAttribute TipoAudiencia tipo,
            @RequestParam(required = false) Integer gerenciaId, RedirectAttributes ra) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            validarPuedeEditarCatalogos(usuarioActual);
            Gerencia gerencia = resolverGerenciaPermitida(gerenciaId == null ? null : gerenciaId.toString(), usuarioActual);
            tipo.setGerencia(gerencia);
            tipo.setUsuario(usuarioActual);
            tipoAudienciaService.guardar(tipo);
            ra.addFlashAttribute("mensajeExito", "Tipo de Audiencia guardado correctamente.");
        } catch (Exception ex) {
            ra.addFlashAttribute("mensajeError", "No se pudo guardar el Tipo de Audiencia: " + ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#audiencias";
    }

    @DeleteMapping("/tipos-audiencia/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarTipoAudiencia(@PathVariable("id") Integer id) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            TipoAudiencia tipoActual = usuarioService.buscarTipoAudienciaPorId(id);
            validarAlcanceGerencia(tipoActual.getGerencia(), usuarioActual, "tipos de audiencia");
            usuarioService.eliminarTipoAudiencia(id);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Tipo de Audiencia eliminado correctamente."));
        } catch (DataIntegrityViolationException e) {
            TipoAudiencia t = usuarioService.buscarTipoAudienciaPorId(id);
            t.setActivo(false);
            tipoAudienciaRepository.save(t);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "El valor estaba en uso. Ha sido ocultado del catÃ¡logo."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }

    // ===============================================================
    // CRUD - TIPOS DE EXPEDIENTE
    // ===============================================================
    @PostMapping("/tipos-expediente/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarTipoExpediente(@RequestBody Map<String, String> payload) {
        try {
            String nombre = payload.get("nombre");
            String idStr = payload.get("id");
            String gerenciaIdStr = payload.get("gerenciaId");

            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio."));
            }

            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuarioActual = usuarioRepository.findByEmail(email).orElseThrow();

            Gerencia gerenciaPermitida = resolverGerenciaPermitida(gerenciaIdStr, usuarioActual);

            TipoExpediente tipo;
            if (idStr != null && !idStr.isEmpty()) {
                // EDICIÃ“N
                tipo = tipoExpedienteRepository.findById(Integer.parseInt(idStr))
                        .orElseThrow(() -> new RuntimeException("Tipo de Expediente no encontrado"));
                validarAlcanceGerencia(tipo.getGerencia(), usuarioActual, "tipos de expediente");
                tipo.setNombre(nombre.trim());
            } else {
                // CREACIÃ“N
                tipo = tipoExpedienteRepository.findByNombreIgnoreCase(nombre.trim())
                    .filter(t -> Boolean.FALSE.equals(t.getActivo()))
                    .orElse(null);

                if (tipo != null) {
                    tipo.setActivo(true);
                    tipo.setUsuario(usuarioActual);
                    tipo.setGerencia(gerenciaPermitida);
                } else {
                    tipo = new TipoExpediente();
                    tipo.setNombre(nombre.trim());
                    tipo.setActivo(true);
                    tipo.setUsuario(usuarioActual);
                    tipo.setGerencia(gerenciaPermitida);
                }
            }

            tipoExpedienteRepository.save(tipo);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Tipo de Expediente guardado correctamente."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/tipos-expediente/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarTipoExpediente(@PathVariable("id") Integer id) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            TipoExpediente tipoActual = tipoExpedienteRepository.findById(id).orElseThrow();
            validarAlcanceGerencia(tipoActual.getGerencia(), usuarioActual, "tipos de expediente");
            tipoExpedienteRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Tipo de Expediente eliminado correctamente."));
        } catch (DataIntegrityViolationException e) {
            TipoExpediente t = tipoExpedienteRepository.findById(id).orElseThrow();
            t.setActivo(false);
            tipoExpedienteRepository.save(t);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "El valor estaba en uso. Ha sido ocultado del catÃ¡logo."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }

    // ===============================================================
    // CRUD - SEDES/ESTADOS
    // ===============================================================
    @PostMapping("/sedes/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarSede(@RequestBody Map<String, String> payload) {
        try {
            String nombre = payload.get("nombre");
            String idStr = payload.get("id");
            String gerenciaIdStr = payload.get("gerenciaId");

            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio."));
            }

            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuarioActual = usuarioRepository.findByEmail(email).orElseThrow();

            Gerencia gerenciaPermitida = resolverGerenciaPermitida(gerenciaIdStr, usuarioActual);

            Estado estado;
            if (idStr != null && !idStr.isEmpty()) {
                // EDICIÃ“N
                estado = estadoRepository.findById(Integer.parseInt(idStr))
                    .orElseThrow(() -> new RuntimeException("Sede no encontrada."));
                validarAlcanceGerencia(estado.getGerencia(), usuarioActual, "sedes");
                estado.setNombre(nombre.trim());
            } else {
                // CREACIÃ“N: Primero verificar si ya existe uno ACTIVO
                Optional<Estado> activoOpt = estadoRepository.findByNombreIgnoreCase(nombre.trim())
                    .filter(e -> Boolean.TRUE.equals(e.getActivo()));
                if (activoOpt.isPresent()) {
                    return ResponseEntity.ok(Map.of("status", "exists", "id", activoOpt.get().getId(), "mensaje", "La Sede/Estado ya existe."));
                }

                // CREACIÃ“N: buscar si ya existe uno inactivo con el mismo nombre
                estado = estadoRepository.findByNombreIgnoreCase(nombre.trim())
                    .filter(e -> Boolean.FALSE.equals(e.getActivo()))
                    .orElse(null);

                if (estado != null) {
                    // Reactivar el existente
                    estado.setActivo(true);
                    estado.setUsuario(usuarioActual);
                    estado.setGerencia(gerenciaPermitida);
                } else {
                    estado = new Estado();
                    estado.setNombre(nombre.trim());
                    estado.setActivo(true);
                    estado.setUsuario(usuarioActual);
                    estado.setGerencia(gerenciaPermitida);
                }
            }

            String nombreViejo = (idStr != null && !idStr.isEmpty()) ? 
                estadoRepository.findById(Integer.parseInt(idStr)).map(Estado::getNombre).orElse(null) : null;

            estadoRepository.save(estado);
            
            if (nombreViejo != null && !nombreViejo.equals(estado.getNombre())) {
                syncService.sincronizarNombreSede(nombreViejo, estado.getNombre());
            }
            
            return ResponseEntity.ok(Map.of("status", "ok", "id", estado.getId(), "mensaje", "Sede/Estado guardado correctamente."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/sedes/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarSede(@PathVariable("id") Integer id) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            Estado sedeActual = estadoRepository.findById(id).orElseThrow();
            validarAlcanceGerencia(sedeActual.getGerencia(), usuarioActual, "sedes");
            estadoRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Sede/Estado eliminado correctamente."));
        } catch (DataIntegrityViolationException e) {
            Estado s = estadoRepository.findById(id).orElseThrow();
            s.setActivo(false);
            estadoRepository.save(s);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "El valor estaba en uso. Ha sido ocultado del catÃ¡logo."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }

    // ===============================================================
    // CRUD - GERENCIAS (conservado para admin)
    // ===============================================================
    @PostMapping("/gerencias/guardar")
    public String guardarGerencia(@ModelAttribute Gerencia gerencia, RedirectAttributes redirectAttrs) {
        validarPuedeModificarGerencia(getUsuarioActual(), gerencia);
        usuarioService.guardarGerencia(gerencia);
        redirectAttrs.addFlashAttribute("mensaje", "Gerencia guardada.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/catalogos?gerenciaId=" + gerencia.getId();
    }

    // ===============================================================
    // CRUD - MATERIAS
    // ===============================================================
    @PostMapping("/materias/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarMateriaCat(@RequestBody Map<String, String> payload) {
        try {
            String nombre = payload.get("nombre");
            String idStr = payload.get("id");
            String gerenciaIdStr = payload.get("gerenciaId");
            Integer gerenciaId = (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) ? Integer.parseInt(gerenciaIdStr) : null;

            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio."));
            }

            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuarioActual = usuarioRepository.findByEmail(email).orElseThrow();

            Gerencia gerenciaPermitida = resolverGerenciaPermitida(gerenciaIdStr, usuarioActual);
            Integer gerenciaPermitidaId = gerenciaPermitida != null ? gerenciaPermitida.getId() : null;
            gerenciaId = gerenciaPermitidaId;

            Materia materia;
            if (idStr != null && !idStr.isEmpty()) {
                // EDICIÃ“N
                materia = materiaRepository.findById(Integer.parseInt(idStr))
                    .orElseThrow(() -> new RuntimeException("Materia no encontrada."));
                validarAlcanceGerencia(materia.getGerencia(), usuarioActual, "materias");
                materia.setNombre(nombre.trim());
            } else {
                
                // CREACIÃ“N: Primero verificar si ya existe uno ACTIVO (filtrado por gerencia si aplica)
                if (gerenciaId != null) {
                    Optional<Materia> activoOpt = materiaRepository.findByNombreIgnoreCaseAndGerenciaId(nombre.trim(), gerenciaId)
                        .filter(m -> Boolean.TRUE.equals(m.getActivo()));
                    if (activoOpt.isPresent()) {
                        return ResponseEntity.ok(Map.of("status", "exists", "id", activoOpt.get().getId(), "mensaje", "La Materia ya existe en esta Gerencia."));
                    }
                } else {
                    Optional<Materia> activoOpt = materiaRepository.findByNombreIgnoreCase(nombre.trim())
                        .filter(m -> Boolean.TRUE.equals(m.getActivo()));
                    if (activoOpt.isPresent()) {
                        return ResponseEntity.ok(Map.of("status", "exists", "id", activoOpt.get().getId(), "mensaje", "La Materia ya existe."));
                    }
                }

                // CREACIÃ“N: buscar si existe inactiva (filtrada por gerencia si aplica, de lo contrario global)
                if (gerenciaId != null) {
                    materia = materiaRepository.findByNombreIgnoreCaseAndGerenciaId(nombre.trim(), gerenciaId)
                        .filter(m -> Boolean.FALSE.equals(m.getActivo()))
                        .orElse(null);
                } else {
                    materia = materiaRepository.findByNombreIgnoreCase(nombre.trim())
                        .filter(m -> Boolean.FALSE.equals(m.getActivo()))
                        .orElse(null);
                }

                if (materia != null) {
                    materia.setActivo(true);
                    materia.setUsuario(usuarioActual);
                    materia.setGerencia(gerenciaPermitida);
                } else {
                    materia = new Materia();
                    materia.setNombre(nombre.trim());
                    materia.setActivo(true);
                    materia.setUsuario(usuarioActual);
                    materia.setGerencia(gerenciaPermitida);
                }
            }

            materiaRepository.save(materia);

            // Auto-asignar la nueva materia al usuario creador si es rol limitado
            boolean esCreacion = (idStr == null || idStr.isEmpty());
            if (esCreacion) {
                if (usuarioActual.getRol() == RolUsuario.ABOGADO ||
                    usuarioActual.getRol() == RolUsuario.JEFE_DEPTO ||
                    usuarioActual.getRol() == RolUsuario.GERENTE) {
                    usuarioActual.getMaterias().add(materia);
                    usuarioRepository.save(usuarioActual);
                }

                // Asignar automÃ¡ticamente a todos los Gerentes activos de esta gerencia
                if (gerenciaId != null) {
                    List<Usuario> gerentes = usuarioRepository.findByRolAndGerenciasIdAndActivoTrue(RolUsuario.GERENTE, gerenciaId);
                    for (Usuario g : gerentes) {
                        if (!g.getId().equals(usuarioActual.getId())) {
                            g.getMaterias().add(materia);
                            usuarioRepository.save(g);
                        }
                    }
                }
            }

            return ResponseEntity.ok(Map.of("status", "ok", "id", materia.getId(), "mensaje", "Materia guardada correctamente."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/materias/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarMateriaCat(@PathVariable("id") Integer id) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            Materia materiaActual = materiaRepository.findById(id).orElseThrow();
            validarAlcanceGerencia(materiaActual.getGerencia(), usuarioActual, "materias");
            materiaRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Materia eliminada correctamente."));
        } catch (DataIntegrityViolationException e) {
            Materia m = materiaRepository.findById(id).orElseThrow();
            m.setActivo(false);
            materiaRepository.save(m);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "La materia estaba en uso. Ha sido ocultada del catÃ¡logo."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }

    // ===============================================================
    // CRUD - ETAPAS PROCESALES POR MATERIA
    // ===============================================================
    @GetMapping("/etapas/materia/{materiaId}")
    @ResponseBody
    public ResponseEntity<?> listarEtapasPorMateria(@PathVariable("materiaId") Integer materiaId) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            validarMateriaPermitida(materiaId, usuarioActual);
            List<Map<String, Object>> etapas = etapaCatalogoRepository
                    .findByMateriaIdOrderByOrdenAscNombreAsc(materiaId)
                    .stream()
                    .map(this::mapearEtapaCatalogo)
                    .toList();
            return ResponseEntity.ok(etapas);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/etapas/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarEtapa(@RequestBody Map<String, String> payload) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            String nombre = payload.get("nombre");
            String idStr = payload.get("id");
            String materiaIdStr = payload.get("materiaId");
            String parentIdStr = payload.get("parentId");

            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre de la etapa es obligatorio."));
            }
            if (materiaIdStr == null || materiaIdStr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Selecciona una materia."));
            }

            Materia materia = validarMateriaPermitida(Integer.parseInt(materiaIdStr), usuarioActual);
            EtapaCatalogo parent = null;
            if (parentIdStr != null && !parentIdStr.isBlank()) {
                parent = etapaCatalogoRepository.findById(Integer.parseInt(parentIdStr))
                        .orElseThrow(() -> new IllegalArgumentException("Etapa padre no encontrada."));
                if (!parent.getMateria().getId().equals(materia.getId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "La subetapa debe pertenecer a la misma materia."));
                }
                if (parent.getParent() != null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Solo puedes crear subetapas dentro de etapas principales."));
                }
            }

            EtapaCatalogo etapa;
            if (idStr != null && !idStr.isBlank()) {
                etapa = etapaCatalogoRepository.findById(Integer.parseInt(idStr))
                        .orElseThrow(() -> new IllegalArgumentException("Etapa no encontrada."));
                validarMateriaPermitida(etapa.getMateria().getId(), usuarioActual);
                if (parent != null && parent.getId().equals(etapa.getId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Una etapa no puede ser padre de si misma."));
                }
                if (parent != null && etapaCatalogoRepository.existsByParentId(etapa.getId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "No puedes convertir en subetapa una etapa que ya tiene subetapas."));
                }
            } else {
                etapa = new EtapaCatalogo();
                etapa.setMateria(materia);
                etapa.setUsuario(usuarioActual);
                etapa.setActivo(true);
            }

            etapa.setNombre(nombre.trim());
            etapa.setClave(payload.getOrDefault("clave", normalizarClaveEtapa(nombre)));
            etapa.setOrden(parseIntOrDefault(payload.get("orden"), 1));
            etapa.setDescripcion(payload.get("descripcion"));
            etapa.setParent(parent);
            etapa.setCierraExpediente(Boolean.parseBoolean(payload.getOrDefault("cierraExpediente", "false")));
            etapa.setActivo(Boolean.parseBoolean(payload.getOrDefault("activo", "true")));
            etapa.setEtapaLegacy(Boolean.TRUE.equals(etapa.getCierraExpediente())
                    ? com.juridico.sistema_juridico.Entity.enums.EtapaProcesal.CONCLUIDO
                    : com.juridico.sistema_juridico.Entity.enums.EtapaProcesal.TRAMITE);

            etapaCatalogoRepository.save(etapa);
            return ResponseEntity.ok(Map.of("status", "ok", "id", etapa.getId(), "mensaje", "Etapa guardada correctamente."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/etapas/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarEtapa(@PathVariable("id") Integer id) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            EtapaCatalogo etapa = etapaCatalogoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Etapa no encontrada."));
            validarMateriaPermitida(etapa.getMateria().getId(), usuarioActual);
            etapa.setActivo(false);
            etapaCatalogoRepository.save(etapa);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Etapa desactivada correctamente."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo desactivar: " + e.getMessage()));
        }
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return value != null && !value.isBlank() ? Integer.parseInt(value) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private List<EtapaCatalogo> ordenarEtapasJerarquicas(List<EtapaCatalogo> etapas) {
        return etapas.stream()
                .sorted(Comparator
                        .comparing((EtapaCatalogo e) -> e.getMateria() != null ? e.getMateria().getNombre() : "",
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(e -> e.getParent() != null ? e.getParent().getOrden() : e.getOrden(),
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(e -> e.getParent() != null ? e.getParent().getNombre() : e.getNombre(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(e -> e.getParent() != null ? 1 : 0)
                        .thenComparing(EtapaCatalogo::getOrden, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(EtapaCatalogo::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<String, Object> mapearEtapaCatalogo(EtapaCatalogo etapa) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", etapa.getId());
        map.put("nombre", etapa.getNombre());
        map.put("nombreJerarquico", etapa.getNombreJerarquico());
        map.put("clave", etapa.getClave());
        map.put("orden", etapa.getOrden());
        map.put("activo", etapa.getActivo());
        map.put("cierraExpediente", etapa.getCierraExpediente());
        map.put("materiaId", etapa.getMateria() != null ? etapa.getMateria().getId() : null);
        map.put("materiaNombre", etapa.getMateria() != null ? etapa.getMateria().getNombre() : "");
        map.put("parentId", etapa.getParent() != null ? etapa.getParent().getId() : null);
        map.put("parentNombre", etapa.getParent() != null ? etapa.getParent().getNombre() : "");
        return map;
    }

    @GetMapping("/gerencias/toggle/{id}")
    public String toggleGerencia(@PathVariable("id") Integer id) {
        Usuario usuarioActual = getUsuarioActual();
        Gerencia g = usuarioService.buscarGerenciaPorId(id);
        validarPuedeModificarGerencia(usuarioActual, g);
        g.setActivo(!g.getActivo());
        usuarioService.guardarGerencia(g);
        return "redirect:/catalogos?gerenciaId=" + id;
    }

    @GetMapping("/organos/toggle/{id}")
    public String toggleOrgano(@PathVariable("id") Integer id) {
        Usuario usuarioActual = getUsuarioActual();
        OrganoJurisdiccional o = usuarioService.buscarOrganoPorId(id);
        validarAlcanceGerencia(o.getGerencia(), usuarioActual, "organos jurisdiccionales");
        o.setActivo(!o.getActivo());
        usuarioService.guardarOrgano(o);
        Integer gId = o.getGerencia() != null ? o.getGerencia().getId() : null;
        return "redirect:/catalogos" + (gId != null ? "?gerenciaId=" + gId : "");
    }

    @GetMapping("/tipos-audiencia/toggle/{id}")
    public String toggleTipoAudiencia(@PathVariable("id") Integer id) {
        Usuario usuarioActual = getUsuarioActual();
        TipoAudiencia t = usuarioService.buscarTipoAudienciaPorId(id);
        validarAlcanceGerencia(t.getGerencia(), usuarioActual, "tipos de audiencia");
        t.setActivo(!t.getActivo());
        usuarioService.guardarTipoAudiencia(t);
        Integer gId = t.getGerencia() != null ? t.getGerencia().getId() : null;
        return "redirect:/catalogos" + (gId != null ? "?gerenciaId=" + gId : "");
    }

    @GetMapping("/tipos-expediente/toggle/{id}")
    public String toggleTipoExpediente(@PathVariable("id") Integer id) {
        Usuario usuarioActual = getUsuarioActual();
        TipoExpediente t = tipoExpedienteRepository.findById(id).orElseThrow();
        validarAlcanceGerencia(t.getGerencia(), usuarioActual, "tipos de expediente");
        t.setActivo(!t.getActivo());
        tipoExpedienteRepository.save(t);
        Integer gId = t.getGerencia() != null ? t.getGerencia().getId() : null;
        return "redirect:/catalogos" + (gId != null ? "?gerenciaId=" + gId : "");
    }

    // ===============================================================
    // CRUD - TIPOS DE TÉRMINO
    // ===============================================================
    @PostMapping("/tipos-termino/guardar")
    public String guardarTipoTermino(@ModelAttribute com.juridico.sistema_juridico.Entity.catalogo.TipoTermino tipoTermino, RedirectAttributes ra) {
        try {
            if (tipoTermino.getId() == null) {
                tipoTerminoService.crear(tipoTermino);
                ra.addFlashAttribute("mensajeExito", "Tipo de término creado correctamente.");
            } else {
                tipoTerminoService.actualizar(tipoTermino.getId(), tipoTermino);
                ra.addFlashAttribute("mensajeExito", "Tipo de término actualizado correctamente.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al guardar el tipo de término: " + e.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#terminos";
    }

    @PostMapping("/tipos-termino/estado/{id}")
    public String cambiarEstadoTipoTermino(@PathVariable Integer id, @RequestParam boolean activo, RedirectAttributes ra) {
        try {
            tipoTerminoService.cambiarEstado(id, activo);
            ra.addFlashAttribute("mensajeExito", "Estado del tipo de término actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al cambiar el estado del tipo de término: " + e.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#terminos";
    }
}
