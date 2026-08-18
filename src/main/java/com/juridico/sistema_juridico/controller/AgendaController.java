package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.service.AgendaService;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private GerenciaRepository gerenciaRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoAudienciaRepository tipoAudienciaRepository;

    @Autowired
    private CatPrioridadRepository catPrioridadRepository;

    @GetMapping
    public String index(Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) Integer abogadoId,
            @RequestParam(required = false) Integer tipoAudienciaId,
            @RequestParam(required = false) Integer prioridadId,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String rangoFecha,
            @RequestParam(defaultValue = "audiencias") String tab,
            @RequestParam(defaultValue = "0") int page) {

        Usuario usuario = obtenerUsuarioActual();
        LocalDate[] rangoAplicado = aplicarRangoRapido(rangoFecha, fechaInicio, fechaFin);
        fechaInicio = rangoAplicado[0];
        fechaFin = rangoAplicado[1];

        // 2. Obtener Datos PAGINADOS
        var audiencias = agendaService.getAudienciasPorFiltro(keyword, fechaInicio, fechaFin, gerenciaId, materiaId,
                abogadoId, tipoAudienciaId, documento, page);
        var terminos = agendaService.getTerminosPorFiltro(keyword, fechaInicio, fechaFin, gerenciaId, materiaId,
                abogadoId, prioridadId, documento, page);

        // 3. Cargar Modelo
        model.addAttribute("audiencias", audiencias);
        model.addAttribute("terminos", terminos);
        model.addAttribute("paramKeyword", keyword);

        // 4. UI y Filtros
        model.addAttribute("pageTitle", "Concluidos");
        model.addAttribute("activePage", "agenda");
        model.addAttribute("activeTab", tab);
        
        model.addAttribute("paramFechaInicio", fechaInicio);
        model.addAttribute("paramFechaFin", fechaFin);
        model.addAttribute("paramGerenciaId", gerenciaId);
        model.addAttribute("paramMateriaId", materiaId);
        model.addAttribute("paramAbogadoId", abogadoId);
        model.addAttribute("paramTipoAudienciaId", tipoAudienciaId);
        model.addAttribute("paramPrioridadId", prioridadId);
        model.addAttribute("paramDocumento", documento);
        model.addAttribute("paramRangoFecha", rangoFecha);

        cargarCatalogosFiltro(model, usuario, gerenciaId, materiaId);

        return "views/agenda/index";
    }

    @GetMapping("/api/filtros")
    @ResponseBody
    public ResponseEntity<Map<String, List<Map<String, Object>>>> obtenerCatalogosFiltros(
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId) {

        Usuario usuario = obtenerUsuarioActual();
        Integer gerenciaEfectivaId = obtenerGerenciaEfectiva(usuario, gerenciaId);

        Map<String, List<Map<String, Object>>> respuesta = new LinkedHashMap<>();
        respuesta.put("materias", mapearMaterias(obtenerMateriasFiltradas(usuario, gerenciaEfectivaId)));
        respuesta.put("abogados", mapearUsuarios(obtenerAbogadosFiltrados(usuario, gerenciaEfectivaId, materiaId)));
        respuesta.put("tiposAudiencia", mapearTiposAudiencia(obtenerTiposAudienciaFiltrados(gerenciaEfectivaId)));
        return ResponseEntity.ok(respuesta);
    }

    private Usuario obtenerUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email).orElseThrow();
    }

    private LocalDate[] aplicarRangoRapido(String rangoFecha, LocalDate fechaInicio, LocalDate fechaFin) {
        if (rangoFecha == null || rangoFecha.isBlank()) {
            return new LocalDate[] { fechaInicio, fechaFin };
        }

        LocalDate hoy = LocalDate.now();
        return switch (rangoFecha) {
            case "hoy" -> new LocalDate[] { hoy, hoy };
            case "semana" -> new LocalDate[] { hoy.with(java.time.DayOfWeek.MONDAY), hoy.with(java.time.DayOfWeek.SUNDAY) };
            case "mes" -> new LocalDate[] { hoy.withDayOfMonth(1), hoy.with(TemporalAdjusters.lastDayOfMonth()) };
            case "mes_anterior" -> {
                LocalDate mesAnterior = hoy.minusMonths(1);
                yield new LocalDate[] { mesAnterior.withDayOfMonth(1), mesAnterior.with(TemporalAdjusters.lastDayOfMonth()) };
            }
            default -> new LocalDate[] { fechaInicio, fechaFin };
        };
    }

    private void cargarCatalogosFiltro(Model model, Usuario usuario, Integer gerenciaId, Integer materiaId) {
        Integer gerenciaEfectivaId = obtenerGerenciaEfectiva(usuario, gerenciaId);
        model.addAttribute("listaTiposAudiencia", obtenerTiposAudienciaFiltrados(gerenciaEfectivaId));
        model.addAttribute("listaPrioridades", catPrioridadRepository.findAll());
        model.addAttribute("rolActual", usuario.getRol());

        if (usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECTOR_PRINCIPAL
                || usuario.getRol() == RolUsuario.DIRECTOR || usuario.getRol() == RolUsuario.SUBDIRECTOR) {
            model.addAttribute("listaGerencias", gerenciaRepository.findAll());
            model.addAttribute("listaMaterias", obtenerMateriasFiltradas(usuario, gerenciaEfectivaId));
            model.addAttribute("listaAbogados", obtenerAbogadosFiltrados(usuario, gerenciaEfectivaId, materiaId));
            return;
        }

        model.addAttribute("listaGerencias", usuario.getGerencia() != null ? List.of(usuario.getGerencia()) : Collections.emptyList());

        model.addAttribute("listaMaterias", obtenerMateriasFiltradas(usuario, gerenciaEfectivaId));
        model.addAttribute("listaAbogados", obtenerAbogadosFiltrados(usuario, gerenciaEfectivaId, materiaId));
    }

    private Integer obtenerGerenciaEfectiva(Usuario usuario, Integer gerenciaId) {
        boolean rolGlobal = usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECTOR_PRINCIPAL
                || usuario.getRol() == RolUsuario.DIRECTOR || usuario.getRol() == RolUsuario.SUBDIRECTOR;
        if (rolGlobal) {
            return gerenciaId;
        }
        return usuario.getGerencia() != null ? usuario.getGerencia().getId() : null;
    }

    private List<Materia> obtenerMateriasFiltradas(Usuario usuario, Integer gerenciaId) {
        List<Materia> materias = gerenciaId != null
                ? materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(gerenciaId)
                : materiaRepository.findByActivoTrueOrderByNombreAsc();

        if (usuario.getRol() == RolUsuario.JEFE_DEPTO) {
            Set<Integer> misMateriasIds = usuario.getMaterias().stream()
                    .map(Materia::getId)
                    .collect(Collectors.toSet());
            materias = materias.stream()
                    .filter(m -> misMateriasIds.contains(m.getId()))
                    .toList();
        }

        return materias;
    }

    private List<Usuario> obtenerAbogadosFiltrados(Usuario usuario, Integer gerenciaId, Integer materiaId) {
        List<Usuario> abogados = gerenciaId != null
                ? usuarioRepository.findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(gerenciaId)
                : usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO);

        abogados = abogados.stream()
                .filter(u -> u.getRol() == RolUsuario.ABOGADO)
                .filter(u -> materiaId == null || u.tieneMateria(materiaId))
                .toList();

        if (usuario.getRol() == RolUsuario.JEFE_DEPTO) {
            Set<Integer> misMateriasIds = usuario.getMaterias().stream()
                    .map(Materia::getId)
                    .collect(Collectors.toSet());
            abogados = abogados.stream()
                    .filter(u -> u.getMaterias() != null && u.getMaterias().stream()
                            .anyMatch(m -> misMateriasIds.contains(m.getId())))
                    .toList();
        }

        return abogados.stream()
                .sorted(Comparator.comparing(Usuario::getNombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<TipoAudiencia> obtenerTiposAudienciaFiltrados(Integer gerenciaId) {
        if (gerenciaId == null) {
            return tipoAudienciaRepository.findByActivoTrue().stream()
                    .sorted(Comparator.comparing(TipoAudiencia::getNombre, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        List<TipoAudiencia> tipos = new ArrayList<>(tipoAudienciaRepository.findByGerenciaIdAndActivoTrue(gerenciaId));
        tipos.addAll(tipoAudienciaRepository.findByGerenciaIsNullAndActivoTrue());
        return tipos.stream()
                .collect(Collectors.toMap(TipoAudiencia::getId, t -> t, (a, b) -> a, LinkedHashMap::new))
                .values()
                .stream()
                .sorted(Comparator.comparing(TipoAudiencia::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<Map<String, Object>> mapearMaterias(List<Materia> materias) {
        return materias.stream()
                .map(m -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", m.getId());
                    item.put("nombre", m.getNombre());
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> mapearUsuarios(List<Usuario> usuarios) {
        return usuarios.stream()
                .map(u -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", u.getId());
                    item.put("nombre", u.getNombreCompleto());
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> mapearTiposAudiencia(List<TipoAudiencia> tipos) {
        return tipos.stream()
                .map(t -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", t.getId());
                    item.put("nombre", t.getNombre());
                    return item;
                })
                .toList();
    }
}
