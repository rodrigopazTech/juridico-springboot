package com.juridico.sistema_juridico.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.enums.OrigenTermino;
import com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoContexto;
import com.juridico.sistema_juridico.Entity.procesal.TerminoPresentado;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.exception.ForbiddenException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoPresentadoRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.service.AuditoriaService;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.util.TerminoExcelExporter;
import com.juridico.sistema_juridico.service.procesal.TerminoArchivoService;
import com.juridico.sistema_juridico.service.procesal.CalculadoraPlazoService;
import com.juridico.sistema_juridico.service.procesal.EvaluadorAtencionTemporalService;
import com.juridico.sistema_juridico.service.procesal.ResultadoCalculoPlazo;
import com.juridico.sistema_juridico.service.procesal.FlujoTerminoService;
import com.juridico.sistema_juridico.service.FileStorageService;
import com.juridico.sistema_juridico.Entity.enums.TipoDocumentoTermino;
import com.juridico.sistema_juridico.Entity.enums.TipoEvidenciaPresentacion;
import com.juridico.sistema_juridico.Entity.enums.TipoReferenciaAsunto;
import com.juridico.sistema_juridico.Entity.procesal.AsuntoIndependiente;
import com.juridico.sistema_juridico.Entity.procesal.TerminoArchivo;
import com.juridico.sistema_juridico.service.procesal.AsuntoIndependienteService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/terminos")
public class TerminosController {

    @Autowired
    private TerminoRepository terminoRepository;
    @Autowired
    private TerminoArchivoService terminoArchivoService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ExpedienteRepository expedienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TerminoPresentadoRepository terminoPresentadoRepository;
    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private com.juridico.sistema_juridico.repository.Expediente.ColaboradorExpedienteRepository colaboradorExpedienteRepository;
    @Autowired
    private GerenciaRepository gerenciaRepository;
    @Autowired
    private MateriaRepository materiaRepository;
    @Autowired
    private CatPrioridadRepository catPrioridadRepository;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private com.juridico.sistema_juridico.service.catalogo.TipoTerminoService tipoTerminoService;
    @Autowired
    private CalculadoraPlazoService calculadoraPlazoService;
    @Autowired
    private EvaluadorAtencionTemporalService evaluadorAtencionTemporalService;
    @Autowired
    private AsuntoIndependienteService asuntoIndependienteService;
    @Autowired
    private FlujoTerminoService flujoTerminoService;

    // --- MATRIZ DE PERMISOS ---
    private static final Map<EstatusTermino, List<RolUsuario>> PERMISOS_ETAPAS = new HashMap<>();

    static {
        List<RolUsuario> todosLosRoles = Arrays.asList(RolUsuario.values());
        PERMISOS_ETAPAS.put(EstatusTermino.PROYECTISTA, todosLosRoles);
        PERMISOS_ETAPAS.put(EstatusTermino.REVISION, todosLosRoles);
        PERMISOS_ETAPAS.put(EstatusTermino.GERENCIA, todosLosRoles);
        PERMISOS_ETAPAS.put(EstatusTermino.DIRECCION, todosLosRoles);
        PERMISOS_ETAPAS.put(EstatusTermino.LIBERADO, todosLosRoles);
        PERMISOS_ETAPAS.put(EstatusTermino.PRESENTADO, todosLosRoles);
    }

    private static final Map<EstatusTermino, Integer> ORDEN_ETAPAS = new HashMap<>();

    static {
        ORDEN_ETAPAS.put(EstatusTermino.PROYECTISTA, 1);
        ORDEN_ETAPAS.put(EstatusTermino.REVISION, 2);
        ORDEN_ETAPAS.put(EstatusTermino.GERENCIA, 3);
        ORDEN_ETAPAS.put(EstatusTermino.DIRECCION, 4);
        ORDEN_ETAPAS.put(EstatusTermino.LIBERADO, 5);
        ORDEN_ETAPAS.put(EstatusTermino.PRESENTADO, 6);
        ORDEN_ETAPAS.put(EstatusTermino.CONCLUIDO, 7);
    }

    @GetMapping({"", "/"})
    public String index(Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EstatusTermino estatus,
            @RequestParam(required = false) Integer prioridadId,
            @RequestParam(required = false) Integer abogadoId,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) PeriodoFiltro periodo,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            @RequestParam(defaultValue = "fechaVencimiento") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // --- 1. LIMPIEZA DE URL (Fix Notificaciones) ---
        if (keyword != null && keyword.contains("?keyword=")) {
            keyword = keyword.substring(keyword.lastIndexOf("=") + 1);
        }

        Usuario usuarioActual = getUsuarioActual();
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, 10, sort);

        // --- 2. FILTROS DE SEGURIDAD (TABLA) ---
        Integer filtroAbogadoSeguro = abogadoId;
        Integer filtroGerenciaSeguro = gerenciaId;
        List<Integer> filtroMateriaIds = null;

        switch (usuarioActual.getRol()) {
            case JEFE_DEPTO:
                if (usuarioActual.getGerencia() != null) {
                    filtroGerenciaSeguro = usuarioActual.getGerencia().getId();
                } else {
                    filtroGerenciaSeguro = -1;
                }
                if (usuarioActual.getMaterias() != null && !usuarioActual.getMaterias().isEmpty()) {
                    filtroMateriaIds = usuarioActual.getMaterias().stream()
                            .map(m -> m.getId())
                            .collect(Collectors.toList());
                } else {
                    filtroMateriaIds = java.util.Collections.emptyList();
                }
                break;
            case ABOGADO:
                filtroAbogadoSeguro = usuarioActual.getId();
                filtroGerenciaSeguro = null;
                break;
            case GERENTE:
                if (usuarioActual.getGerencia() != null) {
                    filtroGerenciaSeguro = usuarioActual.getGerencia().getId();
                } else {
                    filtroGerenciaSeguro = -1;
                }
                break;
            case ADMIN:
            case DIRECTOR_PRINCIPAL:
            case DIRECTOR:
            case SUBDIRECTOR:
                // Ven todo
                break;
        }

        // CÁLCULO DE FECHAS
        LocalDate fechaInicio = LocalDate.of(1900, 1, 1);
        LocalDate fechaFin = LocalDate.of(2100, 12, 31);

        String searchKeyword = keyword;
        if (keyword != null && keyword.matches("\\d{4}-\\d{2}-\\d{2}")) {
            LocalDate fecha = LocalDate.parse(keyword);
            fechaInicio = fecha;
            fechaFin = fecha;
            searchKeyword = null;
        } else if (periodo != null) {
            LocalDate[] rango = periodo.calcularRango();
            fechaInicio = rango[0];
            fechaFin = rango[1];
        }

        boolean isTextSearch = searchKeyword != null && !searchKeyword.trim().isEmpty();
        boolean isExactDate = keyword != null && keyword.matches("\\d{4}-\\d{2}-\\d{2}");

        if (mes != null && anio != null && !isTextSearch && !isExactDate && periodo == null) {
            java.time.YearMonth ym = java.time.YearMonth.of(anio, mes);
            fechaInicio = ym.atDay(1);
            fechaFin = ym.atEndOfMonth();
        }



        Page<Termino> paginaTerminos = terminoRepository.buscarConFiltros(
                id, searchKeyword, estatus, prioridadId,
                filtroAbogadoSeguro, filtroGerenciaSeguro,
                fechaInicio, fechaFin,
                materiaId, filtroMateriaIds,
                pageable);

        model.addAttribute("terminos", paginaTerminos);

        // --- 3. FILTRO DE ABOGADOS PARA EL MODAL (SEGURIDAD) ---
        List<Usuario> abogadosParaModal;
        if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
            abogadosParaModal = Collections.singletonList(usuarioActual);
        } else if (usuarioActual.getRol() == RolUsuario.GERENTE) {
            if (usuarioActual.getGerencia() != null) {
                abogadosParaModal = usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO).stream()
                        .filter(u -> u.getGerencia() != null
                        && u.getGerencia().getId().equals(usuarioActual.getGerencia().getId()))
                        .collect(Collectors.toList());
            } else {
                abogadosParaModal = usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO);
            }
        } else if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            abogadosParaModal = usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO).stream()
                    .filter(u -> u.getGerencia() != null && usuarioActual.getGerencia() != null
                    && u.getGerencia().getId().equals(usuarioActual.getGerencia().getId()))
                    .filter(u -> u.getMaterias().stream().anyMatch(m -> usuarioActual.getMaterias().contains(m)))
                    .collect(Collectors.toList());
        } else {
            // ADMIN, DIRECTOR_PRINCIPAL, DIRECTOR, SUBDIRECTOR
            abogadosParaModal = usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO);
        }
        model.addAttribute("abogados", abogadosParaModal);

        // --- 4. LISTAS PARA FILTROS GENERALES ---
        model.addAttribute("listaAbogados", usuarioRepository.findAll());
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        List<Gerencia> gerenciasParaModal;
        if (usuarioActual.getRol() == RolUsuario.ADMIN || usuarioActual.getRol() == RolUsuario.DIRECTOR_PRINCIPAL 
                || usuarioActual.getRol() == RolUsuario.DIRECTOR || usuarioActual.getRol() == RolUsuario.SUBDIRECTOR 
                || usuarioActual.getGerencia() == null) {
            gerenciasParaModal = gerenciaRepository.findAll();
        } else {
            gerenciasParaModal = Collections.singletonList(usuarioActual.getGerencia());
        }
        model.addAttribute("gerenciasParaModal", gerenciasParaModal);
        model.addAttribute("listaMaterias", materiaRepository.findAll());
        model.addAttribute("listaPrioridades", catPrioridadRepository.findAll());
        model.addAttribute("listaTiposTermino", tipoTerminoService.listarActivos());
        model.addAttribute("asuntosIndependientesList", asuntoIndependienteService.listarActivos(usuarioActual));
        model.addAttribute("tiposReferenciaAsunto", TipoReferenciaAsunto.values());
        model.addAttribute("listaEstatus", EstatusTermino.values());

        // --- 5. LOGICA ANTI-FANTASMAS (EXPEDIENTES DISPONIBLES) ---
        // El selector usa exactamente el mismo alcance que la validación del servidor.
        List<Expediente> expedientesParaDropdown = expedienteRepository.findAll().stream()
                .filter(expediente -> puedeAccederExpediente(expediente, usuarioActual))
                .toList();
        model.addAttribute("expedientesList", expedientesParaDropdown);

        model.addAttribute("rolActual", usuarioActual.getRol());
        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("ordenEtapas", ORDEN_ETAPAS);

        // Mantener valores
        model.addAttribute("keyword", keyword);
        model.addAttribute("paramEstatus", estatus);
        model.addAttribute("paramEstatusName", estatus != null ? estatus.name() : null);
        model.addAttribute("paramPrioridadId", prioridadId);
        model.addAttribute("paramAbogadoId", abogadoId);
        model.addAttribute("paramGerenciaId", gerenciaId);
        model.addAttribute("paramMateriaId", materiaId);
        model.addAttribute("paramPeriodo", periodo != null ? periodo.name() : null);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        // --- 6. CONTEOS PARA LA LÍNEA DE TIEMPO (NUEVO) ---
        List<Object[]> rawCounts = terminoRepository.contarPorEtapaConFiltros(
                searchKeyword, prioridadId, filtroAbogadoSeguro, filtroGerenciaSeguro, materiaId,
                fechaInicio, fechaFin,
                filtroMateriaIds);

        Map<String, Long> conteosMap = new HashMap<>();
        for (EstatusTermino e : EstatusTermino.values()) {
            conteosMap.put(e.name(), 0L);
        }
        for (Object[] row : rawCounts) {
            if (row[0] != null) {
                conteosMap.put(((EstatusTermino) row[0]).name(), (Long) row[1]);
            }
        }
        model.addAttribute("conteosEtapas", conteosMap);

        // --- 7. TOTALES EN PROCESO ---
        long totalesEnProceso = 0;
        for (EstatusTermino e : EstatusTermino.values()) {
            if (e != EstatusTermino.CONCLUIDO) {
                totalesEnProceso += conteosMap.getOrDefault(e.name(), 0L);
            }
        }
        model.addAttribute("totalesEnProceso", totalesEnProceso);
        model.addAttribute("pageTitle", "Términos");
        model.addAttribute("activePage", "terminos");
        model.addAttribute("mesActual", mes != null ? mes : LocalDate.now().getMonthValue());
        model.addAttribute("anioActual", anio != null ? anio : LocalDate.now().getYear());

        // --- 7. ETAPAS ASIGNADAS AL ROL ---
        List<EstatusTermino> etapasAsignadas = getEtapasAsignadasPorRol(usuarioActual.getRol());
        model.addAttribute("etapasAsignadas", etapasAsignadas);
        model.addAttribute("etapasAsignadasNames", etapasAsignadas.stream().map(EstatusTermino::name).collect(Collectors.toList()));

        // --- 8. FILTRO POR DEFECTO PARA EL USUARIO (ELIMINADO PARA MOSTRAR TODO) ---
        // Se eliminó la lógica que filtraba por etapas asignadas por defecto para mostrar el listado completo.
        // --- OPTIMIZACIÓN AJAX ---
        HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();
        String isAjax = request.getHeader("X-Requested-With");

        if ("XMLHttpRequest".equals(isAjax)) {
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                paginaTerminos = terminoRepository.buscarConFiltros(
                        id, searchKeyword, null, prioridadId, filtroAbogadoSeguro, filtroGerenciaSeguro,
                        fechaInicio, fechaFin, materiaId, filtroMateriaIds, pageable);
                model.addAttribute("terminos", paginaTerminos);
                model.addAttribute("paramEstatus", null);
                model.addAttribute("paramEstatusName", null);
            }
            return "views/terminos/index :: gridMaestroTerminos";
        }

        return "views/terminos/index";
    }

    private List<EstatusTermino> getEtapasAsignadasPorRol(RolUsuario rol) {
        return Arrays.asList(EstatusTermino.values());
    }

    @PostMapping("/guardar")
    @Transactional
    public Object guardar(@ModelAttribute Termino terminoForm,
            @RequestParam(value = "expedienteId", required = false) UUID expedienteId,
            @RequestParam(value = "abogadoId", required = false) Integer abogadoId,
            @RequestParam(value = "tipoTerminoId", required = false) Integer tipoTerminoId,
            @RequestParam(value = "origenTermino", required = false, defaultValue = "EXPEDIENTE") OrigenTermino origenTermino,
            @RequestParam(value = "contextoMateriaId", required = false) Integer contextoMateriaId,
            @RequestParam(value = "contextoGerenciaId", required = false) Integer contextoGerenciaId,
            @RequestParam(value = "contextoDescripcion", required = false) String contextoDescripcion,
            @RequestParam(value = "contextoPartes", required = false) String contextoPartes,
            @RequestParam(value = "contextoNumeroReferencia", required = false) String contextoNumeroReferencia,
            @RequestParam(value = "asuntoIndependienteId", required = false) Long asuntoIndependienteId,
            @RequestParam(value = "tipoReferenciaAsunto", required = false) TipoReferenciaAsunto tipoReferenciaAsunto,
            @RequestParam(value = "fechaReferenciaAsunto", required = false) LocalDate fechaReferenciaAsunto,
            RedirectAttributes redirectAttrs,
            HttpServletRequest request) {

        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        try {
            Usuario actor = getUsuarioActual();
            Termino terminoGuardar;
            boolean esNuevo = false;
            TipoTermino tipoSeleccionado = null;

            if (terminoForm.getId() == null && tipoTerminoId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un Tipo de Término.");
            }
            if (tipoTerminoId != null) {
                tipoSeleccionado = tipoTerminoService.obtenerPorId(tipoTerminoId);
                if (!Boolean.TRUE.equals(tipoSeleccionado.getActivo())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El Tipo de Término seleccionado está inactivo.");
                }
                terminoForm.setTipoTerminoRef(tipoSeleccionado);
                terminoForm.setTipoDiasCalculado(tipoSeleccionado.getTipoDias());
            } else if (terminoForm.getId() != null) {
                tipoSeleccionado = terminoRepository.findById(terminoForm.getId())
                        .map(Termino::getTipoTerminoRef)
                        .orElse(null);
            }

            if (terminoForm.getAsunto() == null || terminoForm.getAsunto().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo asunto/actuación es obligatorio.");
            }
            if (tipoSeleccionado != null && terminoForm.getId() == null) {
                LocalDate fechaOrigen = terminoForm.getFechaIngreso() != null
                        ? terminoForm.getFechaIngreso() : LocalDate.now();
                terminoForm.setFechaIngreso(fechaOrigen);
                calculadoraPlazoService.aplicarSnapshot(terminoForm,
                        calculadoraPlazoService.calcularResultado(
                                tipoSeleccionado, fechaOrigen, terminoForm.getFechaVencimiento()));
            } else if (terminoForm.getId() == null && terminoForm.getFechaVencimiento() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El término no tiene un Tipo de Término válido ni una fecha de vencimiento.");
            }

            if (actor.getRol() == RolUsuario.ABOGADO) {
                abogadoId = actor.getId();
            }

            if (origenTermino == OrigenTermino.ASUNTO_UNICO) {
                if (asuntoIndependienteId == null && actor.getRol() == RolUsuario.JEFE_DEPTO) {
                    Set<Integer> misMateriasIds = actor.getMaterias().stream()
                            .map(Materia::getId)
                            .collect(Collectors.toSet());
                    if (contextoMateriaId == null || !misMateriasIds.contains(contextoMateriaId)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tienes acceso a la materia seleccionada.");
                    }
                } else if (asuntoIndependienteId == null
                        && (actor.getRol() == RolUsuario.ABOGADO || actor.getRol() == RolUsuario.GERENTE)) {
                    if (actor.getGerencia() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no tiene una gerencia asignada.");
                    }
                    Materia mat = materiaRepository.findById(contextoMateriaId).orElse(null);
                    if (mat == null || mat.getGerencia() == null || !mat.getGerencia().getId().equals(actor.getGerencia().getId())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tienes acceso a la materia seleccionada.");
                    }
                }
                if ((actor.getRol() == RolUsuario.ABOGADO || actor.getRol() == RolUsuario.JEFE_DEPTO
                        || actor.getRol() == RolUsuario.GERENTE) && actor.getGerencia() != null) {
                    contextoGerenciaId = actor.getGerencia().getId();
                }
                AsuntoIndependiente asuntoIndependiente = asuntoIndependienteService.resolverOCrear(
                        asuntoIndependienteId, tipoReferenciaAsunto, contextoNumeroReferencia,
                        fechaReferenciaAsunto, contextoMateriaId, contextoGerenciaId,
                        contextoDescripcion, contextoPartes, abogadoId, actor);
                terminoForm.setExpediente(null);
                terminoForm.setAsuntoIndependiente(asuntoIndependiente);
                terminoForm.setContexto(crearContextoDesdeAsuntoIndependiente(asuntoIndependiente));
            } else if (expedienteId != null) {
                Expediente exp = expedienteRepository.findById(expedienteId)
                        .orElseThrow(() -> new ResourceNotFoundException("El expediente asociado no existe."));
                validarAccesoExpediente(exp, actor);
                terminoForm.setExpediente(exp);
                terminoForm.setAsuntoIndependiente(null);
                terminoForm.setContexto(crearContextoDesdeExpediente(exp));
            }

            if (abogadoId != null) {
                Usuario abogado = usuarioRepository.findById(abogadoId)
                        .orElseThrow(() -> new ResourceNotFoundException("El abogado asignado no existe."));
                terminoForm.setAbogadoResponsable(abogado);
            } else if (terminoForm.getId() == null && origenTermino == OrigenTermino.ASUNTO_UNICO) {
                terminoForm.setAbogadoResponsable(actor);
            }

            boolean expedienteCambiado = false;
            if (terminoForm.getId() != null) {
                Termino existente = terminoRepository.findById(terminoForm.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("El término que intentas editar no existe."));

                validarAccesoTermino(existente, actor);

                existente.setAsunto(terminoForm.getAsunto());

                if (terminoForm.getAbogadoResponsable() != null) {
                    existente.setAbogadoResponsable(terminoForm.getAbogadoResponsable());
                }

                Expediente expAnterior = existente.getExpediente();
                if (origenTermino == OrigenTermino.ASUNTO_UNICO) {
                    expedienteCambiado = expAnterior != null;
                    existente.setExpediente(null);
                    existente.setAsuntoIndependiente(terminoForm.getAsuntoIndependiente());
                    existente.setContexto(terminoForm.getContexto());
                } else if (terminoForm.getExpediente() != null) {
                    if (expAnterior == null || !expAnterior.getId().equals(terminoForm.getExpediente().getId())) {
                        expedienteCambiado = true;
                    }
                    existente.setExpediente(terminoForm.getExpediente());
                    existente.setAsuntoIndependiente(null);
                    existente.setContexto(terminoForm.getContexto());
                }
                if (terminoForm.getPrioridad() != null) {
                    existente.setPrioridad(terminoForm.getPrioridad());
                }
                if (terminoForm.getObservaciones() != null) {
                    existente.setObservaciones(terminoForm.getObservaciones());
                }
                existente.setUpdatedAt(LocalDateTime.now());
                terminoGuardar = existente;
            } else {
                if (terminoForm.getExpediente() == null && terminoForm.getAsuntoIndependiente() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Selecciona un expediente o un Asunto Independiente.");
                }
                esNuevo = true;
                terminoGuardar = terminoForm;
                terminoGuardar.setFechaCreacion(LocalDate.now());
                if (terminoGuardar.getFechaIngreso() == null) terminoGuardar.setFechaIngreso(LocalDate.now());
                terminoGuardar.setEstatusTermino(EstatusTermino.PROYECTISTA);
                terminoGuardar.setCreatedAt(LocalDateTime.now());
                terminoGuardar.setUpdatedAt(LocalDateTime.now());

                if (terminoGuardar.getAbogadoResponsable() == null && terminoGuardar.getExpediente() != null) {
                    terminoGuardar.setAbogadoResponsable(terminoGuardar.getExpediente().getAbogadoResponsable());
                }
                if (terminoGuardar.getAbogadoResponsable() == null && terminoGuardar.getAsuntoIndependiente() != null) {
                    terminoGuardar.setAbogadoResponsable(terminoGuardar.getAsuntoIndependiente().getResponsable());
                }
                if (terminoGuardar.getPrioridad() == null && terminoGuardar.getExpediente() != null) {
                    terminoGuardar.setPrioridad(terminoGuardar.getExpediente().getPrioridad());
                }

                validarAccesoTermino(terminoGuardar, actor);
            }

            if (esNuevo) {
                evaluadorAtencionTemporalService.fijarSnapshot(terminoGuardar);
            }
            Termino terminoGuardado = terminoRepository.save(terminoGuardar);

            if (esNuevo) {
                String desc = "Se ha registrado un nuevo término: " + (terminoGuardado.getAsunto() != null ? terminoGuardado.getAsunto() : "Sin Especificar")
                        + ". Acción realizada por: " + actor.getNombreCompleto() + " el " + LocalDate.now() + ".";
                if (terminoGuardado.getExpediente() != null) {
                    auditoriaService.registrar(terminoGuardado.getExpediente(), "Término Creado", desc, "CHECK", actor);
                } else {
                    auditoriaService.registrarActividadGeneral(actor, "Término Creado", desc, "CHECK");
                }
            }

            Usuario usuarioActualN = getUsuarioActual();

            Gerencia g = terminoGuardado.getGerenciaReferencia();
            if (g != null) {
                List<Usuario> destinatarios = usuarioRepository.findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(g.getId());
                String expedienteNum = terminoGuardado.getNumeroReferencia();

                if (esNuevo) {
                    for (Usuario u : destinatarios) {
                        String msg = u.getId().equals(actor.getId())
                                ? "Has creado el término con el asunto: " + terminoGuardado.getAsunto()
                                : "El usuario " + actor.getNombreCompleto() + " ha creado el término: " + terminoGuardado.getAsunto();

                        notificacionService.crearNotificacion(
                                u,
                                actor,
                                NotificacionEvento.TERMINO_NUEVO,
                                "Nuevo Término: " + expedienteNum,
                                msg,
                                terminoGuardado.getPrioridad(),
                                terminoGuardado.getId().toString(),
                                true);
                    }
                } else if (expedienteCambiado) {
                    for (Usuario u : destinatarios) {
                        String msg = u.getId().equals(actor.getId())
                                ? "Has vinculado el término a tu expediente/gerencia: " + terminoGuardado.getAsunto()
                                : "El usuario " + actor.getNombreCompleto() + " ha vinculado un término a tu expediente/gerencia: " + terminoGuardado.getAsunto();

                        notificacionService.crearNotificacion(
                                u,
                                actor,
                                NotificacionEvento.TERMINO_ASIGNADO,
                                "Término Vinculado: " + expedienteNum,
                                msg,
                                terminoGuardado.getPrioridad(),
                                terminoGuardado.getId().toString(),
                                true);
                    }
                }
            }

            redirectAttrs.addFlashAttribute("mensaje", "Término guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

            if (ajax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("mensaje", "Término guardado correctamente.");
                return ResponseEntity.ok(response);
            }
            return "redirect:/terminos";

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            String errorMsg = e instanceof ResponseStatusException ? ((ResponseStatusException) e).getReason() : e.getMessage();
            if (errorMsg == null) {
                errorMsg = "Error al guardar el término.";
            }
            if (ajax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", errorMsg);
                return ResponseEntity.badRequest().body(response);
            }
            redirectAttrs.addFlashAttribute("mensaje", errorMsg);
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }
    }

    private TerminoContexto crearContextoDesdeExpediente(Expediente exp) {
        if (exp.getMateria() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El expediente no tiene materia asignada.");
        }
        if (exp.getGerencia() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El expediente no tiene gerencia asignada.");
        }
        String descripcion = normalizarTexto(exp.getDescripcion());
        if (descripcion == null) {
            descripcion = "Expediente " + exp.getNumero();
        }

        return TerminoContexto.builder()
                .origen(OrigenTermino.EXPEDIENTE)
                .numeroExpedienteSnapshot(exp.getNumero())
                .descripcion(descripcion)
                .materia(exp.getMateria())
                .gerencia(exp.getGerencia())
                .organoJurisdiccional(exp.getOrganoJurisdiccional())
                .organoJurisdiccionalTexto(exp.getOrganoJurisdiccionalTexto())
                .partes(exp.getPartes())
                .sede(exp.getSede())
                .build();
    }

    private TerminoContexto crearContextoAsuntoUnico(Integer materiaId, Integer gerenciaId, String descripcion,
            String partes, String numeroReferencia) {
        if (materiaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La materia es obligatoria para un asunto único.");
        }
        if (gerenciaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La gerencia es obligatoria para un asunto único.");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria para un asunto único.");
        }

        Materia materia = materiaRepository.findById(materiaId)
                .orElseThrow(() -> new ResourceNotFoundException("La materia seleccionada no existe."));
        Gerencia gerencia = gerenciaRepository.findById(gerenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("La gerencia seleccionada no existe."));

        return TerminoContexto.builder()
                .origen(OrigenTermino.ASUNTO_UNICO)
                .numeroExpedienteSnapshot(normalizarTexto(numeroReferencia))
                .descripcion(descripcion.trim())
                .materia(materia)
                .gerencia(gerencia)
                .partes(normalizarTexto(partes))
                .build();
    }

    private TerminoContexto crearContextoDesdeAsuntoIndependiente(AsuntoIndependiente asunto) {
        return TerminoContexto.builder()
                .origen(OrigenTermino.ASUNTO_UNICO)
                .numeroExpedienteSnapshot(asunto.getFolioSiaj())
                .descripcion(asunto.getDescripcion())
                .materia(asunto.getMateria())
                .gerencia(asunto.getGerencia())
                .partes(asunto.getParteInteresada())
                .build();
    }

    private String normalizarTexto(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }

    @GetMapping("/avanzar/{id}")
    public String avanzarEstado(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        Termino termino = terminoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Término no encontrado."));

        Usuario actor = getUsuarioActual();
        validarAccesoTermino(termino, actor);

        EstatusTermino etapaAnterior = termino.getEstatusTermino();

        if (!tienePermiso(etapaAnterior, actor.getRol())) {
            redirectAttrs.addFlashAttribute("mensaje", "⛔ No tienes permiso para avanzar la etapa.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }

        boolean tieneDocumento = terminoArchivoService
                .obtenerUltimoPorTipo(id, TipoDocumentoTermino.BORRADOR).isPresent()
                || (termino.getAcuse() != null && !termino.getAcuse().isBlank());
        EstatusTermino siguienteEtapa;
        try {
            siguienteEtapa = flujoTerminoService.validarYResolverAvance(termino, tieneDocumento);
        } catch (RuntimeException ex) {
            redirectAttrs.addFlashAttribute("mensaje", ex.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }
        termino.setEstatusTermino(siguienteEtapa);
        if (siguienteEtapa == EstatusTermino.PRESENTADO) {
            termino.setFechaPresentacion(LocalDate.now());
        }

        terminoRepository.save(termino);

        if (termino.getExpediente() != null) {
            String desc = "Cambio de estatus de Término a " + siguienteEtapa.getDescripcion()
                    + "). Acción realizada por: " + actor.getNombreCompleto() + " el "
                    + LocalDate.now() + ".";
            auditoriaService.registrar(termino.getExpediente(), "Cambio de Estado (Término)", desc, "EDIT", actor);
        } else {
            String desc = "Cambio de estatus de Término '" + termino.getAsunto() + "' a " + siguienteEtapa.getDescripcion()
                    + ". Acción realizada por: " + actor.getNombreCompleto() + " el "
                    + LocalDate.now() + ".";
            auditoriaService.registrarActividadGeneral(actor, "Cambio de Estado (Término)", desc, "EDIT");
        }

        notificarCambioDeEtapa(termino, etapaAnterior, siguienteEtapa, actor);
        redirectAttrs.addFlashAttribute("mensaje", "Avanzó a " + siguienteEtapa.getDescripcion());
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/terminos";
    }

    private void notificarCambioDeEtapa(Termino termino, EstatusTermino etapaAnterior, EstatusTermino nuevaEtapa, Usuario actor) {
        Gerencia g = termino.getGerenciaReferencia();
        if (g == null) {
            return;
        }

        String titulo = (nuevaEtapa == EstatusTermino.CONCLUIDO)
                ? "Término Concluido"
                : "Término en " + nuevaEtapa.getDescripcion();

        String mensajeBase = String.format("Movimiento de '%s' a '%s' por %s", 
                etapaAnterior.getDescripcion(), nuevaEtapa.getDescripcion(), actor.getNombreCompleto());

        List<Usuario> destinatarios = usuarioRepository.findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(g.getId());
        Set<Usuario> unicos = new HashSet<>(destinatarios);
        String expedienteNum = termino.getNumeroReferencia();
        String tituloExp = titulo + ": " + expedienteNum;

        for (Usuario u : unicos) {
            NotificacionEvento evento = (nuevaEtapa == EstatusTermino.CONCLUIDO)
                    ? NotificacionEvento.TERMINO_VALIDADO_CONCLUIDO
                    : NotificacionEvento.TERMINO_CAMBIO_ETAPA;

            notificacionService.crearNotificacion(
                    u,
                    actor,
                    evento,
                    tituloExp,
                    mensajeBase,
                    termino.getPrioridad(),
                    termino.getId().toString(),
                    true);
        }
    }

    @PostMapping("/subir-archivo")
    public Object subirArchivo(@RequestParam("id") Integer id, @RequestParam("archivo") MultipartFile archivo, RedirectAttributes redirectAttrs, HttpServletRequest request) {
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        if (archivo.isEmpty()) {
            if (ajax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "Por favor selecciona un archivo.");
                return ResponseEntity.badRequest().body(response);
            }
            redirectAttrs.addFlashAttribute("mensaje", "Por favor selecciona un archivo.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }
        try {
            Termino termino = terminoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Término no encontrado"));
            Usuario actor = getUsuarioActual();
            validarAccesoTermino(termino, actor);

            if (!tienePermiso(termino.getEstatusTermino(), actor.getRol())) {
                String msg = "⛔ No tienes permiso para modificar archivos en la etapa: " + termino.getEstatusTermino();
                if (ajax) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("mensaje", msg);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
                redirectAttrs.addFlashAttribute("mensaje", msg);
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            EstatusTermino st = termino.getEstatusTermino();
            if (st == EstatusTermino.LIBERADO || st == EstatusTermino.PRESENTADO || st == EstatusTermino.CONCLUIDO) {
                String msg = "El documento está bloqueado en esta etapa.";
                if (ajax) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("mensaje", msg);
                    return ResponseEntity.badRequest().body(response);
                }
                redirectAttrs.addFlashAttribute("mensaje", msg);
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            // Usar persistencia avanzada para guardar el Borrador
            TerminoArchivo ta = terminoArchivoService.guardarArchivo(termino, archivo, TipoDocumentoTermino.BORRADOR, actor);

            termino.setAcuse(ta.getNombreOriginal());
            termino.setUpdatedAt(LocalDateTime.now());
            terminoRepository.save(termino);

            if (termino.getExpediente() != null) {
                auditoriaService.registrar(termino.getExpediente(), "ARCHIVO CARGADO", "Borrador: " + archivo.getOriginalFilename(), "UPLOAD", actor);
            } else {
                auditoriaService.registrarActividadGeneral(actor, "ARCHIVO CARGADO", "Borrador de término '" + termino.getAsunto() + "' cargado: " + archivo.getOriginalFilename(), "UPLOAD");
            }

            redirectAttrs.addFlashAttribute("mensaje", "Borrador actualizado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
            if (ajax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("mensaje", "Borrador actualizado correctamente.");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            String errorMsg = "No fue posible guardar el borrador. Verifica que el archivo sea válido e inténtalo nuevamente.";
            if (e instanceof ForbiddenException || e instanceof ResourceNotFoundException) {
                errorMsg = e.getMessage();
            }
            if (ajax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", errorMsg);
                return ResponseEntity.badRequest().body(response);
            }
            redirectAttrs.addFlashAttribute("mensaje", errorMsg);
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    @PostMapping("/subir-acuse")
    public Object subirAcuse(@RequestParam("id") Integer id, @RequestParam("archivoAcuse") MultipartFile archivo, @RequestParam(value = "observaciones", required = false) String observaciones, RedirectAttributes redirectAttrs, HttpServletRequest request) {
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        Usuario actor = getUsuarioActual();
        if (!tienePermiso(EstatusTermino.LIBERADO, actor.getRol())) {
            String msg = "No tienes permiso para cargar acuses.";
            if (ajax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", msg);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            redirectAttrs.addFlashAttribute("mensaje", msg);
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }
        try {
            Termino termino = terminoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Término no encontrado"));
            validarAccesoTermino(termino, actor);

            if (termino.getEstatusTermino() == EstatusTermino.LIBERADO) {
                flujoTerminoService.validarPresentacion(termino, TipoEvidenciaPresentacion.ACUSE);
                // Guardar acuse con persistencia avanzada
                TerminoArchivo ta = terminoArchivoService.guardarArchivo(termino, archivo, TipoDocumentoTermino.ACUSE, actor);

                TerminoPresentado presentado = TerminoPresentado.builder()
                        .termino(termino)
                        .expedienteNumero(termino.getExpediente() != null ? termino.getExpediente().getNumero() : "Sin Expediente")
                        .fechaPresentacion(LocalDate.now())
                        .acuseDocumento(ta.getNombreOriginal())
                        .sincronizadoAt(LocalDateTime.now()).build();
                terminoPresentadoRepository.save(presentado);

                termino.setAcuseNombre(ta.getNombreOriginal());
                termino.setTipoEvidenciaPresentacion(TipoEvidenciaPresentacion.ACUSE);
                termino.setEstatusTermino(EstatusTermino.PRESENTADO);
                termino.setFechaPresentacion(LocalDate.now());
                if (observaciones != null) {
                    termino.setObservaciones(observaciones);
                }
                termino.setUpdatedAt(LocalDateTime.now());
                terminoRepository.save(termino);

                String logDesc = "Se ha cargado el acuse: " + archivo.getOriginalFilename();
                if (observaciones != null && !observaciones.trim().isEmpty()) {
                    logDesc += " - Observaciones: " + observaciones;
                }

                if (termino.getExpediente() != null) {
                    auditoriaService.registrar(termino.getExpediente(), "ARCHIVO CARGADO", logDesc, "UPLOAD", actor);
                } else {
                    auditoriaService.registrarActividadGeneral(actor, "ARCHIVO CARGADO", "Acuse de término '" + termino.getAsunto() + "' cargado. " + logDesc, "UPLOAD");
                }

                redirectAttrs.addFlashAttribute("mensaje", "Acuse cargado. El término ahora está Presentado.");
                redirectAttrs.addFlashAttribute("tipo", "success");
                if (ajax) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("mensaje", "Acuse cargado. El término ahora está Presentado.");
                    return ResponseEntity.ok(response);
                }
            } else {
                String msg = "El término no está en la etapa LIBERADO.";
                if (ajax) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("mensaje", msg);
                    return ResponseEntity.badRequest().body(response);
                }
                redirectAttrs.addFlashAttribute("mensaje", msg);
                redirectAttrs.addFlashAttribute("tipo", "error");
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (ajax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "Error: " + errorMsg);
                return ResponseEntity.badRequest().body(response);
            }
            redirectAttrs.addFlashAttribute("mensaje", "Error: " + errorMsg);
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    @PostMapping("/subir-nota-informativa")
    public Object subirNotaInformativa(@RequestParam("id") Integer id,
            @RequestParam("archivoNota") MultipartFile archivo,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            RedirectAttributes redirectAttrs, HttpServletRequest request) {
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        Usuario actor = getUsuarioActual();
        try {
            if (!tienePermiso(EstatusTermino.LIBERADO, actor.getRol())) {
                throw new ForbiddenException("No tienes permiso para registrar la presentación.");
            }
            Termino termino = terminoRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Término no encontrado"));
            validarAccesoTermino(termino, actor);
            if (termino.getEstatusTermino() != EstatusTermino.LIBERADO) {
                throw new IllegalStateException("La Nota Informativa sólo puede cargarse cuando el término está Liberado.");
            }
            flujoTerminoService.validarPresentacion(termino, TipoEvidenciaPresentacion.NOTA_INFORMATIVA);
            TerminoArchivo ta = terminoArchivoService.guardarArchivo(termino, archivo,
                    TipoDocumentoTermino.NOTA_INFORMATIVA, actor);
            terminoPresentadoRepository.save(TerminoPresentado.builder()
                    .termino(termino)
                    .expedienteNumero(termino.getExpediente() != null ? termino.getExpediente().getNumero() : "Sin Expediente")
                    .fechaPresentacion(LocalDate.now())
                    .acuseDocumento(ta.getNombreOriginal())
                    .sincronizadoAt(LocalDateTime.now()).build());
            termino.setAcuseNombre(ta.getNombreOriginal());
            termino.setTipoEvidenciaPresentacion(TipoEvidenciaPresentacion.NOTA_INFORMATIVA);
            termino.setEstatusTermino(EstatusTermino.PRESENTADO);
            termino.setFechaPresentacion(LocalDate.now());
            if (observaciones != null && !observaciones.isBlank()) termino.setObservaciones(observaciones);
            termino.setUpdatedAt(LocalDateTime.now());
            terminoRepository.save(termino);
            String descripcion = "Nota Informativa cargada como evidencia alternativa: " + archivo.getOriginalFilename();
            if (termino.getExpediente() != null) {
                auditoriaService.registrar(termino.getExpediente(), "ARCHIVO CARGADO", descripcion, "UPLOAD", actor);
            } else {
                auditoriaService.registrarActividadGeneral(actor, "ARCHIVO CARGADO", descripcion, "UPLOAD");
            }
            String mensaje = "Nota Informativa cargada. El término ahora está Presentado.";
            if (ajax) return ResponseEntity.ok(Map.of("success", true, "mensaje", mensaje));
            redirectAttrs.addFlashAttribute("mensaje", mensaje);
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception ex) {
            if (ajax) return ResponseEntity.badRequest().body(Map.of("success", false, "mensaje", ex.getMessage()));
            redirectAttrs.addFlashAttribute("mensaje", "Error: " + ex.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    @PostMapping("/concluir")
    public Object concluir(@RequestParam("id") Integer id, @RequestParam(value = "observaciones", required = false) String observaciones, RedirectAttributes redirectAttrs, HttpServletRequest request) {
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        Usuario actor = getUsuarioActual();
        if (!tienePermiso(EstatusTermino.PRESENTADO, actor.getRol())) {
            String msg = "No tienes permiso para finalizar este término.";
            if (ajax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", msg);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            redirectAttrs.addFlashAttribute("mensaje", msg);
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }
        try {
            Termino termino = terminoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Término no encontrado"));
            validarAccesoTermino(termino, actor);
            flujoTerminoService.validarConclusion(termino);

            if (termino.getEstatusTermino() == EstatusTermino.PRESENTADO) {
                termino.setEstatusTermino(EstatusTermino.CONCLUIDO);
                if (observaciones != null) {
                    termino.setObservaciones(observaciones);
                }
                termino.setUpdatedAt(LocalDateTime.now());
                terminoRepository.save(termino);

                notificarCambioDeEtapa(termino, EstatusTermino.PRESENTADO, EstatusTermino.CONCLUIDO, actor);

                String logDesc = "El término ha sido marcado como CONCLUIDO y archivado.";
                if (observaciones != null && !observaciones.trim().isEmpty()) {
                    logDesc += " - Observaciones: " + observaciones;
                }
                if (termino.getExpediente() != null) {
                    auditoriaService.registrar(termino.getExpediente(), "TÉRMINO CONCLUIDO", logDesc, "CHECK", actor);
                } else {
                    auditoriaService.registrarActividadGeneral(actor, "TÉRMINO CONCLUIDO", "Término '" + termino.getAsunto() + "' marcado como CONCLUIDO. " + logDesc, "CHECK");
                }

                redirectAttrs.addFlashAttribute("mensaje", "¡Término finalizado y archivado correctamente!");
                redirectAttrs.addFlashAttribute("tipo", "success");
                if (ajax) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("mensaje", "¡Término finalizado y archivado correctamente!");
                    return ResponseEntity.ok(response);
                }
            } else {
                String msg = "El término no está en la etapa PRESENTADO.";
                if (ajax) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("mensaje", msg);
                    return ResponseEntity.badRequest().body(response);
                }
                redirectAttrs.addFlashAttribute("mensaje", msg);
                redirectAttrs.addFlashAttribute("tipo", "error");
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (ajax) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "Error: " + errorMsg);
                return ResponseEntity.badRequest().body(response);
            }
            redirectAttrs.addFlashAttribute("mensaje", "Error al concluir el término.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    private boolean tienePermiso(EstatusTermino etapaActual, RolUsuario rolUsuario) {
        return true;
    }

    @GetMapping("/previsualizar-plazo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previsualizarPlazo(
            @RequestParam Integer tipoTerminoId,
            @RequestParam LocalDate fechaOrigen,
            @RequestParam(required = false) LocalDate fechaVencimiento) {
        TipoTermino tipo = tipoTerminoService.obtenerPorId(tipoTerminoId);
        ResultadoCalculoPlazo resultado = calculadoraPlazoService.calcularResultado(
                tipo, fechaOrigen, fechaVencimiento);
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("modo", resultado.modo().name());
        respuesta.put("fechaCalculada", resultado.fechaCalculada());
        respuesta.put("fechaAdoptada", resultado.fechaAdoptada());
        respuesta.put("calendarioVersion", resultado.calendarioVersion());
        respuesta.put("explicacion", resultado.explicacion());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/excepcion-fecha")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> registrarExcepcionFecha(
            @PathVariable Integer id,
            @RequestParam LocalDate nuevaFecha,
            @RequestParam String motivo) {
        Usuario actor = getUsuarioActual();
        Termino termino = terminoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El término no existe."));
        validarAccesoTermino(termino, actor);
        if (!puedeAutorizarExcepcion(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Sólo una jefatura o nivel superior puede autorizar una excepción de fecha.");
        }
        if (termino.getEstatusTermino() == EstatusTermino.PRESENTADO
                || termino.getEstatusTermino() == EstatusTermino.CONCLUIDO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cambiar el vencimiento de un término presentado o concluido.");
        }
        LocalDate origen = termino.getFechaIngreso() != null ? termino.getFechaIngreso() : termino.getFechaCreacion();
        if (nuevaFecha == null || origen == null || !nuevaFecha.isAfter(origen)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nueva fecha debe ser posterior a la fecha origen.");
        }
        if (motivo == null || motivo.isBlank() || motivo.trim().length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Describe el motivo de la excepción con al menos 10 caracteres.");
        }
        LocalDate fechaAnterior = termino.getFechaVencimiento();
        termino.setFechaVencimiento(nuevaFecha);
        termino.setPlazoFechaAdoptada(nuevaFecha);
        termino.setPlazoExcepcionMotivo(motivo.trim());
        termino.setPlazoExcepcionAutorId(actor.getId());
        termino.setPlazoExcepcionAutor(actor.getNombreCompleto());
        termino.setPlazoExcepcionFecha(LocalDateTime.now());
        termino.setUpdatedAt(LocalDateTime.now());
        evaluadorAtencionTemporalService.recalcularUmbralesDesdeSnapshot(termino);
        terminoRepository.save(termino);

        String detalle = "Se autorizó una excepción de vencimiento de " + fechaAnterior + " a " + nuevaFecha
                + ". Motivo: " + motivo.trim();
        if (termino.getExpediente() != null) {
            auditoriaService.registrar(termino.getExpediente(), "Excepción de plazo", detalle, "CALENDAR", actor);
        } else {
            auditoriaService.registrarActividadGeneral(actor, "Excepción de plazo", detalle, "CALENDAR");
        }
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "La nueva fecha fue autorizada y quedó registrada en la trazabilidad.");
        respuesta.put("fechaAnterior", fechaAnterior);
        respuesta.put("nuevaFecha", nuevaFecha);
        return ResponseEntity.ok(respuesta);
    }

    private boolean puedeAutorizarExcepcion(Usuario usuario) {
        return usuario.getRol() == RolUsuario.ADMIN
                || usuario.getRol() == RolUsuario.DIRECTOR_PRINCIPAL
                || usuario.getRol() == RolUsuario.DIRECTOR
                || usuario.getRol() == RolUsuario.SUBDIRECTOR
                || usuario.getRol() == RolUsuario.GERENTE
                || usuario.getRol() == RolUsuario.JEFE_DEPTO;
    }

    private Usuario getUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable("id") Integer id) {
        try {
            Termino termino = terminoRepository.findById(id).orElse(null);
            if (termino == null) {
                return ResponseEntity.notFound().build();
            }

            validarAccesoTermino(termino, getUsuarioActual());

            // 1. Intentar persistencia avanzada (Borrador)
            Optional<TerminoArchivo> taOpt = terminoArchivoService.obtenerUltimoPorTipo(id, TipoDocumentoTermino.BORRADOR);
            if (taOpt.isPresent()) {
                TerminoArchivo ta = taOpt.get();
                Path rutaArchivo = fileStorageService.obtenerArchivo(ta.getRutaArchivo());
                Resource recurso = new UrlResource(rutaArchivo.toUri());
                if (recurso.exists() || recurso.isReadable()) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ta.getNombreOriginal() + "\"")
                            .body(recurso);
                }
            }

            // 2. Fallback a persistencia clásica
            if (termino.getAcuse() == null) {
                return ResponseEntity.notFound().build();
            }
            Path rutaArchivo = fileStorageService.obtenerArchivo(rutaLegadoTermino(termino.getAcuse(), "terminos"));
            Resource recurso = new UrlResource(rutaArchivo.toUri());
            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + (termino.getAcuseNombre() != null ? termino.getAcuseNombre() : recurso.getFilename()) + "\"")
                        .body(recurso);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exportar-excel")
    public void exportarExcel(HttpServletResponse response,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EstatusTermino estatus,
            @RequestParam(required = false) Integer prioridadId,
            @RequestParam(required = false) Integer abogadoId,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) PeriodoFiltro periodo,
            @RequestParam(required = false) Integer materiaId) throws IOException {

        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());
        response.setHeader("Content-Disposition", "attachment; filename=Terminos_" + currentDateTime + ".xlsx");

        Usuario usuarioActual = getUsuarioActual();
        Integer secGerenciaId = null;
        List<Integer> secMateriaIds = null;
        Integer secUsuarioId = usuarioActual.getId();

        if (usuarioActual.getRol() == RolUsuario.ADMIN || usuarioActual.getRol() == RolUsuario.DIRECTOR || usuarioActual.getRol() == RolUsuario.SUBDIRECTOR) {
        } else if (usuarioActual.getRol() == RolUsuario.GERENTE) {
            if (usuarioActual.getGerencia() != null) {
                secGerenciaId = usuarioActual.getGerencia().getId();
            }
        } else if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            if (usuarioActual.getGerencia() != null) {
                secGerenciaId = usuarioActual.getGerencia().getId();
            }
            if (usuarioActual.getMaterias() != null && !usuarioActual.getMaterias().isEmpty()) {
                secMateriaIds = usuarioActual.getMaterias().stream().map(m -> m.getId()).collect(Collectors.toList());
            } else {
                secMateriaIds = Collections.emptyList();
            }
        } else if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
            secGerenciaId = null;
            secMateriaIds = Collections.emptyList();
        } else {
            if (usuarioActual.getGerencia() != null) {
                secGerenciaId = usuarioActual.getGerencia().getId();
            }
            secMateriaIds = Collections.emptyList();
        }

        LocalDate fechaInicio = LocalDate.of(1900, 1, 1);
        LocalDate fechaFin = LocalDate.of(2100, 12, 31);
        if (periodo != null) {
            LocalDate[] rango = periodo.calcularRango();
            fechaInicio = rango[0];
            fechaFin = rango[1];
        }

        List<Termino> listaTerminos = terminoRepository.listarParaExcel(
                id, keyword, estatus, prioridadId, abogadoId, gerenciaId, fechaInicio, fechaFin, materiaId,
                secGerenciaId, secMateriaIds, secUsuarioId);

        TerminoExcelExporter excelExporter = new TerminoExcelExporter(listaTerminos);
        excelExporter.export(response);
    }

    @PostMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> eliminar(@PathVariable("id") Integer id) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            Usuario actor = getUsuarioActual();

            Termino termino = terminoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Término no existe."));
            validarPermisoEliminarTermino(termino, actor);
            String referencia = termino.getNumeroReferencia();
            Materia materia = termino.getMateriaReferencia();
            com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad prioridad = termino.getPrioridad();

            // 1. Eliminar archivos asociados y registros en TerminoArchivo
            terminoArchivoService.eliminarArchivosPorTermino(id);

            // 2. Eliminar acuses presentados
            List<TerminoPresentado> presentados = terminoPresentadoRepository.findByTerminoId(id);
            if (presentados != null && !presentados.isEmpty()) {
                terminoPresentadoRepository.deleteAll(presentados);
            }

            // 3. Eliminar el término principal
            terminoRepository.delete(termino);
            notificacionService.notificarSuperioresDeEliminacion(
                    actor,
                    NotificacionEvento.TERMINO_CAMBIO_ETAPA,
                    "Termino eliminado",
                    "El usuario " + actor.getNombreCompleto() + " elimino el termino " + referencia + ".",
                    prioridad,
                    String.valueOf(id),
                    materia);
            response.put("success", true);
            response.put("message", "Término eliminado correctamente.");
            return ResponseEntity.ok(response);
        } catch (ForbiddenException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/descargar-acuse/{id}")
    public ResponseEntity<Resource> descargarAcuse(@PathVariable("id") Integer id) {
        try {
            Termino termino = terminoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Término no encontrado"));
            validarAccesoTermino(termino, getUsuarioActual());

            TipoDocumentoTermino tipoEvidencia = termino.getTipoEvidenciaPresentacion()
                    == TipoEvidenciaPresentacion.NOTA_INFORMATIVA
                    ? TipoDocumentoTermino.NOTA_INFORMATIVA
                    : TipoDocumentoTermino.ACUSE;
            Optional<TerminoArchivo> taOpt = terminoArchivoService.obtenerUltimoPorTipo(id, tipoEvidencia);
            if (taOpt.isPresent()) {
                TerminoArchivo ta = taOpt.get();
                Path rutaArchivo = fileStorageService.obtenerArchivo(ta.getRutaArchivo());
                Resource recurso = new UrlResource(rutaArchivo.toUri());
                if (recurso.exists() || recurso.isReadable()) {
                    return respuestaDocumento(rutaArchivo, recurso, ta.getNombreOriginal(), false);
                }
            }

            // 2. Fallback a persistencia clásica
            List<TerminoPresentado> presentados;
            if (termino.getExpediente() != null) {
                presentados = terminoPresentadoRepository.findByTerminoExpedienteId(termino.getExpediente().getId());
            } else {
                presentados = terminoPresentadoRepository.findByTerminoId(id);
            }
            TerminoPresentado acuse = presentados.stream()
                    .filter(p -> p.getTermino().getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Acuse no encontrado"));

            if (acuse.getAcuseDocumento() == null) {
                return ResponseEntity.notFound().build();
            }

            Path rutaArchivo = fileStorageService.obtenerArchivo(rutaLegadoTermino(acuse.getAcuseDocumento(), "acuses"));
            Resource recurso = new UrlResource(rutaArchivo.toUri());
            if (recurso.exists() || recurso.isReadable()) {
                return respuestaDocumento(rutaArchivo, recurso, recurso.getFilename(), false);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previsualizarArchivo(@PathVariable("id") Integer id) {
        return servirArchivoTermino(id, false);
    }

    private String rutaLegadoTermino(String nombreArchivo, String carpeta) {
        String rutaNormalizada = nombreArchivo.replace("\\", "/");
        if (rutaNormalizada.startsWith(carpeta + "/")) {
            return rutaNormalizada;
        }
        return carpeta + "/" + java.nio.file.Paths.get(nombreArchivo).getFileName().toString();
    }

    private ResponseEntity<Resource> servirArchivoTermino(Integer id, boolean descarga) {
        try {
            Termino termino = terminoRepository.findById(id).orElse(null);
            if (termino == null) {
                return ResponseEntity.notFound().build();
            }

            validarAccesoTermino(termino, getUsuarioActual());

            Optional<TerminoArchivo> taOpt = terminoArchivoService.obtenerUltimoPorTipo(id, TipoDocumentoTermino.BORRADOR);
            if (taOpt.isPresent()) {
                TerminoArchivo ta = taOpt.get();
                Path rutaArchivo = fileStorageService.obtenerArchivo(ta.getRutaArchivo());
                Resource recurso = new UrlResource(rutaArchivo.toUri());
                if (recurso.exists() || recurso.isReadable()) {
                    return respuestaDocumento(rutaArchivo, recurso, ta.getNombreOriginal(), descarga);
                }
            }

            if (termino.getAcuse() == null) {
                return ResponseEntity.notFound().build();
            }
            Path rutaArchivo = fileStorageService.obtenerArchivo(rutaLegadoTermino(termino.getAcuse(), "terminos"));
            Resource recurso = new UrlResource(rutaArchivo.toUri());
            if (recurso.exists() || recurso.isReadable()) {
                String nombre = termino.getAcuse() != null ? termino.getAcuse() : recurso.getFilename();
                return respuestaDocumento(rutaArchivo, recurso, nombre, descarga);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private void validarPermisoEliminarTermino(Termino termino, Usuario actor) {
        RolUsuario rol = actor.getRol();
        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR
                || rol == RolUsuario.GERENTE || rol == RolUsuario.JEFE_DEPTO) {
            validarAccesoTermino(termino, actor);
            return;
        }

        if (rol == RolUsuario.ABOGADO) {
            Expediente expediente = termino.getExpediente();
            if (expediente != null && expediente.getAbogadoResponsable() != null
                    && expediente.getAbogadoResponsable().getId().equals(actor.getId())) {
                return;
            }
            throw new ForbiddenException("Solo el abogado responsable del expediente puede eliminar este término.");
        }

        throw new ForbiddenException("No tienes permisos para eliminar este término.");
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerTermino(@PathVariable("id") Integer id) {
        try {
            Termino termino = terminoRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Término no encontrado"));
            validarAccesoTermino(termino, getUsuarioActual());

            Map<String, Object> res = new HashMap<>();
            res.put("id", termino.getId());
            res.put("asunto", termino.getAsunto());
            res.put("fechaVencimiento", termino.getFechaVencimiento() != null ? termino.getFechaVencimiento().toString() : "");
            res.put("expedienteId", termino.getExpediente() != null ? termino.getExpediente().getId().toString() : "");
            res.put("expedienteNum", termino.getExpediente() != null ? termino.getExpediente().getNumero() : "");
            res.put("abogadoId", termino.getAbogadoResponsable() != null ? termino.getAbogadoResponsable().getId() : "");
            res.put("prioridad", termino.getPrioridad() != null ? termino.getPrioridad().getId() : "");
            res.put("observaciones", termino.getObservaciones() != null ? termino.getObservaciones() : "");
            TerminoContexto ctx = termino.getContexto();
            res.put("origenTermino", ctx != null && ctx.getOrigen() != null ? ctx.getOrigen().name() : "EXPEDIENTE");
            res.put("materiaId", ctx != null && ctx.getMateria() != null ? ctx.getMateria().getId() : "");
            res.put("gerenciaId", ctx != null && ctx.getGerencia() != null ? ctx.getGerencia().getId() : "");
            res.put("descripcion", ctx != null && ctx.getDescripcion() != null ? ctx.getDescripcion() : "");
            res.put("partes", ctx != null && ctx.getPartes() != null ? ctx.getPartes() : "");
            res.put("referencia", ctx != null && ctx.getNumeroExpedienteSnapshot() != null ? ctx.getNumeroExpedienteSnapshot() : "");

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/responsable-expediente/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerResponsable(@PathVariable("id") String id) {
        try {
            Expediente exp = expedienteRepository.findById(UUID.fromString(id)).orElseThrow(() -> new RuntimeException("Expediente no encontrado"));
            validarAccesoExpediente(exp, getUsuarioActual());

            Usuario responsable = exp.getAbogadoResponsable();
            Map<String, Object> respuesta = new HashMap<>();
            if (responsable != null) {
                respuesta.put("id", responsable.getId());
                respuesta.put("nombre", responsable.getNombreCompleto());
                return ResponseEntity.ok(respuesta);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private void validarAccesoTermino(Termino termino, Usuario usuario) {
        if (usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECTOR_PRINCIPAL 
                || usuario.getRol() == RolUsuario.DIRECTOR || usuario.getRol() == RolUsuario.SUBDIRECTOR) {
            return;
        }
        if (termino.getAbogadoResponsable() != null && termino.getAbogadoResponsable().getId().equals(usuario.getId())) {
            return;
        }
        Gerencia gt = termino.getGerenciaReferencia();
        if (usuario.getRol() == RolUsuario.GERENTE && gt != null && usuario.getGerencia() != null
                && gt.getId().equals(usuario.getGerencia().getId())) {
            return;
        }
        if (usuario.getRol() == RolUsuario.JEFE_DEPTO && gt != null && usuario.getGerencia() != null
                && gt.getId().equals(usuario.getGerencia().getId())
                && termino.getMateriaReferencia() != null
                && usuario.tieneMateria(termino.getMateriaReferencia().getId())) return;
        Expediente e = termino.getExpediente();
        if (e != null) {
            validarAccesoExpediente(e, usuario);
            return;
        }
        if (termino.getAsuntoIndependiente() != null) {
            asuntoIndependienteService.validarAcceso(termino.getAsuntoIndependiente(), usuario);
            return;
        }
        throw new ForbiddenException("No tienes acceso a este término.");
    }

    private void validarAccesoExpediente(Expediente e, Usuario usuario) {
        if (!puedeAccederExpediente(e, usuario)) {
            throw new ForbiddenException("No tienes acceso a este expediente.");
        }
    }

    private boolean puedeAccederExpediente(Expediente e, Usuario usuario) {
        RolUsuario rol = usuario.getRol();
        boolean tienePermiso = false;

        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL 
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR) {
            tienePermiso = true;
        } else if (rol == RolUsuario.GERENTE) {
            if (e.getGerencia() != null && usuario.getGerencia() != null 
                    && e.getGerencia().getId().equals(usuario.getGerencia().getId())) {
                tienePermiso = true;
            }
        } else if (rol == RolUsuario.JEFE_DEPTO) {
            if (e.getGerencia() != null && usuario.getGerencia() != null 
                    && e.getGerencia().getId().equals(usuario.getGerencia().getId())
                    && e.getMateria() != null 
                    && usuario.getMaterias().stream().anyMatch(m -> m.getId().equals(e.getMateria().getId()))) {
                tienePermiso = true;
            }
        } else if (rol == RolUsuario.ABOGADO) {
            if (e.getAbogadoResponsable() != null && e.getAbogadoResponsable().getId().equals(usuario.getId())) {
                tienePermiso = true;
            } else if (colaboradorExpedienteRepository.existsByExpedienteIdAndUsuarioIdAndFechaExpiracionAfter(
                    e.getId(), usuario.getId(), LocalDateTime.now())) {
                tienePermiso = true;
            }
        }

        return tienePermiso;
    }

    private ResponseEntity<Resource> respuestaDocumento(Path rutaArchivo, Resource recurso,
            String nombreOriginal, boolean descargaSolicitada) throws IOException {
        String tipoMime = Files.probeContentType(rutaArchivo);
        if (tipoMime == null && nombreOriginal != null && nombreOriginal.toLowerCase().endsWith(".pdf")) {
            tipoMime = MediaType.APPLICATION_PDF_VALUE;
        }
        boolean esPdf = MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(tipoMime);
        String disposicion = !descargaSolicitada && esPdf ? "inline" : "attachment";
        String nombreSeguro = nombreOriginal == null ? "documento" : nombreOriginal.replace("\"", "");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipoMime != null ? tipoMime : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion + "; filename=\"" + nombreSeguro + "\"")
                .body(recurso);
    }

    @GetMapping("/api/materias-por-gerencia/{gerenciaId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerMateriasPorGerencia(@PathVariable("gerenciaId") Integer gerenciaId) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            List<Materia> materias = materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(gerenciaId);
            
            if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
                Set<Integer> misMateriasIds = usuarioActual.getMaterias().stream()
                        .map(Materia::getId)
                        .collect(Collectors.toSet());
                materias = materias.stream()
                        .filter(m -> misMateriasIds.contains(m.getId()))
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> respuesta = materias.stream().map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("nombre", m.getNombre());
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/abogados-por-gerencia/{gerenciaId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerAbogadosPorGerencia(@PathVariable("gerenciaId") Integer gerenciaId) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            List<Usuario> abogados = usuarioRepository.findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(gerenciaId).stream()
                    .filter(u -> u.getRol() == RolUsuario.ABOGADO)
                    .collect(Collectors.toList());

            if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
                Set<Integer> misMateriasIds = usuarioActual.getMaterias().stream()
                        .map(Materia::getId)
                        .collect(Collectors.toSet());
                abogados = abogados.stream()
                        .filter(u -> u.getMaterias().stream().anyMatch(m -> misMateriasIds.contains(m.getId())))
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> respuesta = abogados.stream().map(a -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", a.getId());
                map.put("nombre", a.getNombreCompleto());
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
// TerminosController - Version VsCode dentro de antigravity - lineas 960
