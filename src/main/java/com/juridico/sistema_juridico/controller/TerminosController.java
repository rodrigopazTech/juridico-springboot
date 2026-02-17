package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoPresentado;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoPresentadoRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.util.TerminoExcelExporter;

import jakarta.servlet.http.HttpServletResponse;
import com.juridico.sistema_juridico.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
    private NotificacionService notificacionService;
    @Autowired
    private SecurityService securityService;

    // --- MATRIZ DE PERMISOS ---
    private static final Map<EstatusTermino, List<RolUsuario>> PERMISOS_ETAPAS = new HashMap<>();
    static {
        PERMISOS_ETAPAS.put(EstatusTermino.PROYECTISTA, Arrays.asList(RolUsuario.ABOGADO, RolUsuario.GERENTE,
                RolUsuario.JEFE_DEPTO, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put(EstatusTermino.REVISION, Arrays.asList(RolUsuario.JEFE_DEPTO, RolUsuario.GERENTE,
                RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put(EstatusTermino.GERENCIA,
                Arrays.asList(RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put(EstatusTermino.DIRECCION, Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put(EstatusTermino.LIBERADO, Arrays.asList(RolUsuario.ABOGADO, RolUsuario.JEFE_DEPTO,
                RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put(EstatusTermino.PRESENTADO, Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION,
                RolUsuario.ABOGADO, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO));
        PERMISOS_ETAPAS.put(EstatusTermino.CONCLUIDO, Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION,
                RolUsuario.ABOGADO, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO));
    }

    @GetMapping
    public String index(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EstatusTermino estatus,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) Integer abogadoId) { // Filtro del usuario (opcional)

        Usuario usuarioActual = getUsuarioActual();
        Pageable pageable = PageRequest.of(page, 10, Sort.by("fechaVencimiento").ascending());

        Integer filtroAbogadoSeguro = abogadoId;
        Integer filtroGerenciaSeguro = null;

        if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
            filtroAbogadoSeguro = usuarioActual.getId();
        } else if (usuarioActual.getRol() == RolUsuario.GERENTE
                || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            if (usuarioActual.getGerencia() != null) {
                filtroGerenciaSeguro = usuarioActual.getGerencia().getId();
            }
        }

        Page<Termino> paginaTerminos = terminoRepository.buscarConFiltros(
                keyword, estatus, prioridad,
                filtroAbogadoSeguro,
                filtroGerenciaSeguro,
                pageable);

        model.addAttribute("terminos", paginaTerminos);

        model.addAttribute("abogados", usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO));
        model.addAttribute("listaAbogados", usuarioRepository.findAll());
        model.addAttribute("listaPrioridades", Prioridad.values());
        model.addAttribute("listaEstatus", EstatusTermino.values());
        model.addAttribute("expedientesList", expedienteRepository.findAll());
        model.addAttribute("rolActual", usuarioActual.getRol());
        model.addAttribute("mapaPermisos", PERMISOS_ETAPAS);

        model.addAttribute("keyword", keyword);
        model.addAttribute("paramEstatus", estatus);
        model.addAttribute("paramPrioridad", prioridad);
        model.addAttribute("paramAbogadoId", abogadoId);

        model.addAttribute("activePage", "terminos");

        return "views/terminos/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Termino terminoForm,
            @RequestParam(value = "expedienteId", required = false) UUID expedienteId,
            RedirectAttributes redirectAttrs) {
        try {
            Usuario actor = getUsuarioActual();
            Termino terminoGuardar;
            boolean esNuevo = false;

            if (terminoForm.getId() != null) {
                Termino existente = terminoRepository.findById(terminoForm.getId()).orElse(null);
                if (existente == null)
                    return "redirect:/terminos";

                // SEGURIDAD ROD-39: Validar acceso de escritura al expediente actual del
                // término
                if (!securityService.tieneAccesoEscritura(actor, existente.getExpediente())) {
                    redirectAttrs.addFlashAttribute("mensaje", "Acceso denegado: No tiene permisos de escritura.");
                    redirectAttrs.addFlashAttribute("tipo", "error");
                    return "redirect:/terminos";
                }

                existente.setActuacion(terminoForm.getActuacion());
                existente.setFechaVencimiento(terminoForm.getFechaVencimiento());
                existente.setAbogadoResponsable(terminoForm.getAbogadoResponsable());
                existente.setUpdatedAt(LocalDateTime.now());

                if (expedienteId != null) {
                    Expediente exp = expedienteRepository.findById(expedienteId).orElse(null);
                    if (exp != null)
                        existente.setExpediente(exp);
                }
                terminoGuardar = existente;
            } else {
                esNuevo = true;
                terminoGuardar = terminoForm;
                terminoGuardar.setFechaIngreso(LocalDate.now());
                terminoGuardar.setEstatusTermino(EstatusTermino.PROYECTISTA);
                terminoGuardar.setCreatedAt(LocalDateTime.now());
                terminoGuardar.setUpdatedAt(LocalDateTime.now());

                if (expedienteId != null) {
                    Expediente exp = expedienteRepository.findById(expedienteId).orElse(null);
                    if (exp != null) {
                        // VALIDACIÓN ROD-19
                        if (!securityService.tieneAccesoEscritura(actor, exp)) {
                            redirectAttrs.addFlashAttribute("mensaje",
                                    "Acceso denegado: No tiene permisos de escritura para este expediente.");
                            redirectAttrs.addFlashAttribute("tipo", "error");
                            return "redirect:/terminos";
                        }
                        terminoGuardar.setExpediente(exp);

                        if (terminoGuardar.getPrioridad() == null) {
                            terminoGuardar.setPrioridad(exp.getPrioridad());
                        }

                        if (terminoGuardar.getAbogadoResponsable() == null) {
                            terminoGuardar.setAbogadoResponsable(exp.getAbogadoResponsable());
                        }
                    }
                }
            }

            Termino terminoGuardado = terminoRepository.save(terminoGuardar);

            if (esNuevo && terminoGuardado.getAbogadoResponsable() != null) {
                notificacionService.crearNotificacion(
                        terminoGuardado.getAbogadoResponsable(),
                        "Nuevo Término Asignado",
                        "El usuario " + actor.getNombreCompleto() + " te ha asignado: "
                                + terminoGuardado.getActuacion(),
                        "TERMINO",
                        terminoGuardado.getPrioridad(),
                        terminoGuardado.getId().toString());
            }

            redirectAttrs.addFlashAttribute("mensaje", "Término guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    @GetMapping("/avanzar/{id}")
    public String avanzarEstado(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Termino termino = terminoRepository.findById(id).orElse(null);
        if (termino != null) {
            Usuario actor = getUsuarioActual();

            // SEGURIDAD ROD-39: Validar acceso de escritura
            if (!securityService.tieneAccesoEscritura(actor, termino.getExpediente())) {
                redirectAttrs.addFlashAttribute("mensaje",
                        "Acceso denegado: No tiene permisos para modificar este término.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            EstatusTermino etapaAnterior = termino.getEstatusTermino();

            if (!tienePermiso(etapaAnterior, actor.getRol())) {
                redirectAttrs.addFlashAttribute("mensaje", "⛔ No tienes permiso para avanzar la etapa.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            if (EstatusTermino.PROYECTISTA == etapaAnterior && termino.getArchivoWord() == null) {
                redirectAttrs.addFlashAttribute("mensaje", "Sube el archivo Word antes de avanzar.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            EstatusTermino siguienteEtapa = calcularSiguienteEstado(etapaAnterior);
            termino.setEstatusTermino(siguienteEtapa);
            if (EstatusTermino.PRESENTADO == siguienteEtapa)
                termino.setFechaPresentacion(LocalDate.now());

            terminoRepository.save(termino);

            notificarCambioDeEtapa(termino, etapaAnterior, siguienteEtapa, actor);

            redirectAttrs.addFlashAttribute("mensaje", "Avanzó a: " + siguienteEtapa.getNombre());
            redirectAttrs.addFlashAttribute("tipo", "success");
        }
        return "redirect:/terminos";
    }

    private void notificarCambioDeEtapa(Termino termino, EstatusTermino etapaAnterior, EstatusTermino nuevaEtapa,
            Usuario actor) {
        String titulo = "Término en " + nuevaEtapa.getNombre();
        String mensajeBase = String.format("Movimiento de '%s' a '%s' por %s", etapaAnterior.getNombre(),
                nuevaEtapa.getNombre(), actor.getNombreCompleto());

        List<Usuario> destinatarios = new ArrayList<>();
        Gerencia g = termino.getExpediente().getGerencia();

        switch (nuevaEtapa) {
            case REVISION:
                destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.JEFE_DEPTO, g));
                break;
            case GERENCIA:
                destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.GERENTE, g));
                break;
            case DIRECCION:
                destinatarios.addAll(usuarioRepository
                        .findByRolInAndActivoTrue(Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION)));
                break;
            case LIBERADO:
                if (termino.getAbogadoResponsable() != null)
                    destinatarios.add(termino.getAbogadoResponsable());
                destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.JEFE_DEPTO, g));
                break;
            case PRESENTADO:
                destinatarios.addAll(usuarioRepository.findByRolInAndActivoTrue(Arrays.asList(RolUsuario.DIRECCION)));
                break;
            default:
                break;
        }

        Set<Usuario> unicos = new HashSet<>(destinatarios);
        for (Usuario u : unicos) {
            if (!u.getId().equals(actor.getId())) {
                notificacionService.crearNotificacion(u, titulo, mensajeBase, "TERMINO", Prioridad.ALTA,
                        termino.getId().toString());
            }
        }
    }

    @PostMapping("/subir-archivo")
    public String subirArchivo(@RequestParam("id") Integer id,
            @RequestParam("archivo") MultipartFile archivo,
            RedirectAttributes redirectAttrs) {

        if (archivo.isEmpty()) {
            redirectAttrs.addFlashAttribute("mensaje", "Por favor selecciona un archivo.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }

        try {
            Termino termino = terminoRepository.findById(id).orElse(null);
            if (termino != null) {
                Usuario actor = getUsuarioActual();

                // SEGURIDAD ROD-39: Validar acceso de escritura
                if (!securityService.tieneAccesoEscritura(actor, termino.getExpediente())) {
                    redirectAttrs.addFlashAttribute("mensaje",
                            "Acceso denegado: No tiene permisos para subir archivos.");
                    redirectAttrs.addFlashAttribute("tipo", "error");
                    return "redirect:/terminos";
                }

                // Validación: No permitir cambiar archivo si ya está en etapas finales
                EstatusTermino st = termino.getEstatusTermino();
                if (EstatusTermino.LIBERADO == st || EstatusTermino.PRESENTADO == st
                        || EstatusTermino.CONCLUIDO == st) {
                    redirectAttrs.addFlashAttribute("mensaje", "El documento está bloqueado en esta etapa.");
                    redirectAttrs.addFlashAttribute("tipo", "error");
                    return "redirect:/terminos";
                }

                // 1. Crear directorio si no existe
                String carpetaDestino = "uploads/terminos/";
                Path rutaCarpeta = Paths.get(carpetaDestino);
                if (!Files.exists(rutaCarpeta))
                    Files.createDirectories(rutaCarpeta);

                // 2. Guardar archivo con nombre único (ID_NombreOriginal)
                String nombreFinal = id + "_" + archivo.getOriginalFilename();
                Files.copy(archivo.getInputStream(), rutaCarpeta.resolve(nombreFinal),
                        StandardCopyOption.REPLACE_EXISTING);

                // 3. Actualizar base de datos
                termino.setArchivoWord(nombreFinal);
                termino.setUpdatedAt(LocalDateTime.now());
                terminoRepository.save(termino);

                redirectAttrs.addFlashAttribute("mensaje", "Borrador actualizado correctamente.");
                redirectAttrs.addFlashAttribute("tipo", "success");
            }
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Error al guardar el archivo: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    // 4. SUBIR ACUSE (CON SEGURIDAD RBAC)
    @PostMapping("/subir-acuse")
    public String subirAcuse(@RequestParam("id") Integer id,
            @RequestParam("archivoAcuse") MultipartFile archivo,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            RedirectAttributes redirectAttrs) {

        Usuario actor = getUsuarioActual();

        // --- VALIDACIÓN DE PERMISOS (Para concluir) ---
        // Usamos la regla de 'Presentado' porque es la etapa actual
        if (!tienePermiso(EstatusTermino.PRESENTADO, actor.getRol())) {
            redirectAttrs.addFlashAttribute("mensaje", "⛔ No tienes permiso para concluir términos.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }

        // SEGURIDAD ROD-39: Validar acceso de escritura
        try {
            Termino termino = terminoRepository.findById(id).orElse(null);
            if (termino != null) {
                if (!securityService.tieneAccesoEscritura(actor, termino.getExpediente())) {
                    redirectAttrs.addFlashAttribute("mensaje", "Acceso denegado: No tiene acceso a este expediente.");
                    redirectAttrs.addFlashAttribute("tipo", "error");
                    return "redirect:/terminos";
                }

                if (EstatusTermino.PRESENTADO == termino.getEstatusTermino()) {
                    String carpeta = "uploads/acuses/";
                    Path ruta = Paths.get(carpeta);
                    if (!Files.exists(ruta))
                        Files.createDirectories(ruta);
                    String nombreAcuse = "ACUSE_" + id + "_" + archivo.getOriginalFilename();
                    Files.copy(archivo.getInputStream(), ruta.resolve(nombreAcuse),
                            StandardCopyOption.REPLACE_EXISTING);

                    TerminoPresentado presentado = TerminoPresentado.builder()
                            .termino(termino)
                            .expedienteNumero(termino.getExpediente().getNumero())
                            .fechaPresentacion(LocalDate.now())
                            .acuseDocumento(nombreAcuse)
                            .sincronizadoAt(LocalDateTime.now()).build();
                    terminoPresentadoRepository.save(presentado);

                    termino.setEstatusTermino(EstatusTermino.CONCLUIDO);
                    termino.setObservaciones(observaciones);
                    if (termino.getFechaPresentacion() == null)
                        termino.setFechaPresentacion(LocalDate.now());
                    terminoRepository.save(termino);

                    redirectAttrs.addFlashAttribute("mensaje", "¡Término Concluido!");
                    redirectAttrs.addFlashAttribute("tipo", "success");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/terminos";
    }

    private boolean tienePermiso(EstatusTermino etapaActual, RolUsuario rolUsuario) {
        if (etapaActual == null)
            return false;
        List<RolUsuario> permitidos = PERMISOS_ETAPAS.get(etapaActual);
        return permitidos != null && permitidos.contains(rolUsuario);
    }

    private EstatusTermino calcularSiguienteEstado(EstatusTermino actual) {
        if (actual == null)
            return EstatusTermino.PROYECTISTA;
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
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable Integer id) {
        try {
            Termino termino = terminoRepository.findById(id).orElse(null);

            // Validamos que exista el término y tenga archivo
            if (termino == null || termino.getArchivoWord() == null) {
                return ResponseEntity.notFound().build();
            }

            // SEGURIDAD: Validar acceso al expediente
            Usuario usuarioActual = getUsuarioActual();
            if (!securityService.tieneAccesoLectura(usuarioActual, termino.getExpediente())) {
                return ResponseEntity.status(403).build();
            }

            // Buscamos el archivo en la carpeta "uploads/terminos"
            Path rutaArchivo = Paths.get("uploads/terminos").resolve(termino.getArchivoWord());
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                // Preparamos la respuesta para forzar la descarga
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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EstatusTermino estatus,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) Integer abogadoId) throws IOException {

        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Terminos_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);

        // --- LÓGICA DE SEGURIDAD PARA EXCEL (Igual que en index) ---
        Usuario usuarioActual = getUsuarioActual();
        Integer filtroAbogado = abogadoId; // Por defecto usa el filtro del usuario
        Integer filtroGerencia = null; // Por defecto ve todo

        // 1. Si es Abogado, SOLO exporta lo suyo
        if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
            filtroAbogado = usuarioActual.getId();
        }
        // 2. Si es Gerente/Jefe, SOLO exporta su Gerencia
        else if (usuarioActual.getRol() == RolUsuario.GERENTE || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            if (usuarioActual.getGerencia() != null) {
                filtroGerencia = usuarioActual.getGerencia().getId();
            }
        }
        // -----------------------------------------------------------

        // Ahora sí, las variables keyword, estatus y prioridad ya existen
        List<Termino> listaTerminos = terminoRepository.listarParaExcel(
                keyword, estatus, prioridad, filtroAbogado, filtroGerencia);

        TerminoExcelExporter excelExporter = new TerminoExcelExporter(listaTerminos);
        excelExporter.export(response);
    }

    @PostMapping("/eliminar")
    public String eliminar(@RequestParam("id") Integer id, RedirectAttributes redirectAttrs) {
        try {
            Usuario actor = getUsuarioActual();

            Termino termino = terminoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Término no encontrado"));

            // SECURITY ROD-13: Validar permiso de escritura en el expediente asociado
            if (!securityService.tieneAccesoEscritura(actor, termino.getExpediente())) {
                redirectAttrs.addFlashAttribute("mensaje",
                        "Acceso denegado: No tiene permisos para eliminar este término.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            // CORRECCIÓN 3: Si eres ABOGADO, NO puedes borrar. Todos los demás SÍ.
            if (actor.getRol() == RolUsuario.ABOGADO) {
                redirectAttrs.addFlashAttribute("mensaje", "⛔ Los abogados no pueden eliminar términos.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            terminoRepository.deleteById(id);
            redirectAttrs.addFlashAttribute("mensaje", "Eliminado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "No se puede eliminar (quizás tiene histórico de acuses).");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    @GetMapping("/descargar-acuse/{id}")
    public ResponseEntity<Resource> descargarAcuse(@PathVariable Integer id) {
        try {
            // Buscamos el registro histórico del acuse
            // Nota: Aquí asumimos que buscamos el ÚLTIMO acuse subido para este término
            List<TerminoPresentado> presentados = terminoPresentadoRepository.findByTerminoExpedienteId(
                    terminoRepository.findById(id).get().getExpediente().getId());

            // Filtramos para encontrar el que corresponde a este término específico (ID)
            TerminoPresentado acuse = presentados.stream()
                    .filter(p -> p.getTermino().getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (acuse == null || acuse.getAcuseDocumento() == null) {
                return ResponseEntity.notFound().build();
            }

            // SEGURIDAD: Validar acceso al expediente
            Usuario usuarioActual = getUsuarioActual();
            if (!securityService.tieneAccesoLectura(usuarioActual, acuse.getTermino().getExpediente())) {
                return ResponseEntity.status(403).build();
            }

            Path rutaArchivo = Paths.get("uploads/acuses").resolve(acuse.getAcuseDocumento());
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + recurso.getFilename() + "\"")
                        .body(recurso);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}