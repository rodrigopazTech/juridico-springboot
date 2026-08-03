package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ForbiddenException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.service.procesal.AudienciaService;
import com.juridico.sistema_juridico.Entity.expediente.ColaboradorExpediente;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import com.juridico.sistema_juridico.repository.Catalogo.EstadoRepository;
import com.juridico.sistema_juridico.Entity.procesal.AudienciaContexto;
import com.juridico.sistema_juridico.repository.procesal.AudienciaContextoRepository;
import com.juridico.sistema_juridico.Entity.enums.OrigenAudiencia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.util.AudienciaExcelExporter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.UUID;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

@Controller
@RequestMapping("/audiencias")
public class AudienciasController {

    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private AudienciaContextoRepository audienciaContextoRepository;
    @Autowired
    private ExpedienteRepository expedienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AudienciaService audienciaService;
    @Autowired
    private com.juridico.sistema_juridico.service.OutlookCalendarService outlookCalendarService;
    @Autowired
    private com.juridico.sistema_juridico.service.NotificacionService notificacionService;
    @Autowired
    private com.juridico.sistema_juridico.repository.Expediente.ColaboradorExpedienteRepository colaboradorExpedienteRepository;

    // @Autowired private ColaboradorExpedienteRepository colaboradorRepository;
    @Autowired
    private GerenciaRepository gerenciaRepository;
    @Autowired
    private MateriaRepository materiaRepository;
    @Autowired
    private TipoAudienciaRepository tipoAudienciaRepository;
    @Autowired
    private OrganoJurisdiccionalRepository organoJurisdiccionalRepository;
    @Autowired
    private EstadoRepository estadoRepository;

    private void validarAccesoExpediente(Expediente e, Usuario usuario) {
        RolUsuario rol = usuario.getRol();
        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL 
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR) {
            return;
        }
        // El responsable directo del expediente siempre tiene acceso prioritario
        if (e.getAbogadoResponsable() != null && e.getAbogadoResponsable().getId().equals(usuario.getId())) {
            return;
        }
        if (rol == RolUsuario.GERENTE) {
            if (usuario.getGerencia() != null && e.getGerencia() != null 
                    && e.getGerencia().getId().equals(usuario.getGerencia().getId())) {
                return;
            }
        } else if (rol == RolUsuario.JEFE_DEPTO) {
            if (usuario.getGerencia() != null && e.getGerencia() != null 
                    && e.getGerencia().getId().equals(usuario.getGerencia().getId())
                    && e.getMateria() != null 
                    && usuario.getMaterias().stream().anyMatch(m -> m.getId().equals(e.getMateria().getId()))) {
                return;
            }
        } else if (rol == RolUsuario.ABOGADO) {
            if (colaboradorExpedienteRepository.existsByExpedienteIdAndUsuarioIdAndFechaExpiracionAfter(
                    e.getId(), usuario.getId(), java.time.LocalDateTime.now())) {
                return;
            }
        }
        throw new ForbiddenException("No tienes acceso a este expediente.");
    }

    private void validarPermisoEliminarAudiencia(Audiencia audiencia, Usuario usuario) {
        RolUsuario rol = usuario.getRol();
        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR) {
            return;
        }

        Expediente expediente = audiencia.getExpediente();
        if (rol == RolUsuario.ABOGADO) {
            if (expediente != null && expediente.getAbogadoResponsable() != null
                    && expediente.getAbogadoResponsable().getId().equals(usuario.getId())) {
                return;
            }
            throw new ForbiddenException("Solo el abogado responsable del expediente puede eliminar esta audiencia.");
        }

        if (rol == RolUsuario.GERENTE || rol == RolUsuario.JEFE_DEPTO) {
            if (expediente != null) {
                validarAccesoExpediente(expediente, usuario);
                return;
            }
            Gerencia gerencia = audiencia.getGerenciaReferencia();
            Materia materia = audiencia.getMateriaReferencia();
            if (rol == RolUsuario.GERENTE && usuario.getGerencia() != null && gerencia != null
                    && usuario.getGerencia().getId().equals(gerencia.getId())) {
                return;
            }
            if (rol == RolUsuario.JEFE_DEPTO && usuario.getGerencia() != null && gerencia != null
                    && usuario.getGerencia().getId().equals(gerencia.getId())
                    && materia != null && usuario.getMaterias().stream().anyMatch(m -> m.getId().equals(materia.getId()))) {
                return;
            }
        }

        throw new ForbiddenException("No tienes permisos para eliminar esta audiencia.");
    }

    @GetMapping({"", "/"})
    public String index(Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "gerencia", required = false) String gerencia,
            @RequestParam(value = "materia", required = false) String materia,
            @RequestParam(value = "estatus", required = false) EstatusAudiencia estatus,
            @RequestParam(value = "abogadoId", required = false) Integer abogadoId,
            @RequestParam(value = "periodo", required = false) PeriodoFiltro periodo,
            @RequestParam(value = "fecha", required = false) LocalDate fecha,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "anio", required = false) Integer anio,
            @RequestParam(value = "sortBy", defaultValue = "fechaAudiencia") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {

        if (keyword != null && keyword.contains("?keyword=")) {
            // Nos quedamos solo con lo que esté después del último igual (=)
            keyword = keyword.substring(keyword.lastIndexOf("=") + 1);
        }

        // 1. OBTENER USUARIO ACTUAL
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        String rol = usuario.getRol().name();

        // 2. CONFIGURAR FILTROS DE SEGURIDAD
        Integer filtroUsuarioId = null;
        Integer filtroGerenciaId = null;
        List<Integer> filtroMateriaIds = null;

        switch (usuario.getRol()) {
            case ABOGADO:
                filtroUsuarioId = usuario.getId();
                filtroGerenciaId = null;
                break;
            case GERENTE:
                if (usuario.getGerencia() != null) {
                    filtroGerenciaId = usuario.getGerencia().getId();
                } else {
                    filtroGerenciaId = -1;
                }
                break;
            case JEFE_DEPTO:
                if (usuario.getGerencia() != null) {
                    filtroGerenciaId = usuario.getGerencia().getId();
                } else {
                    filtroGerenciaId = -1;
                }
                if (usuario.getMaterias() != null && !usuario.getMaterias().isEmpty()) {
                    filtroMateriaIds = usuario.getMaterias().stream()
                            .map(m -> m.getId())
                            .collect(Collectors.toList());
                } else {
                    filtroMateriaIds = Collections.emptyList();
                }
                break;
            case ADMIN:
            case DIRECTOR_PRINCIPAL:
            case DIRECTOR:
            case SUBDIRECTOR:
                // Ven todo
                break;
        }

        // 3. LÓGICA DE FECHAS (Prioridad: Fecha Calendario > Búsqueda Texto > Periodo > Mes > Default)
        LocalDate fechaInicio;
        LocalDate fechaFin;

        String searchKeyword = (keyword != null && !keyword.isEmpty()) ? keyword.trim() : null;

        if (fecha != null) {
            fechaInicio = fecha;
            fechaFin = fecha;
        } else if (searchKeyword != null && searchKeyword.matches("\\d{4}-\\d{2}-\\d{2}")) {
            // 1. Fecha específica desde clic en calendario
            LocalDate fechaDesdeKeyword = LocalDate.parse(searchKeyword);
            fechaInicio = fechaDesdeKeyword;
            fechaFin = fechaDesdeKeyword;
            searchKeyword = null; 
        } else if (searchKeyword != null && !searchKeyword.isEmpty()) {
            // 2. Búsqueda Global por texto (Ignora el mes seleccionado para encontrar todo)
            fechaInicio = LocalDate.of(1900, 1, 1);
            fechaFin = LocalDate.of(2100, 12, 31);
        } else if (periodo != null) {
            // 3. Periodos relativos (Hoy, Semana, etc.)
            LocalDate[] rango = periodo.calcularRango();
            fechaInicio = rango[0];
            fechaFin = rango[1];
        } else if (mes != null && anio != null) {
            // 4. Navegación por mes (Flechas del calendario)
            fechaInicio = LocalDate.of(anio, mes, 1);
            fechaFin = fechaInicio.withDayOfMonth(fechaInicio.lengthOfMonth());
        } else {
            // 5. Default: Mostrar TODO (Como en Términos)
            fechaInicio = LocalDate.of(1900, 1, 1);
            fechaFin = LocalDate.of(2100, 12, 31);
            
            LocalDate hoy = LocalDate.now();
            if (mes == null) mes = hoy.getMonthValue();
            if (anio == null) anio = hoy.getYear();
        }

        // 4. CONSULTA CON PAGINACIÓN (Standard: 10 por página)
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, 10, sort);

        // --- OPTIMIZACIÓN ROD-185: Si hay búsqueda, ignoramos fechas para que las KPIs y la tabla coincidan ---
        if (fecha == null && searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            fechaInicio = LocalDate.of(1900, 1, 1);
            fechaFin = LocalDate.of(2100, 12, 31);
        }

        Page<Audiencia> audiencias = audienciaRepository.buscarConFiltros(
                id, searchKeyword, tipo, gerencia, materia, estatus,
                abogadoId, fechaInicio, fechaFin,
                filtroUsuarioId, filtroGerenciaId, filtroMateriaIds,
                pageable);

        model.addAttribute("audiencias", audiencias);
        // ... (resto de atributos se mantienen igual)
        model.addAttribute("pageTitle", "Gestión de Audiencias");
        model.addAttribute("activePage", "audiencias");
        model.addAttribute("paramPeriodo", periodo != null ? periodo.name() : null);
        model.addAttribute("mesActual", mes != null ? mes : LocalDate.now().getMonthValue());
        model.addAttribute("anioActual", anio != null ? anio : LocalDate.now().getYear());

        // ... (Sección de dropdown inteligentes ...)
        List<Expediente> expedientesParaDropdown;
        // ... (se omite para brevedad pero se conserva el original)
        if (rol.equals("ABOGADO")) {
            List<Expediente> misExpedientes = expedienteRepository.findAll().stream()
                    .filter(e -> e.getAbogadoResponsable() != null
                            && e.getAbogadoResponsable().getId().equals(usuario.getId()))
                    .collect(Collectors.toList());
            List<Expediente> expedientesPorAudiencia = audienciaRepository.findAll().stream()
                    .filter(a -> a.getAbogadoComparece() != null &&
                            a.getAbogadoComparece().getId().equals(usuario.getId()) &&
                            EstatusAudiencia.CONCLUIDA != a.getEstatusAudiencia() &&
                            a.getExpediente() != null)
                    .map(Audiencia::getExpediente)
                    .collect(Collectors.toList());
            Set<Expediente> conjunto = new HashSet<>(misExpedientes);
            conjunto.addAll(expedientesPorAudiencia);
            expedientesParaDropdown = new ArrayList<>(conjunto);
        } else if (rol.equals("GERENTE")) {
            expedientesParaDropdown = expedienteRepository.findAll().stream()
                    .filter(e -> e.getGerencia() != null && usuario.getGerencia() != null &&
                            e.getGerencia().getId().equals(usuario.getGerencia().getId()))
                    .collect(Collectors.toList());
        } else if (rol.equals("JEFE_DEPTO")) {
            expedientesParaDropdown = expedienteRepository.findAll().stream()
                    .filter(e -> e.getMateria() != null && usuario.getMaterias().stream()
                            .anyMatch(m -> m.getId().equals(e.getMateria().getId())))
                    .collect(Collectors.toList());
        } else {
            expedientesParaDropdown = expedienteRepository.findAll();
        }
        model.addAttribute("expedientesList", expedientesParaDropdown);

        model.addAttribute("listaTiposAudiencia", tipoAudienciaRepository.findByActivoTrue());
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        // Materias filtradas por gerencia para roles limitados
        List<com.juridico.sistema_juridico.Entity.catalogo.Materia> listaMaterias;
        if (usuario.getRol() == RolUsuario.ABOGADO || usuario.getRol() == RolUsuario.JEFE_DEPTO
                || usuario.getRol() == RolUsuario.GERENTE) {
            Gerencia gerenciaUsuario = usuario.getGerencia();
            if (gerenciaUsuario != null) {
                listaMaterias = materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(gerenciaUsuario.getId());
            } else {
                listaMaterias = materiaRepository.findByActivoTrueOrderByNombreAsc();
            }
        } else {
            listaMaterias = materiaRepository.findByActivoTrueOrderByNombreAsc();
        }
        model.addAttribute("listaMaterias", listaMaterias);
        model.addAttribute("tipos", tipoAudienciaRepository.findByActivoTrue());
        model.addAttribute("listaOrganos", organoJurisdiccionalRepository.findByActivoTrue());

        // Sedes filtradas por gerencia del usuario (o todas si es nivel alto)
        List<com.juridico.sistema_juridico.Entity.catalogo.Estado> listaSedes;
        if (usuario.getRol() == RolUsuario.ABOGADO || usuario.getRol() == RolUsuario.JEFE_DEPTO
                || usuario.getRol() == RolUsuario.GERENTE) {
            Gerencia gerenciaUsuario = usuario.getGerencia();
            if (gerenciaUsuario != null) {
                listaSedes = estadoRepository.findByGerenciaIdAndActivoTrue(gerenciaUsuario.getId());
                List<com.juridico.sistema_juridico.Entity.catalogo.Estado> sedesGlobales = estadoRepository.findByGerenciaIsNullAndActivoTrue();
                listaSedes = new ArrayList<>(listaSedes);
                listaSedes.addAll(sedesGlobales);
            } else {
                listaSedes = estadoRepository.findByActivoTrue();
            }
        } else {
            listaSedes = estadoRepository.findByActivoTrue();
        }
        model.addAttribute("listaSedes", listaSedes);

        List<Usuario> listaAbogados;
        if (usuario.getRol() == RolUsuario.ABOGADO || usuario.getRol() == RolUsuario.JEFE_DEPTO || usuario.getRol() == RolUsuario.GERENTE) {
            if (usuario.getGerencia() != null) {
                listaAbogados = usuarioRepository.findAll().stream()
                        .filter(u -> u.getGerencia() != null &&
                                u.getGerencia().getId().equals(usuario.getGerencia().getId()))
                        .collect(Collectors.toList());
            } else {
                listaAbogados = usuarioRepository.findAll();
            }
        } else {
            // ADMIN, DIRECTOR_PRINCIPAL, DIRECTOR, SUBDIRECTOR
            listaAbogados = usuarioRepository.findAll();
        }
        model.addAttribute("abogados", listaAbogados);

        model.addAttribute("keyword", keyword);
        model.addAttribute("paramTipo", tipo);
        model.addAttribute("paramGerencia", gerencia);
        model.addAttribute("paramMateria", materia);
        model.addAttribute("paramEstatus", estatus != null ? estatus.name() : null);
        model.addAttribute("paramAbogadoId", filtroUsuarioId != null ? filtroUsuarioId : abogadoId);
        model.addAttribute("paramPeriodo", periodo != null ? periodo.name() : null);
        model.addAttribute("paramFecha", fecha != null ? fecha.toString() : null);
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("listaEstatus", EstatusAudiencia.values());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("outlookCalendarLinked", outlookCalendarService.tieneCalendarioVinculado(usuario));

        // --- 6. CONTEOS PARA KPIs ---
        List<Object[]> rawCounts = audienciaRepository.contarPorEstatusConFiltros(
                searchKeyword, tipo, gerencia, materia, abogadoId, fechaInicio, fechaFin,
                filtroUsuarioId, filtroGerenciaId, filtroMateriaIds);

        Map<EstatusAudiencia, Long> kpisAudit = new EnumMap<>(EstatusAudiencia.class);
        for (EstatusAudiencia e : EstatusAudiencia.values()) kpisAudit.put(e, 0L);
        long totalKpi = 0;
        for (Object[] row : rawCounts) {
            if (row[0] != null) {
                EstatusAudiencia e = (EstatusAudiencia) row[0];
                long count = (long) row[1];
                kpisAudit.put(e, count);
                totalKpi += count;
            }
        }
        model.addAttribute("kpisAudiencias", kpisAudit);
        model.addAttribute("totalAudiencias", totalKpi);

        // --- OPTIMIZACIÓN AJAX: Búsqueda Global en tiempo real ---
        HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();
        String isAjax = request.getHeader("X-Requested-With");

        if ("XMLHttpRequest".equals(isAjax)) {
            return "views/audiencias/index :: gridMaestro";
        }

        return "views/audiencias/index";
    }
@PostMapping("/guardar")
public String guardar(@ModelAttribute Audiencia audiencia,
        @RequestParam(value = "usuarioDelegadoId", required = false) Integer usuarioDelegadoId,
        @RequestParam(value = "syncOutlook", defaultValue = "false") boolean syncOutlook,
        RedirectAttributes redirectAttrs) {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

    boolean esNueva = (audiencia.getId() == null);

    if (audiencia.getFechaAudiencia() == null) {
        throw new BadRequestException("La fecha de la audiencia es obligatoria.");
    }
    if (audiencia.getTipoAudiencia() == null || audiencia.getTipoAudiencia().getId() == null) {
        throw new BadRequestException("El tipo de audiencia es obligatorio.");
    }

    Expediente exp = null;

    if (!esNueva) {
        Audiencia audienciaExistente = audienciaRepository.findById(audiencia.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se puede editar: La audiencia con ID " + audiencia.getId() + " no existe."));

        // Validar acceso al expediente original
        if (audienciaExistente.getExpediente() != null) {
            validarAccesoExpediente(audienciaExistente.getExpediente(), usuario);
        }

        // Preservamos los datos que no vienen en el formulario simple
        audiencia.setEstatusAudiencia(audienciaExistente.getEstatusAudiencia());
        audiencia.setActaDocumento(audienciaExistente.getActaDocumento());
        copiarCamposOutlook(audienciaExistente, audiencia);

        // Preservar observaciones si no se enviaron nuevas
        if (audiencia.getObservaciones() == null && audienciaExistente.getObservaciones() != null) {
            audiencia.setObservaciones(audienciaExistente.getObservaciones());
        }

        if (audiencia.getExpediente() != null && audiencia.getExpediente().getId() != null) {
            exp = expedienteRepository.findById(audiencia.getExpediente().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("El expediente asociado no existe."));
        } else {
            exp = audienciaExistente.getExpediente();
            audiencia.setExpediente(exp);
        }
    } else {
        if (audiencia.getExpediente() == null || audiencia.getExpediente().getId() == null) {
            throw new BadRequestException("El expediente es obligatorio.");
        }
        exp = expedienteRepository.findById(audiencia.getExpediente().getId())
                .orElseThrow(() -> new ResourceNotFoundException("El expediente asociado no existe."));
        audiencia.setEstatusAudiencia(EstatusAudiencia.PENDIENTE);
    }

    if (exp == null) {
        throw new BadRequestException("El expediente es obligatorio.");
    }
    validarAccesoExpediente(exp, usuario);

    // Corrección para evitar conflictos con Hibernate si el ID del abogado viene vacío
    if (audiencia.getAbogadoComparece() != null && audiencia.getAbogadoComparece().getId() == null) {
        audiencia.setAbogadoComparece(null);
    }

    // Limpiar delegado si viene vacío
    if (audiencia.getUsuarioDelegado() != null && audiencia.getUsuarioDelegado().getId() == null) {
        audiencia.setUsuarioDelegado(null);
    }

    // Guardar sede si no existe en el catálogo
    if (audiencia.getSalaLugar() != null && !audiencia.getSalaLugar().trim().isEmpty()) {
        String sedeNombre = audiencia.getSalaLugar().trim();
        java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Estado> sedeOpt = estadoRepository.findByNombreIgnoreCase(sedeNombre);
        if (!sedeOpt.isPresent()) {
            com.juridico.sistema_juridico.Entity.catalogo.Estado nuevaSede = new com.juridico.sistema_juridico.Entity.catalogo.Estado();
            nuevaSede.setNombre(sedeNombre);
            nuevaSede.setGerencia(usuario.getGerencia());
            nuevaSede.setUsuario(usuario);
            nuevaSede.setActivo(true);
            estadoRepository.save(nuevaSede);
        }
    }

    // GUARDADO
    Audiencia guardada = audienciaRepository.save(audiencia);
    sincronizarOutlookSiConfirmado(syncOutlook, usuario, guardada, redirectAttrs);

    // DELEGACIÓN: crear/actualizar acceso temporal en colaboradores_expediente
    if (guardada.getUsuarioDelegado() != null) {
        Usuario delegado = guardada.getUsuarioDelegado();
        // Expiración = fecha de audiencia + 2 días, al final del día
        java.time.LocalDateTime expiracion = guardada.getFechaAudiencia()
                .plusDays(2)
                .atTime(23, 59, 59);
        String motivo = "Delegación de audiencia #" + guardada.getId();

        // Buscar registro previo para este usuario+expediente y actualizar, o crear uno nuevo
        List<ColaboradorExpediente> previos = colaboradorExpedienteRepository
                .findByExpedienteIdAndUsuarioId(guardada.getExpediente().getId(), delegado.getId());

        if (!previos.isEmpty()) {
            ColaboradorExpediente existente = previos.get(0);
            existente.setFechaExpiracion(expiracion);
            existente.setMotivo(motivo);
            colaboradorExpedienteRepository.save(existente);
        } else {
            ColaboradorExpediente colaborador = ColaboradorExpediente.builder()
                    .expediente(guardada.getExpediente())
                    .usuario(delegado)
                    .fechaExpiracion(expiracion)
                    .permisoNivel("LECTURA_TOTAL")
                    .motivo(motivo)
                    .build();
            colaboradorExpedienteRepository.save(colaborador);
        }
    }

    // Notificación (Solo si es nueva)
    if (esNueva) {
        audienciaService.notificarCreacionAudiencia(guardada);
    } else {
        audienciaService.registrarEdicionAudiencia(guardada);
    }

    // Notificación al delegado (si se asignó uno)
    if (guardada.getUsuarioDelegado() != null) {
        audienciaService.notificarDelegacion(guardada, usuario);
    }

    redirectAttrs.addFlashAttribute("mensaje", "Audiencia guardada correctamente.");
    redirectAttrs.addFlashAttribute("tipo", "success");

    return "redirect:/audiencias";
}

    @PostMapping("/guardar-asunto-unico")
    public String guardarAsuntoUnico(@ModelAttribute Audiencia audiencia,
            @RequestParam(value = "contextoMateriaId", required = false) Integer contextoMateriaId,
            @RequestParam(value = "contextoGerenciaId", required = false) Integer contextoGerenciaId,
            @RequestParam(value = "contextoDescripcion", required = false) String contextoDescripcion,
            @RequestParam(value = "contextoPartes", required = false) String contextoPartes,
            @RequestParam(value = "contextoNumeroReferencia", required = false) String contextoNumeroReferencia,
            @RequestParam(value = "syncOutlook", defaultValue = "false") boolean syncOutlook,
            RedirectAttributes redirectAttrs) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        boolean esNueva = (audiencia.getId() == null);

        if (audiencia.getFechaAudiencia() == null) {
            throw new BadRequestException("La fecha de la audiencia es obligatoria.");
        }
        if (audiencia.getTipoAudiencia() == null || audiencia.getTipoAudiencia().getId() == null) {
            throw new BadRequestException("El tipo de audiencia es obligatorio.");
        }

        AudienciaContexto contexto = null;

        if (!esNueva) {
            Audiencia audienciaExistente = audienciaRepository.findById(audiencia.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se puede editar: La audiencia con ID " + audiencia.getId() + " no existe."));

            // Preservamos los datos que no vienen en el formulario simple
            audiencia.setEstatusAudiencia(audienciaExistente.getEstatusAudiencia());
            audiencia.setActaDocumento(audienciaExistente.getActaDocumento());
            copiarCamposOutlook(audienciaExistente, audiencia);

            // Preservar observaciones si no se enviaron nuevas
            if (audiencia.getObservaciones() == null && audienciaExistente.getObservaciones() != null) {
                audiencia.setObservaciones(audienciaExistente.getObservaciones());
            }

            // Manejar contexto existente o nuevo
            if (audienciaExistente.getContexto() != null) {
                contexto = audienciaExistente.getContexto();
            } else {
                contexto = new AudienciaContexto();
            }
        } else {
            audiencia.setEstatusAudiencia(EstatusAudiencia.PENDIENTE);
            contexto = new AudienciaContexto();
        }

        // Llenar datos del contexto
        contexto.setOrigen(OrigenAudiencia.ASUNTO_UNICO);
        if (contextoGerenciaId != null) {
            contexto.setGerencia(gerenciaRepository.findById(contextoGerenciaId).orElse(null));
        }
        if (contextoMateriaId != null) {
            contexto.setMateria(materiaRepository.findById(contextoMateriaId).orElse(null));
        }
        contexto.setDescripcion(contextoDescripcion);
        contexto.setPartes(contextoPartes);
        contexto.setNumeroExpedienteSnapshot(contextoNumeroReferencia);

        // Guardar el contexto
        contexto = audienciaContextoRepository.save(contexto);

        // Buscar o crear Expediente Temporal
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
            } else if (usuario.getGerencia() != null) {
                expTemporal.setGerencia(usuario.getGerencia());
            }
            expTemporal.setDescripcion(contextoDescripcion != null ? contextoDescripcion : "Asunto Único");
            expTemporal.setPartes(contextoPartes);
            expTemporal.setAbogadoResponsable(audiencia.getAbogadoComparece() != null ? audiencia.getAbogadoComparece() : usuario);
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
            if (audiencia.getAbogadoComparece() != null) {
                expTemporal.setAbogadoResponsable(audiencia.getAbogadoComparece());
            }
            expTemporal = expedienteRepository.save(expTemporal);
        }

        // Asociar el contexto y el expediente temporal
        audiencia.setContexto(contexto);
        audiencia.setExpediente(expTemporal);

        // Corrección para evitar conflictos con Hibernate si el ID del abogado viene vacío
        if (audiencia.getAbogadoComparece() != null && audiencia.getAbogadoComparece().getId() == null) {
            audiencia.setAbogadoComparece(null);
        }

        // Limpiar delegado si viene vacío
        if (audiencia.getUsuarioDelegado() != null && audiencia.getUsuarioDelegado().getId() == null) {
            audiencia.setUsuarioDelegado(null);
        }

        // Guardar sede si no existe en el catálogo
        if (audiencia.getSalaLugar() != null && !audiencia.getSalaLugar().trim().isEmpty()) {
            String sedeNombre = audiencia.getSalaLugar().trim();
            java.util.Optional<com.juridico.sistema_juridico.Entity.catalogo.Estado> sedeOpt = estadoRepository.findByNombreIgnoreCase(sedeNombre);
            if (!sedeOpt.isPresent()) {
                com.juridico.sistema_juridico.Entity.catalogo.Estado nuevaSede = new com.juridico.sistema_juridico.Entity.catalogo.Estado();
                nuevaSede.setNombre(sedeNombre);
                nuevaSede.setGerencia(usuario.getGerencia());
                nuevaSede.setUsuario(usuario);
                nuevaSede.setActivo(true);
                estadoRepository.save(nuevaSede);
            }
        }

        // GUARDADO
        Audiencia guardada = audienciaRepository.save(audiencia);
        sincronizarOutlookSiConfirmado(syncOutlook, usuario, guardada, redirectAttrs);

        // Notificación (Solo si es nueva)
        if (esNueva) {
            audienciaService.notificarCreacionAudiencia(guardada);
        } else {
            audienciaService.registrarEdicionAudiencia(guardada);
        }

        // Notificación al delegado (si se asignó uno)
        if (guardada.getUsuarioDelegado() != null) {
            audienciaService.notificarDelegacion(guardada, usuario);
        }

        redirectAttrs.addFlashAttribute("mensaje", "Audiencia de asunto único guardada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");

        return "redirect:/audiencias";
    }

    @GetMapping("/obtener/{id}")
    @ResponseBody
    public ResponseEntity<Audiencia> obtenerPorId(@PathVariable("id") Integer id) {
        return audienciaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/subir-acta")
    public String subirActa(@RequestParam("id") Integer id,
            @RequestParam("archivo") MultipartFile archivo,
            RedirectAttributes redirectAttrs) {
        try {
            Audiencia aud = audienciaRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Audiencia no encontrada"));
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
            if (aud.getExpediente() != null) {
                validarAccesoExpediente(aud.getExpediente(), usuario);
            }

            audienciaService.subirActa(id, archivo);
            redirectAttrs.addFlashAttribute("mensaje", "Acta subida correctamente. Estatus actualizado.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al subir acta: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/audiencias";
    }

    @PostMapping("/concluir")
    public String concluir(@RequestParam("id") Integer id,
            @RequestParam("observaciones") String observaciones,
            RedirectAttributes redirectAttrs) {
        try {
            // 1. OBTENER USUARIO ACTUAL
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // 2. EJECUTAR LÓGICA (La seguridad ahora se valida en el Service)
            audienciaService.concluirAudiencia(id, observaciones, usuario);

            redirectAttrs.addFlashAttribute("mensaje", "Audiencia validada y concluida exitosamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/audiencias";
    }

    @PostMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> eliminar(@PathVariable("id") Integer id,
            @RequestParam(value = "deleteOutlook", defaultValue = "false") boolean deleteOutlook) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            // 1. VALIDACIÓN LIMPIA
            Audiencia aud = audienciaRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("La audiencia no existe."));

            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
            validarPermisoEliminarAudiencia(aud, usuario);
            String referencia = aud.getNumeroReferencia() != null ? aud.getNumeroReferencia()
                    : (aud.getExpediente() != null ? aud.getExpediente().getNumero() : "S/N");
            Materia materia = aud.getMateriaReferencia();

            if (deleteOutlook && Boolean.TRUE.equals(aud.getOutlookLinked())) {
                outlookCalendarService.eliminarEventoAudiencia(usuario, aud);
            }

            // 2. LÓGICA FELIZ (Sin try-catch interno)
            audienciaRepository.deleteById(id);
            notificacionService.notificarSuperioresDeEliminacion(
                    usuario,
                    com.juridico.sistema_juridico.Entity.enums.NotificacionEvento.AUDIENCIA_VALIDADA_CONCLUIDA,
                    "Audiencia eliminada",
                    "El usuario " + usuario.getNombreCompleto() + " elimino la audiencia " + referencia + ".",
                    null,
                    String.valueOf(id),
                    materia);

            response.put("success", true);
            response.put("message", "Audiencia eliminada correctamente.");
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (ForbiddenException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar la audiencia.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private void sincronizarOutlookSiConfirmado(boolean syncOutlook, Usuario usuario, Audiencia audiencia,
            RedirectAttributes redirectAttrs) {
        if (!syncOutlook) {
            return;
        }
        try {
            outlookCalendarService.crearOActualizarEventoAudiencia(usuario, audiencia);
            redirectAttrs.addFlashAttribute("mensajeOutlook", "Evento sincronizado con Outlook.");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensajeOutlook",
                    "La audiencia se guardo en SIAJ, pero Outlook no pudo sincronizarse: " + ex.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "warning");
        }
    }

    private void copiarCamposOutlook(Audiencia origen, Audiencia destino) {
        destino.setOutlookLinked(origen.getOutlookLinked());
        destino.setOutlookEventId(origen.getOutlookEventId());
        destino.setOutlookCalendarId(origen.getOutlookCalendarId());
        destino.setOutlookWebLink(origen.getOutlookWebLink());
        destino.setOutlookSyncedAt(origen.getOutlookSyncedAt());
        destino.setOutlookSyncStatus(origen.getOutlookSyncStatus());
        destino.setOutlookSyncError(origen.getOutlookSyncError());
    }

    @GetMapping("/descargar-acta/{id}")
    public ResponseEntity<Resource> descargarActa(@PathVariable("id") Integer id) {
        try {
            Audiencia audiencia = audienciaRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audiencia no encontrada"));

            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
            if (audiencia.getExpediente() != null) {
                validarAccesoExpediente(audiencia.getExpediente(), usuario);
            }

            if (audiencia.getActaDocumento() == null) {
                return ResponseEntity.notFound().build();
            }

            java.nio.file.Path rutaArchivo = java.nio.file.Paths.get("uploads/audiencias")
                    .resolve(audiencia.getActaDocumento());
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + recurso.getFilename() + "\"")
                        .body(recurso);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exportar-excel")
    public void exportarExcel(HttpServletResponse response,
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "gerencia", required = false) String gerencia,
            @RequestParam(value = "materia", required = false) String materia,
            @RequestParam(value = "estatus", required = false) EstatusAudiencia estatus,
            @RequestParam(value = "abogadoId", required = false) Integer abogadoId,
            @RequestParam(value = "periodo", required = false) PeriodoFiltro periodo) throws IOException {

        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Audiencias_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);

        // 1. SEGURIDAD DE ROLES
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        String rol = usuario.getRol().name();

        Integer filtroUsuarioId = null;
        Integer filtroGerenciaId = null;
        List<Integer> filtroMateriaIds = null;

        switch (rol) {
            case "ABOGADO":
                filtroUsuarioId = usuario.getId();
                break;
            case "GERENTE":
                if (usuario.getGerencia() != null)
                    filtroGerenciaId = usuario.getGerencia().getId();
                break;
            case "JEFE_DEPTO":
                if (usuario.getMaterias() != null && !usuario.getMaterias().isEmpty()) {
                    filtroMateriaIds = usuario.getMaterias().stream().map(Materia::getId).collect(Collectors.toList());
                } else {
                    filtroMateriaIds = new ArrayList<>();
                    filtroMateriaIds.add(-1);
                }
                break;
        }

        // 2. LÓGICA DE FECHAS "CENTINELA"
        LocalDate fechaInicio = LocalDate.of(1900, 1, 1);
        LocalDate fechaFin = LocalDate.of(2100, 12, 31);

        if (periodo != null) {
            LocalDate[] rango = periodo.calcularRango();
            fechaInicio = rango[0];
            fechaFin = rango[1];
        }

        // 3. LLAMAR AL REPOSITORIO
        List<Audiencia> listado = audienciaRepository.listarParaExcel(
                id, keyword, tipo, gerencia, materia, estatus,
                abogadoId, fechaInicio, fechaFin,
                filtroUsuarioId, filtroGerenciaId, filtroMateriaIds);

        AudienciaExcelExporter excelExporter = new AudienciaExcelExporter(listado);
        excelExporter.export(response);

    }

    // --- API: BUSCAR ABOGADOS DISPONIBLES (PARA SUSTITUCIÓN EN AUDIENCIAS) ---
    @GetMapping("/api/abogados-disponibles/{expedienteId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerAbogadosDisponibles(
            @PathVariable("expedienteId") String expedienteId) {
        try {
            // 1. Obtener datos del expediente
            Expediente exp = expedienteRepository.findById(UUID.fromString(expedienteId))
                    .orElseThrow(() -> new RuntimeException("Expediente no encontrado"));

            Integer gerenciaId = exp.getGerencia().getId();
            // materiaId no se requiere para filtrar abogados en audiencias según ROD-146

            // 2. Buscamos todos los usuarios activos de la gerencia (sin filtrar por
            // materia para audiencias)
            List<Usuario> abogadosGerencia = usuarioRepository.findAll().stream()
                    .filter(u -> u.getActivo() != null && u.getActivo()
                            && u.getGerencia() != null && u.getGerencia().getId().equals(gerenciaId))
                    .filter(u -> u.getRol() == RolUsuario.ABOGADO || u.getRol() == RolUsuario.GERENTE
                            || u.getRol() == RolUsuario.JEFE_DEPTO)
                    .collect(Collectors.toList());

            // 3. Preparamos el JSON
            List<Map<String, Object>> resultado = abogadosGerencia.stream()
                    .map(u -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getId());
                        map.put("nombre", u.getNombreCompleto());

                        // Marcamos quién es el titular original para distinguirlo visualmente
                        boolean esTitular = exp.getAbogadoResponsable() != null
                                && u.getId().equals(exp.getAbogadoResponsable().getId());
                        map.put("esTitular", esTitular);

                        return map;
                    })
                    .sorted((a, b) -> {
                        // Ordenar: Titular primero, luego alfabéticamente
                        if ((boolean) a.get("esTitular"))
                            return -1;
                        if ((boolean) b.get("esTitular"))
                            return 1;
                        return ((String) a.get("nombre")).compareTo((String) b.get("nombre"));
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/abogados-por-gerencia/{gerenciaId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerAbogadosPorGerencia(
            @PathVariable("gerenciaId") Integer gerenciaId) {
        try {
            List<Usuario> abogadosGerencia = usuarioRepository.findAll().stream()
                    .filter(u -> u.getActivo() != null && u.getActivo()
                            && u.getGerencia() != null && u.getGerencia().getId().equals(gerenciaId))
                    .filter(u -> u.getRol() == RolUsuario.ABOGADO || u.getRol() == RolUsuario.GERENTE
                            || u.getRol() == RolUsuario.JEFE_DEPTO)
                    .collect(Collectors.toList());

            List<Map<String, Object>> resultado = abogadosGerencia.stream()
                    .map(u -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getId());
                        map.put("nombre", u.getNombreCompleto());
                        map.put("esTitular", false);
                        return map;
                    })
                    .sorted((a, b) -> ((String) a.get("nombre")).compareToIgnoreCase((String) b.get("nombre")))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/materias-por-gerencia/{gerenciaId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerMateriasPorGerencia(
            @PathVariable("gerenciaId") Integer gerenciaId) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuarioActual = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

            List<Materia> materias = materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(gerenciaId);

            if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
                Set<Integer> misMateriasIds = usuarioActual.getMaterias().stream()
                        .map(Materia::getId)
                        .collect(Collectors.toSet());
                materias = materias.stream()
                        .filter(m -> misMateriasIds.contains(m.getId()))
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> resultado = materias.stream().map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("nombre", m.getNombre());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
