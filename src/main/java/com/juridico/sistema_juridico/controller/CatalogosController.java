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

        if (gerenciaSeleccionada != null) {
            organos = organoRepository.findByGerenciaId(gerenciaSeleccionada.getId());
            // Incluir órganos sin gerencia asignada
            organos.addAll(organoRepository.findByGerenciaIsNull());
            // Eliminar duplicados por id
            organos = organos.stream()
                .collect(Collectors.collectingAndThen(
                    Collectors.toMap(OrganoJurisdiccional::getId, o -> o, (a, b) -> a),
                    m -> new ArrayList<>(m.values())
                ));
            organos.sort(Comparator.comparing(OrganoJurisdiccional::getNombre, String.CASE_INSENSITIVE_ORDER));

            tiposAudiencia = tipoAudienciaRepository.findByGerenciaId(gerenciaSeleccionada.getId());
            // Incluir tipos sin gerencia asignada (globales)
            tiposAudiencia.addAll(tipoAudienciaRepository.findByGerenciaIsNull());
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

            sedes = estadoRepository.findByGerenciaId(gerenciaSeleccionada.getId());
            // Incluir sedes sin gerencia
            sedes.addAll(estadoRepository.findByGerenciaIsNull());
            sedes = sedes.stream()
                .collect(Collectors.collectingAndThen(
                    Collectors.toMap(Estado::getId, e -> e, (a, b) -> a),
                    m -> new ArrayList<>(m.values())
                ));
            sedes.sort(Comparator.comparing(Estado::getNombre, String.CASE_INSENSITIVE_ORDER));
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
                organo = usuarioService.buscarOrganoPorId(Integer.parseInt(idStr));
                organo.setNombre(nombre.trim());
            } else {
                organo = new OrganoJurisdiccional();
                organo.setNombre(nombre.trim());
                organo.setActivo(true);
                if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                    organo.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                }
            }

            usuarioService.guardarOrgano(organo);
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
                tipo = usuarioService.buscarTipoAudienciaPorId(Integer.parseInt(idStr));
                tipo.setNombre(nombre.trim());
            } else {
                tipo = new TipoAudiencia();
                tipo.setNombre(nombre.trim());
                tipo.setActivo(true);
                if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                    tipo.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
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
                estado = estadoRepository.findById(Integer.parseInt(idStr))
                    .orElseThrow(() -> new RuntimeException("Sede no encontrada."));
                estado.setNombre(nombre.trim());
            } else {
                estado = new Estado();
                estado.setNombre(nombre.trim());
                if (gerenciaIdStr != null && !gerenciaIdStr.isEmpty()) {
                    estado.setGerencia(gerenciaRepository.findById(Integer.parseInt(gerenciaIdStr)).orElse(null));
                }
            }

            estadoRepository.save(estado);
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