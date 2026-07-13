package com.juridico.sistema_juridico.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
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
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
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
import com.juridico.sistema_juridico.service.FileStorageService;
import com.juridico.sistema_juridico.service.configuracion.ConfiguracionSemaforoService;
import com.juridico.sistema_juridico.Entity.enums.TipoDocumentoTermino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoArchivo;

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
    private ConfiguracionSemaforoService configuracionSemaforoService;

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

        configuracionSemaforoService.aplicarATerminos(paginaTerminos.getContent());
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
        model.addAttribute("listaEstatus", EstatusTermino.values());

        // --- 5. LOGICA ANTI-FANTASMAS (EXPEDIENTES DISPONIBLES) - OPTIMIZADA ---
        List<Expediente> expedientesParaDropdown;

        if (usuarioActual.getRol() == RolUsuario.ABOGADO || usuarioActual.getRol() == RolUsuario.GERENTE) {
            if (usuarioActual.getGerencia() != null) {
                expedientesParaDropdown = expedienteRepository.findAllByGerenciaId(usuarioActual.getGerencia().getId());
            } else {
                expedientesParaDropdown = Collections.emptyList();
            }
        } else if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            if (usuarioActual.getMaterias() != null && !usuarioActual.getMaterias().isEmpty()) {
                List<Integer> materiaIds = usuarioActual.getMaterias().stream().map(m -> m.getId()).collect(Collectors.toList());
                expedientesParaDropdown = expedienteRepository.findAllByMateriaIdIn(materiaIds);
            } else {
                expedientesParaDropdown = Collections.emptyList();
            }
        } else {
            expedientesParaDropdown = expedienteRepository.findAll();
        }
        model.addAttribute("expedientesList", expedientesParaDropdown);

        model.addAttribute("rolActual", usuarioActual.getRol());
        model.addAttribute("mapaPermisos", PERMISOS_ETAPAS);
        model.addAttribute("ordenEtapas", ORDEN_ETAPAS);

        // Mantener valores
        model.addAttribute("keyword", keyword);
        model.addAttribute("paramEstatus", estatus);
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

        Map<EstatusTermino, Long> conteosMap = new EnumMap<>(EstatusTermino.class);
        for (EstatusTermino e : EstatusTermino.values()) {
            conteosMap.put(e, 0L);
        }
        for (Object[] row : rawCounts) {
            if (row[0] != null) {
                conteosMap.put((EstatusTermino) row[0], (Long) row[1]);
            }
        }
        model.addAttribute("conteosEtapas", conteosMap);

        // --- 7. TOTALES EN PROCESO ---
        long totalesEnProceso = 0;
        for (Map.Entry<EstatusTermino, Long> entry : conteosMap.entrySet()) {
            if (entry.getKey() != EstatusTermino.CONCLUIDO) {
                totalesEnProceso += entry.getValue();
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
            }
            return "views/terminos/index :: gridMaestroTerminos";
        }

        return "views/terminos/index";
    }

    private List<EstatusTermino> getEtapasAsignadasPorRol(RolUsuario rol) {
        return Arrays.asList(EstatusTermino.values());
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Termino terminoForm,
            @RequestParam(value = "expedienteId", required = false) UUID expedienteId,
            @RequestParam(value = "abogadoId", required = false) Integer abogadoId,
            @RequestParam(value = "origenTermino", required = false, defaultValue = "EXPEDIENTE") OrigenTermino origenTermino,
            @RequestParam(value = "contextoMateriaId", required = false) Integer contextoMateriaId,
            @RequestParam(value = "contextoGerenciaId", required = false) Integer contextoGerenciaId,
            @RequestParam(value = "contextoDescripcion", required = false) String contextoDescripcion,
            @RequestParam(value = "contextoPartes", required = false) String contextoPartes,
            @RequestParam(value = "contextoNumeroReferencia", required = false) String contextoNumeroReferencia,
            RedirectAttributes redirectAttrs) {

        Usuario actor = getUsuarioActual();
        Termino terminoGuardar;
        boolean esNuevo = false;

        if (terminoForm.getAsunto() == null || terminoForm.getAsunto().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo asunto/actuación es obligatorio.");
        }
        if (terminoForm.getFechaVencimiento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de vencimiento es obligatoria.");
        }

        if (actor.getRol() == RolUsuario.ABOGADO) {
            abogadoId = actor.getId();
        }

        if (origenTermino == OrigenTermino.ASUNTO_UNICO) {
            if (actor.getRol() == RolUsuario.JEFE_DEPTO) {
                Set<Integer> misMateriasIds = actor.getMaterias().stream()
                        .map(Materia::getId)
                        .collect(Collectors.toSet());
                if (contextoMateriaId == null || !misMateriasIds.contains(contextoMateriaId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tienes acceso a la materia seleccionada.");
                }
            } else if (actor.getRol() == RolUsuario.ABOGADO || actor.getRol() == RolUsuario.GERENTE) {
                if (actor.getGerencia() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no tiene una gerencia asignada.");
                }
                Materia mat = materiaRepository.findById(contextoMateriaId).orElse(null);
                if (mat == null || mat.getGerencia() == null || !mat.getGerencia().getId().equals(actor.getGerencia().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tienes acceso a la materia seleccionada.");
                }
            }
            if (actor.getGerencia() != null) {
                contextoGerenciaId = actor.getGerencia().getId();
            }
            terminoForm.setContexto(crearContextoAsuntoUnico(contextoMateriaId, contextoGerenciaId,
                    contextoDescripcion, contextoPartes, contextoNumeroReferencia));
        } else if (expedienteId != null) {
            Expediente exp = expedienteRepository.findById(expedienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("El expediente asociado no existe."));
            validarAccesoExpediente(exp, actor);
            terminoForm.setExpediente(exp);
            terminoForm.setContexto(crearContextoDesdeExpediente(exp));
        }

        if (abogadoId != null) {
            Usuario abogado = usuarioRepository.findById(abogadoId)
                    .orElseThrow(() -> new ResourceNotFoundException("El abogado asignado no existe."));
            terminoForm.setAbogadoResponsable(abogado);
        } else if (terminoForm.getId() == null && origenTermino == OrigenTermino.ASUNTO_UNICO) {
            terminoForm.setAbogadoResponsable(actor);
        }

        if (origenTermino == OrigenTermino.ASUNTO_UNICO) {
            Expediente expTemporal = null;
            String ref = (contextoNumeroReferencia != null && !contextoNumeroReferencia.trim().isEmpty())
                    ? contextoNumeroReferencia.trim()
                    : null;
            if (ref != null) {
                expTemporal = expedienteRepository.findByNumeroIgnoreCaseAndEsTemporal(ref, true).orElse(null);
            }
            if (expTemporal == null) {
                expTemporal = new Expediente();
                expTemporal.setNumero(ref != null ? ref : "TEMP-" + java.util.UUID.randomUUID().toString().substring(0, 8));
                expTemporal.setEsTemporal(true);
                if (contextoMateriaId != null) {
                    expTemporal.setMateria(materiaRepository.findById(contextoMateriaId).orElse(null));
                }
                if (contextoGerenciaId != null) {
                    expTemporal.setGerencia(gerenciaRepository.findById(contextoGerenciaId).orElse(null));
                } else if (actor.getGerencia() != null) {
                    expTemporal.setGerencia(actor.getGerencia());
                }
                expTemporal.setDescripcion(contextoDescripcion != null ? contextoDescripcion : "Asunto Único");
                expTemporal.setPartes(contextoPartes);
                expTemporal.setAbogadoResponsable(terminoForm.getAbogadoResponsable() != null ? terminoForm.getAbogadoResponsable() : actor);
                expTemporal = expedienteRepository.save(expTemporal);
            } else {
                if (contextoGerenciaId != null) {
                    expTemporal.setGerencia(gerenciaRepository.findById(contextoGerenciaId).orElse(null));
                }
                if (contextoMateriaId != null) {
                    expTemporal.setMateria(materiaRepository.findById(contextoMateriaId).orElse(null));
                }
                if (contextoDescripcion != null) {
                    expTemporal.setDescripcion(contextoDescripcion);
                }
                if (contextoPartes != null) {
                    expTemporal.setPartes(contextoPartes);
                }
                if (terminoForm.getAbogadoResponsable() != null) {
                    expTemporal.setAbogadoResponsable(terminoForm.getAbogadoResponsable());
                }
                expTemporal = expedienteRepository.save(expTemporal);
            }
            terminoForm.setExpediente(expTemporal);
        }

        boolean expedienteCambiado = false;
        if (terminoForm.getId() != null) {
            Termino existente = terminoRepository.findById(terminoForm.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("El término que intentas editar no existe."));

            validarAccesoTermino(existente, actor);

            existente.setAsunto(terminoForm.getAsunto());
            existente.setFechaVencimiento(terminoForm.getFechaVencimiento());

            if (terminoForm.getAbogadoResponsable() != null) {
                existente.setAbogadoResponsable(terminoForm.getAbogadoResponsable());
            }

            Expediente expAnterior = existente.getExpediente();
            if (origenTermino == OrigenTermino.ASUNTO_UNICO) {
                if (expAnterior == null || !expAnterior.getId().equals(terminoForm.getExpediente().getId())) {
                    expedienteCambiado = true;
                }
                existente.setExpediente(terminoForm.getExpediente());
                existente.setContexto(terminoForm.getContexto());
            } else if (terminoForm.getExpediente() != null) {
                if (expAnterior == null || !expAnterior.getId().equals(terminoForm.getExpediente().getId())) {
                    expedienteCambiado = true;
                }
                existente.setExpediente(terminoForm.getExpediente());
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
            if (origenTermino != OrigenTermino.ASUNTO_UNICO && expedienteId == null && terminoForm.getExpediente() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El expediente asociado es obligatorio para crear un término.");
            }
            esNuevo = true;
            terminoGuardar = terminoForm;
            terminoGuardar.setFechaCreacion(LocalDate.now());
            terminoGuardar.setFechaIngreso(LocalDate.now());
            terminoGuardar.setEstatusTermino(EstatusTermino.PROYECTISTA);
            terminoGuardar.setCreatedAt(LocalDateTime.now());
            terminoGuardar.setUpdatedAt(LocalDateTime.now());

            if (terminoGuardar.getAbogadoResponsable() == null && terminoGuardar.getExpediente() != null) {
                terminoGuardar.setAbogadoResponsable(terminoGuardar.getExpediente().getAbogadoResponsable());
            }
            if (terminoGuardar.getPrioridad() == null && terminoGuardar.getExpediente() != null) {
                terminoGuardar.setPrioridad(terminoGuardar.getExpediente().getPrioridad());
            }

            validarAccesoTermino(terminoGuardar, actor);
        }

        Termino terminoGuardado = terminoRepository.save(terminoGuardar);
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
        return "redirect:/terminos";
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

        if (etapaAnterior == EstatusTermino.PROYECTISTA && termino.getAcuse() == null) {
            redirectAttrs.addFlashAttribute("mensaje", "Sube el archivo Word antes de avanzar.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }

        EstatusTermino siguienteEtapa = calcularSiguienteEstado(etapaAnterior);
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
    public String subirArchivo(@RequestParam("id") Integer id, @RequestParam("archivo") MultipartFile archivo, RedirectAttributes redirectAttrs) {
        if (archivo.isEmpty()) {
            redirectAttrs.addFlashAttribute("mensaje", "Por favor selecciona un archivo.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }
        try {
            Termino termino = terminoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Término no encontrado"));
            Usuario actor = getUsuarioActual();
            validarAccesoTermino(termino, actor);

            if (!tienePermiso(termino.getEstatusTermino(), actor.getRol())) {
                redirectAttrs.addFlashAttribute("mensaje", "⛔ No tienes permiso para modificar archivos en la etapa: " + termino.getEstatusTermino());
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            EstatusTermino st = termino.getEstatusTermino();
            if (st == EstatusTermino.LIBERADO || st == EstatusTermino.PRESENTADO || st == EstatusTermino.CONCLUIDO) {
                redirectAttrs.addFlashAttribute("mensaje", "El documento está bloqueado en esta etapa.");
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
        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al guardar el archivo: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    @PostMapping("/subir-acuse")
    public String subirAcuse(@RequestParam("id") Integer id, @RequestParam("archivoAcuse") MultipartFile archivo, @RequestParam(value = "observaciones", required = false) String observaciones, RedirectAttributes redirectAttrs) {
        Usuario actor = getUsuarioActual();
        if (!tienePermiso(EstatusTermino.LIBERADO, actor.getRol())) {
            redirectAttrs.addFlashAttribute("mensaje", "No tienes permiso para cargar acuses.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }
        try {
            Termino termino = terminoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Término no encontrado"));
            validarAccesoTermino(termino, actor);

            if (termino.getEstatusTermino() == EstatusTermino.LIBERADO) {
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

                redirectAttrs.addFlashAttribute("mensaje", "Acuse cargado correctamente. Ahora puedes avanzar el término a 'Presentado'.");
                redirectAttrs.addFlashAttribute("tipo", "success");
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    @PostMapping("/concluir")
    public String concluir(@RequestParam("id") Integer id, @RequestParam(value = "observaciones", required = false) String observaciones, RedirectAttributes redirectAttrs) {
        Usuario actor = getUsuarioActual();
        if (!tienePermiso(EstatusTermino.PRESENTADO, actor.getRol())) {
            redirectAttrs.addFlashAttribute("mensaje", "No tienes permiso para finalizar este término.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }
        try {
            Termino termino = terminoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Término no encontrado"));
            validarAccesoTermino(termino, actor);

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
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al concluir el término.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    private boolean tienePermiso(EstatusTermino etapaActual, RolUsuario rolUsuario) {
        return true;
    }

    private EstatusTermino calcularSiguienteEstado(EstatusTermino actual) {
        if (actual == null) {
            return EstatusTermino.PROYECTISTA;
        }
        switch (actual) {
            case PROYECTISTA:
                return EstatusTermino.REVISION;
            case REVISION:
                return EstatusTermino.GERENCIA;
            case GERENCIA:
                return EstatusTermino.DIRECCION;
            case DIRECCION:
                return EstatusTermino.LIBERADO;
            case LIBERADO:
                return EstatusTermino.PRESENTADO;
            default:
                return actual;
        }
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
            Path rutaArchivo = Paths.get("uploads/terminos").resolve(termino.getAcuse());
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
            if (actor.getRol() == RolUsuario.ABOGADO) {
                response.put("success", false);
                response.put("message", "Los abogados no tienen permiso para eliminar términos.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            Termino termino = terminoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Término no existe."));
            validarAccesoTermino(termino, actor);

            // 1. Eliminar archivos asociados y registros en TerminoArchivo
            terminoArchivoService.eliminarArchivosPorTermino(id);

            // 2. Eliminar acuses presentados
            List<TerminoPresentado> presentados = terminoPresentadoRepository.findByTerminoId(id);
            if (presentados != null && !presentados.isEmpty()) {
                terminoPresentadoRepository.deleteAll(presentados);
            }

            // 3. Eliminar el término principal
            terminoRepository.delete(termino);
            response.put("success", true);
            response.put("message", "Término eliminado correctamente.");
            return ResponseEntity.ok(response);
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

            // 1. Intentar persistencia avanzada (Acuse)
            Optional<TerminoArchivo> taOpt = terminoArchivoService.obtenerUltimoPorTipo(id, TipoDocumentoTermino.ACUSE);
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

            Path rutaArchivo = Paths.get("uploads/acuses").resolve(acuse.getAcuseDocumento());
            Resource recurso = new UrlResource(rutaArchivo.toUri());
            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"")
                        .body(recurso);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
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
        Gerencia gt = termino.getGerenciaReferencia();
        if (gt != null && usuario.getGerencia() != null && gt.getId().equals(usuario.getGerencia().getId())) {
            return;
        }
        Expediente e = termino.getExpediente();
        if (e != null) {
            validarAccesoExpediente(e, usuario);
            return;
        }
        throw new ForbiddenException("No tienes acceso a este término.");
    }

    private void validarAccesoExpediente(Expediente e, Usuario usuario) {
        RolUsuario rol = usuario.getRol();
        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL 
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR) {
            return;
        }
        if (usuario.getGerencia() != null && e.getGerencia() != null && e.getGerencia().getId().equals(usuario.getGerencia().getId())) {
            return;
        }
        throw new ForbiddenException("No tienes acceso a este expediente.");
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
