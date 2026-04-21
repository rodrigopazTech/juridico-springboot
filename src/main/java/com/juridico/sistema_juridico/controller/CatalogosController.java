package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.catalogo.*;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
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
    @Autowired private com.juridico.sistema_juridico.service.CatalogosSyncService syncService;

    // ===============================================================
    // VISTA PRINCIPAL
    // ===============================================================
    @GetMapping
    public String index(Model model, @RequestParam(value = "gerenciaId", required = false) Integer gerenciaId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioActual = usuarioRepository.findByEmail(email).orElseThrow();
        RolUsuario rol = usuarioActual.getRol();

        // --- Determinar qué gerencias puede ver ---
        boolean esNivelAlto = (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECCION || rol == RolUsuario.SUBDIRECCION);
        boolean esGerente = (rol == RolUsuario.GERENTE || rol == RolUsuario.JEFE_DEPTO);
        boolean esAbogado = (rol == RolUsuario.ABOGADO);

        // Puede realizar acciones? (Admin, Dir, SubDir, Gerente, JefeDepto)
        boolean puedeEditar = !esAbogado;

        List<Gerencia> gerenciasDisponibles;
        Gerencia gerenciaSeleccionada = null;

        if (esNivelAlto) {
            gerenciasDisponibles = gerenciaRepository.findAll();
        } else {
            // Gerente, Jefe o Abogado: Solo su gerencia
            Gerencia miGerencia = usuarioActual.getGerencia();
            if (miGerencia != null) {
                gerenciasDisponibles = List.of(miGerencia);
                gerenciaId = miGerencia.getId(); // Forzar selección a su gerencia
            } else {
                gerenciasDisponibles = List.of();
            }
        }

        // Si se seleccionó una gerencia (o fue forzada)
        if (gerenciaId != null) {
            gerenciaSeleccionada = gerenciaRepository.findById(gerenciaId).orElse(null);
        } else if (!gerenciasDisponibles.isEmpty()) {
            // Auto-seleccionar la primera gerencia
            gerenciaSeleccionada = gerenciasDisponibles.get(0);
        }

        // --- Cargar catálogos filtrados por la gerencia seleccionada ---
        List<OrganoJurisdiccional> organos = new ArrayList<>();
        List<TipoAudiencia> tiposAudiencia = new ArrayList<>();
        List<Usuario> abogados = new ArrayList<>();
        List<Estado> sedes = new ArrayList<>();
        List<Materia> materias = new ArrayList<>();

        if (gerenciaSeleccionada != null) {
            organos = organoRepository.findByGerenciaIdAndActivoTrue(gerenciaSeleccionada.getId());
            // Incluir órganos sin gerencia asignada
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
        
        model.addAttribute("fechaLimiteNuevo", java.time.LocalDateTime.now().minusDays(7));

        model.addAttribute("pageTitle", "Catálogos Generales");
        model.addAttribute("activePage", "catalogos");

        return "views/catalogos/index";
    }

    // ===============================================================
    // CRUD - ÓRGANOS JURISDICCIONALES
    // ===============================================================
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

            OrganoJurisdiccional organo;
            if (idStr != null && !idStr.isEmpty()) {
                // EDICIÓN
                organo = usuarioService.buscarOrganoPorId(Integer.parseInt(idStr));
                organo.setNombre(nombre.trim());
            } else {
                // CREACIÓN: buscar si ya existe uno inactivo con el mismo nombre
                organo = organoRepository.findByNombreIgnoreCase(nombre.trim())
                    .filter(o -> Boolean.FALSE.equals(o.getActivo()))
                    .orElse(null);

                if (organo != null) {
                    // Reactivar el existente
                    organo.setActivo(true);
                    if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                        organo.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                    }
                } else {
                    organo = new OrganoJurisdiccional();
                    organo.setNombre(nombre.trim());
                    organo.setActivo(true);
                    if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                        organo.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                    }
                }
            }

            usuarioService.guardarOrgano(organo);
            syncService.sincronizarNombreOrgano(organo);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Órgano guardado correctamente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/organos/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarOrgano(@PathVariable("id") Integer id) {
        try {
            usuarioService.eliminarOrgano(id);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Órgano eliminado correctamente."));
        } catch (DataIntegrityViolationException e) {
            OrganoJurisdiccional o = usuarioService.buscarOrganoPorId(id);
            o.setActivo(false);
            organoRepository.save(o);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "El valor estaba en uso. Ha sido ocultado del catálogo."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }

    // ===============================================================
    // CRUD - TIPOS DE AUDIENCIA
    // ===============================================================
    @PostMapping("/tipos-audiencia/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarTipoAudiencia(@RequestBody Map<String, String> payload) {
        try {
            String nombre = payload.get("nombre");
            String idStr = payload.get("id");
            String gerenciaIdStr = payload.get("gerenciaId");

            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio."));
            }

            TipoAudiencia tipo;
            if (idStr != null && !idStr.isEmpty()) {
                // EDICIÓN
                tipo = usuarioService.buscarTipoAudienciaPorId(Integer.parseInt(idStr));
                tipo.setNombre(nombre.trim());
            } else {
                // CREACIÓN: buscar si ya existe uno inactivo con el mismo nombre
                tipo = tipoAudienciaRepository.findByNombreIgnoreCase(nombre.trim())
                    .filter(t -> Boolean.FALSE.equals(t.getActivo()))
                    .orElse(null);

                if (tipo != null) {
                    // Reactivar el existente
                    tipo.setActivo(true);
                    if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                        tipo.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                    }
                } else {
                    tipo = new TipoAudiencia();
                    tipo.setNombre(nombre.trim());
                    tipo.setActivo(true);
                    if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                        tipo.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                    }
                }
            }

            usuarioService.guardarTipoAudiencia(tipo);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Tipo de Audiencia guardado correctamente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/tipos-audiencia/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarTipoAudiencia(@PathVariable("id") Integer id) {
        try {
            usuarioService.eliminarTipoAudiencia(id);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Tipo de Audiencia eliminado correctamente."));
        } catch (DataIntegrityViolationException e) {
            TipoAudiencia t = usuarioService.buscarTipoAudienciaPorId(id);
            t.setActivo(false);
            tipoAudienciaRepository.save(t);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "El valor estaba en uso. Ha sido ocultado del catálogo."));
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

            Estado estado;
            if (idStr != null && !idStr.isEmpty()) {
                // EDICIÓN
                estado = estadoRepository.findById(Integer.parseInt(idStr))
                    .orElseThrow(() -> new RuntimeException("Sede no encontrada."));
                estado.setNombre(nombre.trim());
            } else {
                // CREACIÓN: buscar si ya existe uno inactivo con el mismo nombre
                estado = estadoRepository.findByNombreIgnoreCase(nombre.trim())
                    .filter(e -> Boolean.FALSE.equals(e.getActivo()))
                    .orElse(null);

                if (estado != null) {
                    // Reactivar el existente
                    estado.setActivo(true);
                    if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                        estado.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                    }
                } else {
                    estado = new Estado();
                    estado.setNombre(nombre.trim());
                    estado.setActivo(true);
                    if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                        estado.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                    }
                }
            }

            String nombreViejo = (idStr != null && !idStr.isEmpty()) ? 
                estadoRepository.findById(Integer.parseInt(idStr)).map(Estado::getNombre).orElse(null) : null;

            estadoRepository.save(estado);
            
            if (nombreViejo != null && !nombreViejo.equals(estado.getNombre())) {
                syncService.sincronizarNombreSede(nombreViejo, estado.getNombre());
            }
            
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Sede/Estado guardado correctamente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/sedes/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarSede(@PathVariable("id") Integer id) {
        try {
            estadoRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Sede/Estado eliminado correctamente."));
        } catch (DataIntegrityViolationException e) {
            Estado s = estadoRepository.findById(id).orElseThrow();
            s.setActivo(false);
            estadoRepository.save(s);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "El valor estaba en uso. Ha sido ocultado del catálogo."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }

    // ===============================================================
    // CRUD - GERENCIAS (conservado para admin)
    // ===============================================================
    @PostMapping("/gerencias/guardar")
    public String guardarGerencia(@ModelAttribute Gerencia gerencia, RedirectAttributes redirectAttrs) {
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

            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio."));
            }

            Materia materia;
            if (idStr != null && !idStr.isEmpty()) {
                // EDICIÓN
                materia = materiaRepository.findById(Integer.parseInt(idStr))
                    .orElseThrow(() -> new RuntimeException("Materia no encontrada."));
                materia.setNombre(nombre.trim());
            } else {
                // CREACIÓN: buscar si existe inactiva
                materia = materiaRepository.findByNombreIgnoreCase(nombre.trim())
                    .filter(m -> Boolean.FALSE.equals(m.getActivo()))
                    .orElse(null);

                if (materia != null) {
                    materia.setActivo(true);
                    if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                        materia.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                    }
                } else {
                    materia = new Materia();
                    materia.setNombre(nombre.trim());
                    materia.setActivo(true);
                    if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                        materia.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                    }
                }
            }

            materiaRepository.save(materia);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Materia guardada correctamente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/materias/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarMateriaCat(@PathVariable("id") Integer id) {
        try {
            materiaRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Materia eliminada correctamente."));
        } catch (DataIntegrityViolationException e) {
            Materia m = materiaRepository.findById(id).orElseThrow();
            m.setActivo(false);
            materiaRepository.save(m);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "La materia estaba en uso. Ha sido ocultada del catálogo."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }

    @GetMapping("/gerencias/toggle/{id}")
    public String toggleGerencia(@PathVariable("id") Integer id) {
        Gerencia g = usuarioService.buscarGerenciaPorId(id);
        g.setActivo(!g.getActivo());
        usuarioService.guardarGerencia(g);
        return "redirect:/catalogos?gerenciaId=" + id;
    }

    @GetMapping("/organos/toggle/{id}")
    public String toggleOrgano(@PathVariable("id") Integer id) {
        OrganoJurisdiccional o = usuarioService.buscarOrganoPorId(id);
        o.setActivo(!o.getActivo());
        usuarioService.guardarOrgano(o);
        Integer gId = o.getGerencia() != null ? o.getGerencia().getId() : null;
        return "redirect:/catalogos" + (gId != null ? "?gerenciaId=" + gId : "");
    }

    @GetMapping("/tipos-audiencia/toggle/{id}")
    public String toggleTipoAudiencia(@PathVariable("id") Integer id) {
        TipoAudiencia t = usuarioService.buscarTipoAudienciaPorId(id);
        t.setActivo(!t.getActivo());
        usuarioService.guardarTipoAudiencia(t);
        Integer gId = t.getGerencia() != null ? t.getGerencia().getId() : null;
        return "redirect:/catalogos" + (gId != null ? "?gerenciaId=" + gId : "");
    }

    // --- API JSON MATERIAS (Conservado) ---
    @GetMapping("/gerencias/{id}/materias")
    public String obtenerMateriasGerencia(@PathVariable("id") Integer id, Model model) {
        Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
        model.addAttribute("materias", gerencia.getMaterias());
        return "fragments/usuarios/modal-materias :: lista";
    }

    @PostMapping("/gerencias/materias/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarMateria(@RequestBody Materia materia) {
        try {
            usuarioService.guardarMateria(materia);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al guardar materia");
        }
    }

    @DeleteMapping("/gerencias/materias/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarMateria(@PathVariable("id") Integer id) {
        try {
            usuarioService.eliminarMateria(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar materia");
        }
    }
}