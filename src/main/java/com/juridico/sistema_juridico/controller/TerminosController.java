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
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/terminos")
public class TerminosController {

    @Autowired
    private TerminoRepository terminoRepository;
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
    private GerenciaRepository gerenciaRepository;
    @Autowired
    private MateriaRepository materiaRepository;
    @Autowired
    private CatPrioridadRepository catPrioridadRepository;
    @Autowired
    private AuditoriaService auditoriaService;

    // --- MATRIZ DE PERMISOS ---
    private static final Map<EstatusTermino, List<RolUsuario>> PERMISOS_ETAPAS = new HashMap<>();

    static {
        PERMISOS_ETAPAS.put(EstatusTermino.PROYECTISTA,
                Arrays.asList(RolUsuario.ABOGADO, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO,
                        RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION, RolUsuario.ADMIN));
        PERMISOS_ETAPAS.put(EstatusTermino.REVISION,
                Arrays.asList(RolUsuario.JEFE_DEPTO, RolUsuario.GERENTE, RolUsuario.DIRECCION,
                        RolUsuario.SUBDIRECCION, RolUsuario.ADMIN));
        PERMISOS_ETAPAS.put(EstatusTermino.GERENCIA,
                Arrays.asList(RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION, RolUsuario.ADMIN));
        PERMISOS_ETAPAS.put(EstatusTermino.DIRECCION, Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION, RolUsuario.ADMIN));
        PERMISOS_ETAPAS.put(EstatusTermino.LIBERADO,
                Arrays.asList(RolUsuario.ABOGADO, RolUsuario.JEFE_DEPTO, RolUsuario.GERENTE,
                        RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION, RolUsuario.ADMIN));
        PERMISOS_ETAPAS.put(EstatusTermino.PRESENTADO, Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION,
                RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ADMIN));
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

    @GetMapping
    public String index(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EstatusTermino estatus,
            @RequestParam(required = false) Integer prioridadId,
            @RequestParam(required = false) Integer abogadoId,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) PeriodoFiltro periodo,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {

        // --- 1. LIMPIEZA DE URL (Fix Notificaciones) ---
        if (keyword != null && keyword.contains("?keyword=")) {
            keyword = keyword.substring(keyword.lastIndexOf("=") + 1);
        }

        Usuario usuarioActual = getUsuarioActual();
        Pageable pageable = PageRequest.of(page, 10, Sort.by("fechaVencimiento").descending());

        // --- 2. FILTROS DE SEGURIDAD (TABLA) ---
        Integer filtroAbogadoSeguro = abogadoId;
        Integer filtroGerenciaSeguro = gerenciaId;
        List<Integer> filtroMateriaIds = null;

        switch (usuarioActual.getRol()) {
            case ABOGADO:
                filtroAbogadoSeguro = usuarioActual.getId();
                break;
            case JEFE_DEPTO:
                if (usuarioActual.getGerencia() != null) {
                    filtroGerenciaSeguro = usuarioActual.getGerencia().getId();
                }
                if (usuarioActual.getMaterias() != null && !usuarioActual.getMaterias().isEmpty()) {
                    filtroMateriaIds = usuarioActual.getMaterias().stream()
                            .map(m -> m.getId())
                            .collect(Collectors.toList());
                }
                break;
            case GERENTE:
            case DIRECCION:
            case SUBDIRECCION:
                if (usuarioActual.getGerencia() != null) {
                    filtroGerenciaSeguro = usuarioActual.getGerencia().getId();
                }
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
        } else if (usuarioActual.getRol() == RolUsuario.GERENTE || usuarioActual.getRol() == RolUsuario.DIRECCION || usuarioActual.getRol() == RolUsuario.SUBDIRECCION) {
            abogadosParaModal = usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO).stream()
                    .filter(u -> u.getGerencia() != null && usuarioActual.getGerencia() != null
                    && u.getGerencia().getId().equals(usuarioActual.getGerencia().getId()))
                    .collect(Collectors.toList());
        } else if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            abogadosParaModal = usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO).stream()
                    .filter(u -> u.getGerencia() != null && usuarioActual.getGerencia() != null
                    && u.getGerencia().getId().equals(usuarioActual.getGerencia().getId()))
                    .filter(u -> u.getMaterias().stream().anyMatch(m -> usuarioActual.getMaterias().contains(m)))
                    .collect(Collectors.toList());
        } else {
            abogadosParaModal = usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO);
        }
        model.addAttribute("abogados", abogadosParaModal);

        // --- 4. LISTAS PARA FILTROS GENERALES ---
        model.addAttribute("listaAbogados", usuarioRepository.findAll());
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findAll());
        model.addAttribute("listaPrioridades", catPrioridadRepository.findAll());
        model.addAttribute("listaEstatus", EstatusTermino.values());

        // --- 5. LOGICA ANTI-FANTASMAS (EXPEDIENTES DISPONIBLES) - OPTIMIZADA ---
        List<Expediente> expedientesParaDropdown;

        if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
            List<Expediente> misExpedientes = expedienteRepository.findAllByAbogadoResponsableId(usuarioActual.getId());
            List<Expediente> expedientesPorAudiencia = audienciaRepository.findExpedientesByAbogadoInvitadoActivo(usuarioActual.getId());
            Set<Expediente> conjunto = new HashSet<>(misExpedientes);
            conjunto.addAll(expedientesPorAudiencia);
            expedientesParaDropdown = new ArrayList<>(conjunto);
        } else if (usuarioActual.getRol() == RolUsuario.GERENTE) {
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
        if (rol == null) {
            return new ArrayList<>();
        }
        List<EstatusTermino> etapas = new ArrayList<>();
        switch (rol) {
            case ABOGADO:
                etapas.addAll(Arrays.asList(EstatusTermino.PROYECTISTA, EstatusTermino.LIBERADO));
                break;
            case JEFE_DEPTO:
                etapas.addAll(Arrays.asList(EstatusTermino.REVISION, EstatusTermino.PRESENTADO));
                break;
            case GERENTE:
                etapas.addAll(Arrays.asList(EstatusTermino.GERENCIA, EstatusTermino.PRESENTADO));
                break;
            case DIRECCION:
                etapas.addAll(Arrays.asList(EstatusTermino.DIRECCION, EstatusTermino.PRESENTADO));
                break;
            case SUBDIRECCION:
                etapas.addAll(Arrays.asList(EstatusTermino.DIRECCION, EstatusTermino.PRESENTADO, EstatusTermino.CONCLUIDO));
                break;
            case ADMIN:
                etapas.addAll(Arrays.asList(EstatusTermino.values()));
                break;
        }
        if (!etapas.contains(EstatusTermino.CONCLUIDO)) {
            etapas.add(EstatusTermino.CONCLUIDO);
        }
        return etapas;
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Termino terminoForm,
            @RequestParam(value = "expedienteId", required = false) UUID expedienteId,
            @RequestParam(value = "abogadoId", required = false) Integer abogadoId,
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

        if (expedienteId != null) {
            Expediente exp = expedienteRepository.findById(expedienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("El expediente asociado no existe."));
            validarAccesoExpediente(exp, actor);
            terminoForm.setExpediente(exp);
        }

        if (abogadoId != null) {
            Usuario abogado = usuarioRepository.findById(abogadoId)
                    .orElseThrow(() -> new ResourceNotFoundException("El abogado asignado no existe."));
            terminoForm.setAbogadoResponsable(abogado);
        }

        if (terminoForm.getId() != null) {
            Termino existente = terminoRepository.findById(terminoForm.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("El término que intentas editar no existe."));

            validarAccesoTermino(existente, actor);

            existente.setAsunto(terminoForm.getAsunto());
            existente.setFechaVencimiento(terminoForm.getFechaVencimiento());

            if (terminoForm.getAbogadoResponsable() != null) {
                existente.setAbogadoResponsable(terminoForm.getAbogadoResponsable());
            }
            if (terminoForm.getExpediente() != null) {
                existente.setExpediente(terminoForm.getExpediente());
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
            if (expedienteId == null && terminoForm.getExpediente() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El expediente asociado es obligatorio para crear un término.");
            }
            esNuevo = true;
            terminoGuardar = terminoForm;
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

        /* PUNTO DE REFERENCIA - FUNCION ORIGINAL 
        if (esNuevo && terminoGuardado.getAbogadoResponsable() != null) {
            String expedienteNum = (terminoGuardado.getExpediente() != null)
                    ? terminoGuardado.getExpediente().getNumero()
                    : "S/N";
            notificacionService.crearNotificacion(
                    terminoGuardado.getAbogadoResponsable(),
                    "Nuevo Término: " + expedienteNum,
                    "El usuario " + actor.getNombreCompleto() + " te ha asignado: " + terminoGuardado.getAsunto(),
                    "TERMINO",
                    terminoGuardado.getPrioridad(),
                        expedienteNum);



        }
         */
        //BLOQUE ARRIBA SIN MODIFICACIONES ////////////////////////////
        // LINEAS DEBAJO EN MODIFICACION ///////////////////////////////
        // punto de referencia 2
        if (usuarioActualN.getRol() == RolUsuario.ABOGADO) {
            if (esNuevo && terminoGuardado.getAbogadoResponsable() != null) {
                String expedienteNum = (terminoGuardado.getExpediente() != null)
                        ? terminoGuardado.getExpediente().getNumero()
                        : "S/N";
                notificacionService.crearNotificacion(
                        terminoGuardado.getAbogadoResponsable(),
                        actor,
                        "Nuevo Término en expediente: " + expedienteNum,
                        "Haz creado el termino con el asunto: " + terminoGuardado.getAsunto(),
                        "TERMINO",
                          terminoGuardado.getPrioridad(),
                          expedienteNum,
                          false);

            }
        } else {
            if (esNuevo && terminoGuardado.getAbogadoResponsable() != null) {
                String expedienteNum = (terminoGuardado.getExpediente() != null)
                        ? terminoGuardado.getExpediente().getNumero()
                        : "S/N";
                notificacionService.crearNotificacion(
                        terminoGuardado.getAbogadoResponsable(),
                        actor,
                        "Nuevo Término: " + expedienteNum,
                        "El usuario " + actor.getNombreCompleto() + " te ha asignado: " + terminoGuardado.getAsunto(),
                        "TERMINO",
                          terminoGuardado.getPrioridad(),
                          expedienteNum,
                          false);

            }
        }

        /////LINEAS ARRIBA EN MODIFICACION //////////////////////////////////////


        
        //////BLOQUE DEBAJO SIN CAMBIOS ///////////////////////////////////////////////////////

        redirectAttrs.addFlashAttribute("mensaje", "Término guardado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/terminos";
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

        if (termino.getExpediente() != null) {
            String desc = "Cambio de estatus de Término a " + siguienteEtapa.getDescripcion()
                    + "). Acción realizada por: " + actor.getNombreCompleto() + " el "
                    + LocalDate.now() + ".";
            auditoriaService.registrar(termino.getExpediente(), "Cambio de Estado (Término)", desc, "EDIT", actor);
        }

        notificarCambioDeEtapa(termino, etapaAnterior, siguienteEtapa, actor);
        redirectAttrs.addFlashAttribute("mensaje", "Avanzó a: " + siguienteEtapa.getDescripcion());
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/terminos";
    }

    private void notificarCambioDeEtapa(Termino termino, EstatusTermino etapaAnterior, EstatusTermino nuevaEtapa, Usuario actor) {
        if (termino.getExpediente() == null) {
            return;
        }

        Gerencia g = termino.getExpediente().getGerencia();
        if (g == null) {
            return;
        }

        String titulo = "Término en " + nuevaEtapa.getDescripcion();
        String mensajeBase = String.format("Movimiento de '%s' a '%s' por %s", etapaAnterior.getDescripcion(), nuevaEtapa.getDescripcion(), actor.getNombreCompleto());

        List<Usuario> destinatarios = new ArrayList<>();
        switch (nuevaEtapa) {
            case REVISION:
                destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.JEFE_DEPTO, g));
                break;
            case GERENCIA:
                destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.GERENTE, g));
                break;
            case DIRECCION:
                // Dirección/Subdirección deben recibir solo de la misma gerencia del término
                destinatarios.addAll(usuarioRepository.findByGerenciaIdAndRolInAndActivoTrue(g.getId(),
                        Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION)));
                break;

            case LIBERADO:
                if (termino.getAbogadoResponsable() != null) {
                    destinatarios.add(termino.getAbogadoResponsable());
                }
                destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.JEFE_DEPTO, g));
                break;
            case PRESENTADO:
                // Dirección debe recibir solo de la misma gerencia del término
                destinatarios.addAll(usuarioRepository.findByGerenciaIdAndRolInAndActivoTrue(g.getId(),
                        Arrays.asList(RolUsuario.DIRECCION)));
                break;

        }

        Set<Usuario> unicos = new HashSet<>(destinatarios);
        String expedienteNum = termino.getExpediente().getNumero();
        String tituloExp = titulo + ": " + expedienteNum;

        for (Usuario u : unicos) {
            if (!u.getId().equals(actor.getId())) {
                notificacionService.crearNotificacion(u, actor, tituloExp, mensajeBase, "TERMINO", termino.getPrioridad(), termino.getId().toString(), true);
            }
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

            String carpetaDestino = "uploads/terminos/";
            Path rutaCarpeta = Paths.get(carpetaDestino);
            if (!Files.exists(rutaCarpeta)) {
                Files.createDirectories(rutaCarpeta);
            }

            String nombreFinal = id + "_" + archivo.getOriginalFilename();
            Files.copy(archivo.getInputStream(), rutaCarpeta.resolve(nombreFinal), StandardCopyOption.REPLACE_EXISTING);

            termino.setAcuse(nombreFinal);
            termino.setUpdatedAt(LocalDateTime.now());
            terminoRepository.save(termino);

            auditoriaService.registrar(termino.getExpediente(), "ARCHIVO CARGADO", "Borrador: " + archivo.getOriginalFilename(), "UPLOAD", actor);
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
                String carpeta = "uploads/acuses/";
                Path ruta = Paths.get(carpeta);
                if (!Files.exists(ruta)) {
                    Files.createDirectories(ruta);
                }
                String nombreAcuse = "ACUSE_" + id + "_" + archivo.getOriginalFilename();
                Files.copy(archivo.getInputStream(), ruta.resolve(nombreAcuse), StandardCopyOption.REPLACE_EXISTING);

                TerminoPresentado presentado = TerminoPresentado.builder()
                        .termino(termino)
                        .expedienteNumero(termino.getExpediente().getNumero())
                        .fechaPresentacion(LocalDate.now())
                        .acuseDocumento(nombreAcuse)
                        .sincronizadoAt(LocalDateTime.now()).build();
                terminoPresentadoRepository.save(presentado);

                termino.setAcuseNombre(nombreAcuse);
                if (observaciones != null) {
                    termino.setObservaciones(observaciones);
                }
                termino.setUpdatedAt(LocalDateTime.now());
                terminoRepository.save(termino);

                String logDesc = "Se ha cargado el acuse: " + archivo.getOriginalFilename();
                if (observaciones != null && !observaciones.trim().isEmpty()) {
                    logDesc += " - Observaciones: " + observaciones;
                }
                auditoriaService.registrar(termino.getExpediente(), "ARCHIVO CARGADO", logDesc, "UPLOAD", actor);

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

                String logDesc = "El término ha sido marcado como CONCLUIDO y archivado.";
                if (observaciones != null && !observaciones.trim().isEmpty()) {
                    logDesc += " - Observaciones: " + observaciones;
                }
                auditoriaService.registrar(termino.getExpediente(), "TÉRMINO CONCLUIDO", logDesc, "CHECK", actor);

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
        if (etapaActual == null) {
            return false;
        }
        List<RolUsuario> permitidos = PERMISOS_ETAPAS.get(etapaActual);
        return permitidos != null && permitidos.contains(rolUsuario);
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
            if (termino == null || termino.getAcuse() == null) {
                return ResponseEntity.notFound().build();
            }

            validarAccesoTermino(termino, getUsuarioActual());

            Path rutaArchivo = Paths.get("uploads/terminos").resolve(termino.getAcuse());
            Resource recurso = new UrlResource(rutaArchivo.toUri());
            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"").body(recurso);
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

        if (usuarioActual.getRol() == RolUsuario.ADMIN || usuarioActual.getRol() == RolUsuario.DIRECCION || usuarioActual.getRol() == RolUsuario.SUBDIRECCION) {
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

            List<TerminoPresentado> presentados = terminoPresentadoRepository.findByTerminoExpedienteId(termino.getExpediente().getId());
            TerminoPresentado acuse = presentados.stream().filter(p -> p.getTermino().getId().equals(id)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Acuse no encontrado"));

            if (acuse.getAcuseDocumento() == null) {
                return ResponseEntity.notFound().build();
            }

            Path rutaArchivo = Paths.get("uploads/acuses").resolve(acuse.getAcuseDocumento());
            Resource recurso = new UrlResource(rutaArchivo.toUri());
            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"").body(recurso);
            }
            return ResponseEntity.notFound().build();
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
        if (usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECCION || usuario.getRol() == RolUsuario.SUBDIRECCION) {
            return;
        }
        Expediente e = termino.getExpediente();
        if (e == null) {
            return;
        }
        validarAccesoExpediente(e, usuario);
        if (usuario.getRol() == RolUsuario.ABOGADO) {
            boolean esResponsableTermino = termino.getAbogadoResponsable() != null && termino.getAbogadoResponsable().getId().equals(usuario.getId());
            boolean esResponsableExp = e.getAbogadoResponsable() != null && e.getAbogadoResponsable().getId().equals(usuario.getId());
            boolean esInvitado = audienciaRepository.findExpedientesByAbogadoInvitadoActivo(usuario.getId()).stream().anyMatch(exp -> exp.getId().equals(e.getId()));
            if (!esResponsableTermino && !esResponsableExp && !esInvitado) {
                throw new ForbiddenException("No tienes acceso a este término.");
            }
        }
    }

    private void validarAccesoExpediente(Expediente e, Usuario usuario) {
        if (usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECCION || usuario.getRol() == RolUsuario.SUBDIRECCION) {
            return;
        }
        if (usuario.getGerencia() != null && (e.getGerencia() == null || !e.getGerencia().getId().equals(usuario.getGerencia().getId()))) {
            if (usuario.getRol() != RolUsuario.ABOGADO) {
                throw new ForbiddenException("No tienes acceso a esta gerencia.");
            }
        }
        if (usuario.getRol() == RolUsuario.JEFE_DEPTO) {
            if (e.getMateria() == null || usuario.getMaterias().stream().noneMatch(m -> m.getId().equals(e.getMateria().getId()))) {
                throw new ForbiddenException("No tienes acceso a esta materia.");
            }
        } else if (usuario.getRol() == RolUsuario.ABOGADO) {
            boolean esResponsable = e.getAbogadoResponsable() != null && e.getAbogadoResponsable().getId().equals(usuario.getId());
            boolean esInvitado = audienciaRepository.findExpedientesByAbogadoInvitadoActivo(usuario.getId()).stream().anyMatch(exp -> exp.getId().equals(e.getId()));
            if (!esResponsable && !esInvitado) {
                throw new ForbiddenException("No tienes acceso a este expediente.");
            }
        }
    }
}
// TerminosController - Version VsCode dentro de antigravity - lineas 960