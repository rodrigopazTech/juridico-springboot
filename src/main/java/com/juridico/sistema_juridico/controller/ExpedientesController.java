package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.expediente.ActividadExpediente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Catalogo.EstadoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.EtiquetaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import com.juridico.sistema_juridico.Entity.catalogo.Etiqueta;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;

import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ActividadExpedienteRepository;
import com.juridico.sistema_juridico.repository.General.ComentarioRepository;
import com.juridico.sistema_juridico.Entity.general.Comentario;
import com.juridico.sistema_juridico.service.AuditoriaService;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import com.juridico.sistema_juridico.util.ExpedienteExcelExporter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/expedientes")
public class ExpedientesController {

    @Autowired
    private ExpedienteRepository expedienteRepository;
    @Autowired
    private GerenciaRepository gerenciaRepository;
    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private OrganoJurisdiccionalRepository organoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EstadoRepository estadoRepository;
    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private ComentarioRepository comentarioRepository;
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private CatPrioridadRepository catPrioridadRepository;
    @Autowired
    private com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository tipoAudienciaRepository;
    @Autowired
    private com.juridico.sistema_juridico.repository.Catalogo.TipoExpedienteRepository tipoExpedienteRepository;

    @Autowired
    private ActividadExpedienteRepository actividadRepository;
    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private AuditoriaService auditoriaService;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/exportar-excel")
    public void exportarExcel(HttpServletResponse response,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) Integer prioridadId,
            @RequestParam(required = false) EtapaProcesal etapa,
            @RequestParam(required = false) Integer abogadoId,
            @RequestParam(required = false) Integer etiquetaId) throws IOException {

        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Expedientes_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        Integer secGerenciaId = null;
        List<Integer> secMateriaIds = null;
        Integer secUsuarioId = usuario.getId();

        if (usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECCION
                || usuario.getRol() == RolUsuario.SUBDIRECCION) {
        } else if (usuario.getRol() == RolUsuario.GERENTE) {
            if (usuario.getGerencia() != null)
                secGerenciaId = usuario.getGerencia().getId();
        } else if (usuario.getRol() == RolUsuario.JEFE_DEPTO) {
            // Jefe de Depto: ve expedientes de sus materias dentro de su gerencia
            if (usuario.getGerencia() != null)
                secGerenciaId = usuario.getGerencia().getId();
            if (usuario.getMaterias() != null && !usuario.getMaterias().isEmpty()) {
                secMateriaIds = usuario.getMaterias().stream().map(m -> m.getId()).collect(Collectors.toList());
            } else {
                secMateriaIds = Collections.emptyList();
            }
        } else {
            // ABOGADO: solo ve sus propios expedientes
            if (usuario.getGerencia() != null)
                secGerenciaId = usuario.getGerencia().getId();
            secMateriaIds = Collections.emptyList();
        }

        // Buscamos todos los expedientes que coincidan con los filtros (sin paginación)
        List<Expediente> listaExpedientes = expedienteRepository.buscarConSeguridad(
                keyword, gerenciaId, materiaId, prioridadId, etapa, abogadoId, etiquetaId,
                secGerenciaId, secMateriaIds, secUsuarioId,
                Pageable.unpaged()).getContent();

        ExpedienteExcelExporter excelExporter = new ExpedienteExcelExporter(listaExpedientes);
        excelExporter.export(response);
    }

    @GetMapping
    public String index(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) Integer prioridadId,
            @RequestParam(required = false) EtapaProcesal etapa,
            @RequestParam(required = false) Integer abogadoId,
            @RequestParam(required = false) Integer etiquetaId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        Integer secGerenciaId = null;
        List<Integer> secMateriaIds = null;
        Integer secUsuarioId = usuario.getId();

        if (usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECCION
                || usuario.getRol() == RolUsuario.SUBDIRECCION) {
            // Ven todo
        } else if (usuario.getRol() == RolUsuario.GERENTE) {
            if (usuario.getGerencia() != null)
                secGerenciaId = usuario.getGerencia().getId();
        } else if (usuario.getRol() == RolUsuario.JEFE_DEPTO) {
            // Jefe de Depto: ve expedientes de sus materias dentro de su gerencia
            if (usuario.getGerencia() != null)
                secGerenciaId = usuario.getGerencia().getId();
            if (usuario.getMaterias() != null && !usuario.getMaterias().isEmpty()) {
                secMateriaIds = usuario.getMaterias().stream().map(m -> m.getId()).collect(Collectors.toList());
            } else {
                secMateriaIds = Collections.emptyList();
            }
        } else {
            // ABOGADO: solo ve sus propios expedientes (donde es abogadoResponsable)
            // Las materias asignadas no expanden su visibilidad
            if (usuario.getGerencia() != null)
                secGerenciaId = usuario.getGerencia().getId();
            secMateriaIds = Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());

        Page<Expediente> paginaExpedientes = expedienteRepository.buscarConSeguridad(
                keyword, gerenciaId, materiaId, prioridadId, etapa, abogadoId, etiquetaId,
                secGerenciaId, secMateriaIds, secUsuarioId,
                pageable);

        // Conteos para resumen (Respetando seguridad)
        Integer idAlta = catPrioridadRepository.findByNombreIgnoreCase("ALTA").map(CatPrioridad::getId).orElse(null);
        long totalAlta = expedienteRepository.buscarConSeguridad(
                null, null, null, idAlta, null, null, null,
                secGerenciaId, secMateriaIds, secUsuarioId,
                PageRequest.of(0, 1)).getTotalElements();

        long totalTramite = expedienteRepository.buscarConSeguridad(
                null, null, null, null, EtapaProcesal.TRAMITE, null, null,
                secGerenciaId, secMateriaIds, secUsuarioId,
                PageRequest.of(0, 1)).getTotalElements();

        model.addAttribute("totalAlta", totalAlta);
        model.addAttribute("totalTramite", totalTramite);
        model.addAttribute("totalNuevos", paginaExpedientes.getTotalElements()); // Placeholder for "New"

        model.addAttribute("expedientes", paginaExpedientes);
        model.addAttribute("keyword", keyword);
        model.addAttribute("paramGerenciaId", gerenciaId);
        model.addAttribute("paramMateriaId", materiaId);
        model.addAttribute("paramPrioridadId", prioridadId);
        model.addAttribute("paramEtapa", etapa != null ? etapa.name() : null);
        model.addAttribute("paramAbogadoId", abogadoId);
        model.addAttribute("paramEtiquetaId", etiquetaId);
        model.addAttribute("pageTitle", "Gestión de Expedientes");
        model.addAttribute("activePage", "expedientes");

        model.addAttribute("rolActual", usuario.getRol());

        // --- FILTRADO DE CATÁLOGOS SEGÚN ROL ---
        if (usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECCION || usuario.getRol() == RolUsuario.SUBDIRECCION) {
            model.addAttribute("listaGerencias", gerenciaRepository.findAll());
            model.addAttribute("listaMaterias", materiaRepository.findByActivoTrueOrderByNombreAsc());
            model.addAttribute("listaAbogados", usuarioRepository.findAll());
            model.addAttribute("gerencias", gerenciaRepository.findAll());
            model.addAttribute("materias", materiaRepository.findByActivoTrueOrderByNombreAsc());
            model.addAttribute("usuarios", usuarioRepository.findAll());
        } else {
            Integer gid = (usuario.getGerencia() != null) ? usuario.getGerencia().getId() : null;
            if (gid != null) {
                List<Materia> mats = materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(gid);
                List<Usuario> usrs = usuarioRepository.findByGerenciaIdAndActivoTrueOrderByNombreCompletoAsc(gid);
                model.addAttribute("listaGerencias", List.of(usuario.getGerencia()));
                model.addAttribute("gerencias", List.of(usuario.getGerencia()));
                model.addAttribute("listaMaterias", mats);
                model.addAttribute("materias", mats);
                model.addAttribute("listaAbogados", usrs);
                model.addAttribute("usuarios", usrs);
            } else {
                model.addAttribute("listaGerencias", Collections.emptyList());
                model.addAttribute("gerencias", Collections.emptyList());
                model.addAttribute("listaMaterias", Collections.emptyList());
                model.addAttribute("materias", Collections.emptyList());
                model.addAttribute("listaAbogados", Collections.emptyList());
                model.addAttribute("usuarios", Collections.emptyList());
            }
        }

        model.addAttribute("listaPrioridades", catPrioridadRepository.findAll());
        model.addAttribute("etapas", EtapaProcesal.values());
        model.addAttribute("organos", organoRepository.findByActivoTrue());
        model.addAttribute("prioridades", catPrioridadRepository.findAll());
        model.addAttribute("estados", estadoRepository.findAllByActivoTrueOrderByNombreAsc());
        model.addAttribute("etiquetas", etiquetaRepository.findAll());
        model.addAttribute("tiposExpediente", tipoExpedienteRepository.findAll());
        model.addAttribute("usuarioActual", usuario);

        // --- OPTIMIZACIÓN HTMX / FETCH ASÍNCRONO ---
        // Verificar si la petición es asíncrona (para búsqueda en vivo veloz sin recargar toda la plantilla)
        HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) 
            org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();
        String isAjax = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(isAjax)) {
            // Devuelve solo el fragmento de la grilla de la tabla
            return "views/expedientes/index :: gridResultados";
        }

        return "views/expedientes/index";
    }

    @PostMapping("/guardar")
    @Transactional
    public String guardar(@ModelAttribute Expediente expedienteForm,
            @RequestParam(value = "etiquetasNombres", required = false) List<String> etiquetasNombres,
            RedirectAttributes redirectAttrs) {

        UUID idParaRedirigir = null;

        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            boolean esNuevo = (expedienteForm.getId() == null);
            Expediente anterior = null;
            Expediente expedienteReal;

            if (esNuevo) {
                // Validación de Roles para Creación
                String rol = usuario.getRol().name();
                if (!rol.equals("ADMIN") && !rol.equals("DIRECCION") && !rol.equals("SUBDIRECCION")) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "No tienes permiso para crear expedientes.");
                }

                expedienteReal = expedienteForm;
                expedienteReal.setCreatedAt(LocalDateTime.now());
                expedienteReal.setEtapaProcesal(EtapaProcesal.TRAMITE);

                // Inyectar relaciones reales de la base de datos (Creación)
                if (expedienteForm.getGerencia() != null && expedienteForm.getGerencia().getId() != null)
                    expedienteReal.setGerencia(
                            gerenciaRepository.findById(expedienteForm.getGerencia().getId()).orElse(null));
                if (expedienteForm.getMateria() != null && expedienteForm.getMateria().getId() != null)
                    expedienteReal
                            .setMateria(materiaRepository.findById(expedienteForm.getMateria().getId()).orElse(null));
                if (expedienteForm.getOrganoJurisdiccional() != null
                        && expedienteForm.getOrganoJurisdiccional().getId() != null)
                    expedienteReal.setOrganoJurisdiccional(
                            organoRepository.findById(expedienteForm.getOrganoJurisdiccional().getId()).orElse(null));
                if (expedienteForm.getAbogadoResponsable() != null
                        && expedienteForm.getAbogadoResponsable().getId() != null)
                    expedienteReal.setAbogadoResponsable(
                            usuarioRepository.findById(expedienteForm.getAbogadoResponsable().getId()).orElse(null));

                // ── VALIDACIÓN DE DUPLICADOS (Regla de Negocio) ──────────────────────────
                // Si el número NO es S/N, aplicamos la regla clásica: número único.
                // Si el número ES S/N (u otro valor repetido), permitimos duplicarlo SOLO si
                // las partes procesales son distintas. Mismo número + mismas partes = duplicado.
                String numeroIngresado = expedienteForm.getNumero() != null ? expedienteForm.getNumero().trim() : "";
                String partesIngresadas = expedienteForm.getPartes() != null ? expedienteForm.getPartes().trim() : "";

                if ("S/N".equalsIgnoreCase(numeroIngresado) || expedienteRepository.existsByNumero(numeroIngresado)) {
                    // Ya existe al menos un expediente con este número → verificar partes
                    boolean duplicadoExacto = expedienteRepository
                        .findByNumeroIgnoreCaseAndPartesIgnoreCase(numeroIngresado, partesIngresadas)
                        .isPresent();
                    if (duplicadoExacto) {
                        redirectAttrs.addFlashAttribute("mensaje",
                            "Ya existe un expediente con el número \"" + numeroIngresado +
                            "\" y las mismas partes procesales. Si es un asunto diferente, cambia las partes.");
                        redirectAttrs.addFlashAttribute("tipo", "error");
                        return "redirect:/expedientes";
                    }
                    // Si las partes son distintas → se permite crear (continúa el flujo)
                }
                // ──────────────────────────────────────────────────────────────────────────


            } else {
                // MODO EDICIÓN
                Expediente dbOriginal = expedienteRepository.findById(expedienteForm.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("El expediente no existe."));

                // Guardar la foto del "antes" para la auditoría (Detach)
                entityManager.detach(dbOriginal);
                anterior = dbOriginal;

                // Obtener copia fresca para inyectarle los cambios
                expedienteReal = expedienteRepository.findById(expedienteForm.getId()).get();

                // Actualizar SÓLO los campos de texto
                expedienteReal.setNumero(expedienteForm.getNumero());
                expedienteReal.setPrioridad(expedienteForm.getPrioridad());
                expedienteReal.setSede(expedienteForm.getSede());
                expedienteReal.setPartes(expedienteForm.getPartes());
                expedienteReal.setDescripcion(expedienteForm.getDescripcion());
                // Etapa procesal (desde el select del formulario de edición)
                if (expedienteForm.getEtapaProcesal() != null) {
                    expedienteReal.setEtapaProcesal(expedienteForm.getEtapaProcesal());
                }

                // Inyectar relaciones reales (evita errores de Hibernate por objetos "huecos")
                if (expedienteForm.getGerencia() != null && expedienteForm.getGerencia().getId() != null)
                    expedienteReal.setGerencia(
                            gerenciaRepository.findById(expedienteForm.getGerencia().getId()).orElse(null));

                if (expedienteForm.getMateria() != null && expedienteForm.getMateria().getId() != null)
                    expedienteReal
                            .setMateria(materiaRepository.findById(expedienteForm.getMateria().getId()).orElse(null));

                if (expedienteForm.getOrganoJurisdiccional() != null
                        && expedienteForm.getOrganoJurisdiccional().getId() != null)
                    expedienteReal.setOrganoJurisdiccional(
                            organoRepository.findById(expedienteForm.getOrganoJurisdiccional().getId()).orElse(null));

                if (expedienteForm.getAbogadoResponsable() != null
                        && expedienteForm.getAbogadoResponsable().getId() != null)
                    expedienteReal.setAbogadoResponsable(
                            usuarioRepository.findById(expedienteForm.getAbogadoResponsable().getId()).orElse(null));
                

            }

            if (expedienteReal.getNumero() == null || expedienteReal.getNumero().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número es obligatorio.");
            }

            if (expedienteReal.getOrganoJurisdiccional() == null || expedienteReal.getOrganoJurisdiccional().getId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El Órgano Jurisdiccional es obligatorio.");
            }

            // ====== PROCESAMIENTO DE ETIQUETAS ======
            Set<Etiqueta> etiquetasEntidad = new HashSet<>();
            if (etiquetasNombres != null && !etiquetasNombres.isEmpty()) {
                for (String nombreTag : etiquetasNombres) {
                    if (nombreTag == null || nombreTag.trim().isEmpty())
                        continue;
                    String nombreLimpio = nombreTag.trim();
                    Etiqueta tag = etiquetaRepository.findByNombreIgnoreCase(nombreLimpio)
                            .orElseGet(() -> {
                                Etiqueta nueva = Etiqueta.builder()
                                        .nombre(nombreLimpio)
                                        .color("bg-gray-100 text-gray-800")
                                        .build();
                                return etiquetaRepository.save(nueva);
                            });
                    etiquetasEntidad.add(tag);
                }
            }
            expedienteReal.setEtiquetas(etiquetasEntidad);
            // ==========================================

            expedienteReal.setUpdatedAt(LocalDateTime.now());
            Expediente guardado = expedienteRepository.save(expedienteReal);
            idParaRedirigir = guardado.getId();

            if (esNuevo) {
                enviarNotificacionesNuevoExpediente(guardado);
                auditoriaService.registrar(guardado, "Expediente Creado",
                        "Se ha registrado un nuevo expediente: " + guardado.getNumero(), "PLUS", usuario);
            } else {
                auditoriaService.registrarCambiosEstructurados(guardado, anterior, usuario);

                if (anterior != null && guardado.getAbogadoResponsable() != null) {
                    Integer idAnterior = (anterior.getAbogadoResponsable() != null)
                            ? anterior.getAbogadoResponsable().getId()
                            : null;
                    Integer idNuevo = guardado.getAbogadoResponsable().getId();

                    if (!idNuevo.equals(idAnterior)
                            && !guardado.getAbogadoResponsable().getEmail().equalsIgnoreCase(usuario.getEmail())) {
                        notificacionService.crearNotificacion(
                                guardado.getAbogadoResponsable(),
                                "Expediente Reasignado",
                                "Se te ha reasignado el expediente " + guardado.getNumero() + " para su atención.",
                                "ASIGNACION",
                                catPrioridadRepository.findByNombreIgnoreCase("ALTA").orElse(null),
                                guardado.getId().toString());
                    }
                }
            }

            redirectAttrs.addFlashAttribute("mensaje", "Expediente guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (DataIntegrityViolationException e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error de integridad al guardar el expediente. Verifica que los datos sean correctos.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        } catch (Exception e) {
            System.out.println("Error al guardar: " + e.getMessage());
            redirectAttrs.addFlashAttribute("mensaje", "Ocurrió un error interno al guardar.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }

        // UX MEJORADA: Redirigir al detalle si se editó, a la lista si se creó
        if (idParaRedirigir != null && expedienteForm.getId() != null) {
            return "redirect:/expedientes/" + idParaRedirigir;
        }
        return "redirect:/expedientes";
    }

    /**
     * Envía notificaciones al Abogado, Gerente y Jefe de Depto correspondiente.
     */
    private void enviarNotificacionesNuevoExpediente(Expediente exp) {
        // 1. RECUPERAR EL NOMBRE DE LA MATERIA (Parche para el NULL)
        String nombreMateria = "General";

        if (exp.getMateria() != null) {
            // Si el nombre viene nulo (porque Spring solo bindeó el ID), lo buscamos:
            if (exp.getMateria().getNombre() == null && exp.getMateria().getId() != null) {
                nombreMateria = materiaRepository.findById(exp.getMateria().getId())
                        .map(m -> m.getNombre())
                        .orElse("General");
            } else {
                nombreMateria = exp.getMateria().getNombre();
            }
        }

        String titulo = "Nuevo Expediente Registrado";
        // Usamos la variable 'nombreMateria' que ya garantizamos que tiene texto
        String mensaje = "Se ha creado el expediente " + exp.getNumero() + " (" + nombreMateria + ")";

        List<Usuario> destinatarios = new ArrayList<>();

        // A) Abogado Responsable (Verificamos que solo traiga ID y buscamos el objeto
        // completo si es necesario,
        // aunque para notificar solo necesitamos el objeto usuario, hibernate lo maneja
        // bien si el ID es correcto)
        if (exp.getAbogadoResponsable() != null) {
            destinatarios.add(exp.getAbogadoResponsable());
        }

        // B) Gerente de la Gerencia asignada
        if (exp.getGerencia() != null) {
            List<Usuario> gerentes = usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.GERENTE,
                    exp.getGerencia());
            destinatarios.addAll(gerentes);
        }

        // C) Jefe de Depto (Que tenga la Materia asignada)
        if (exp.getMateria() != null) {
            List<Usuario> jefes = usuarioRepository.findByRolAndMateriaId(RolUsuario.JEFE_DEPTO,
                    exp.getMateria().getId());
            destinatarios.addAll(jefes);
        }

        // Enviar notificaciones
        // Usamos un Set para evitar duplicados si una persona tuviera doble rol (raro
        // pero posible)

        // Obtener el registro Alta del catalogo
        CatPrioridad prioAlta = catPrioridadRepository.findByNombreIgnoreCase("ALTA").orElse(null);
        for (Usuario u : new HashSet<>(destinatarios)) {
            notificacionService.crearNotificacion(
                    u,
                    titulo,
                    mensaje,
                    "EXPEDIENTE",
                    prioAlta,
                    exp.getId().toString());
        }
    }

    @GetMapping("/{id}")
    @SuppressWarnings("null")
    public String verDetalle(@PathVariable("id") UUID id, Model model) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        model.addAttribute("usuario", usuario);
        model.addAttribute("rolActual", usuario.getRol());

        Expediente expediente = expedienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + id));

        // --- PREVENCIÓN DE IDOR ---
        boolean tienePermiso = false;
        String rol = usuario.getRol().name();

        if (rol.equals("ADMIN") || rol.equals("DIRECCION") || rol.equals("SUBDIRECCION")) {
            tienePermiso = true;
        } else if (rol.equals("GERENTE") && usuario.getGerencia() != null
                && expediente.getGerencia() != null
                && expediente.getGerencia().getId().equals(usuario.getGerencia().getId())) {
            tienePermiso = true;
        } else if (rol.equals("JEFE_DEPTO") && !usuario.getMaterias().isEmpty()
                && expediente.getMateria() != null) {
            boolean materiaCoincide = usuario.getMaterias().stream()
                    .anyMatch(m -> m.getId().equals(expediente.getMateria().getId()));
            if (materiaCoincide) {
                tienePermiso = true;
            }
        } else if (expediente.getAbogadoResponsable() != null
                && expediente.getAbogadoResponsable().getId().equals(usuario.getId())) {
            tienePermiso = true;
        }

        if (!tienePermiso) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "No tienes permiso para ver este expediente.");
        }
        // --- FIN PREVENCIÓN DE IDOR ---

        model.addAttribute("expediente", expediente);

        // Búsqueda de próxima audiencia excluida concluida
        Audiencia proxima = audienciaRepository.findProximaAudiencia(id, LocalDate.now())
                .stream().findFirst().orElse(null);
        model.addAttribute("proximaAudiencia", proxima);

        model.addAttribute("gerencias", gerenciaRepository.findAll());

        model.addAttribute("organos", organoRepository.findByActivoTrue());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("prioridades", catPrioridadRepository.findAll());
        model.addAttribute("estados", estadoRepository.findByActivoTrue());
        model.addAttribute("etapas", EtapaProcesal.values());
        model.addAttribute("materias", materiaRepository.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("tipos", tipoAudienciaRepository.findByActivoTrue());
        model.addAttribute("tiposExpediente", tipoExpedienteRepository.findAll());
        model.addAttribute("pageTitle", "Detalle del Expediente");
        model.addAttribute("activePage", "expedientes");

        // CARGAR HISTORIAL DE ACTIVIDADES (Bitácora)
        List<ActividadExpediente> actividades = actividadRepository.findByExpedienteIdOrderByFechaRegistroDesc(id);
        model.addAttribute("actividades", actividades);

        return "views/expedientes/detalle";
    }

    // --- ENDPOINTS PARA COMENTARIOS (NOTAS) ---

    @GetMapping("/{id}/comentarios")
    @ResponseBody
    public List<Comentario> getComentarios(@PathVariable("id") String id) {
        return comentarioRepository.findByEntidadTipoAndEntidadIdOrderByCreatedAtDesc("EXPEDIENTE", id);
    }

    @PostMapping("/{id}/comentarios")
    @ResponseBody
    public ResponseEntity<?> addComentario(@PathVariable("id") String id, @RequestBody Comentario nuevo) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        nuevo.setEntidadTipo("EXPEDIENTE");
        nuevo.setEntidadId(id);
        nuevo.setCreatedAt(LocalDateTime.now());
        nuevo.setUsuario(usuario);
        nuevo.setUsuarioNombre(usuario != null ? usuario.getNombreCompleto() : "Sistema");

        // Capturar la etapa procesal actual del expediente
        try {
            UUID expId = UUID.fromString(id);
            expedienteRepository.findById(expId).ifPresent(exp -> {
                nuevo.setEtapaProcesal(exp.getEtapaProcesal());

                auditoriaService.registrar(exp, "Nueva Observación",
                        "Se agregó una nota: "
                                + (nuevo.getComentario().length() > 50 ? nuevo.getComentario().substring(0, 47) + "..."
                                        : nuevo.getComentario()),
                        "EDIT", usuario);
            });
        } catch (Exception e) {
            // Ignorar si el ID no es de expediente
        }

        comentarioRepository.save(nuevo);

        return ResponseEntity.ok(nuevo);
    }

    // --- ACTUALIZACIÓN DE ESTADO (ETAPA) ---

    @PostMapping("/{id}/cambiar-etapa")
    @ResponseBody
    public ResponseEntity<?> cambiarEtapa(@PathVariable("id") UUID id, @RequestParam("etapa") EtapaProcesal etapa) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Expediente exp = expedienteRepository.findById(id).orElseThrow();
        EtapaProcesal anterior = exp.getEtapaProcesal();

        exp.setEtapaProcesal(etapa);
        exp.setUpdatedAt(LocalDateTime.now());
        expedienteRepository.save(exp);

        auditoriaService.registrar(exp, "Cambio de Etapa Procesal",
                "La etapa ha cambiado de " + anterior + " a " + etapa, "EDIT", usuario);

        return ResponseEntity.ok().build();
    }

    // --- API PARA SELECTORES EN CASCADA (NUEVO EXPEDIENTE) ---

    @GetMapping("/api/abogados")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerAbogadosPorMateria(
            @RequestParam("gerenciaId") Integer gerenciaId,
            @RequestParam("materiaId") Integer materiaId) {

        // Buscamos todos los usuarios activos de la gerencia que tengan la materia
        // asignada
        // o que sean Gerentes (que ven todo)
        List<Usuario> usuariosGerencia = usuarioRepository.findAll().stream()
                .filter(u -> u.getActivo() != null && u.getActivo()
                        && u.getGerencia() != null && u.getGerencia().getId().equals(gerenciaId))
                .filter(u -> {
                    RolUsuario rol = u.getRol();
                    if (rol == RolUsuario.GERENTE) {
                        return true; // Gerentes ven todas las materias
                    }
                    if (rol == RolUsuario.ABOGADO || rol == RolUsuario.JEFE_DEPTO) {
                        return u.tieneMateria(materiaId); // Abogados y Jefes filtrados por materia
                    }
                    return false;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> response = usuariosGerencia.stream()
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("nombreCompleto", u.getNombreCompleto());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/organos-por-gerencia/{gerenciaId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerOrganosPorGerencia(@PathVariable("gerenciaId") Integer gerenciaId) {
        // Buscar órganos específicos de la gerencia
        List<Map<String, Object>> res = organoRepository.findByGerenciaIdAndActivoTrue(gerenciaId).stream()
                .map(o -> Map.of("id", (Object) o.getId(), "nombre", o.getNombre()))
                .collect(Collectors.toList());

        // Si no hay órganos para esta gerencia, devolver los órganos generales (sin gerencia asignada)
        if (res.isEmpty()) {
            res = organoRepository.findByGerenciaIsNullAndActivoTrue().stream()
                    .map(o -> Map.of("id", (Object) o.getId(), "nombre", o.getNombre()))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(res);
    }
}