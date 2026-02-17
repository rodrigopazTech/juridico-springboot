package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia; 
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoPresentado;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario; 
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;   
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoPresentadoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.util.TerminoExcelExporter;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ForbiddenException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletResponse;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/terminos")
public class TerminosController {

    @Autowired private TerminoRepository terminoRepository;
    @Autowired private ExpedienteRepository expedienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TerminoPresentadoRepository terminoPresentadoRepository;
    @Autowired private AudienciaRepository audienciaRepository; // NUEVA INYECCIÓN
    @Autowired private NotificacionService notificacionService;
    
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;

   // --- MATRIZ DE PERMISOS ---
    private static final Map<String, List<RolUsuario>> PERMISOS_ETAPAS = new HashMap<>();
    static {
        PERMISOS_ETAPAS.put("Proyectista", Arrays.asList(RolUsuario.ABOGADO, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Revisión",    Arrays.asList(RolUsuario.JEFE_DEPTO, RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Gerencia",    Arrays.asList(RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Dirección",   Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Liberado",    Arrays.asList(RolUsuario.ABOGADO, RolUsuario.JEFE_DEPTO, RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Presentado",  Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION, RolUsuario.ABOGADO, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO));
    }

    private static final Map<String, Integer> ORDEN_ETAPAS = new HashMap<>();
    static {
        ORDEN_ETAPAS.put("Proyectista", 1);
        ORDEN_ETAPAS.put("Revisión", 2);
        ORDEN_ETAPAS.put("Gerencia", 3);
        ORDEN_ETAPAS.put("Dirección", 4);
        ORDEN_ETAPAS.put("Liberado", 5);
        ORDEN_ETAPAS.put("Presentado", 6);
        ORDEN_ETAPAS.put("Concluido", 7);
    }
    
   @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String estatus,
                        @RequestParam(required = false) Prioridad prioridad,
                        @RequestParam(required = false) Integer abogadoId,
                        @RequestParam(required = false) Integer gerenciaId,
                        @RequestParam(required = false) Integer materiaId) {

        // --- 1. LIMPIEZA DE URL (Fix Notificaciones) ---
        if (keyword != null && keyword.contains("?keyword=")) {
            keyword = keyword.substring(keyword.lastIndexOf("=") + 1);
        }

        Usuario usuarioActual = getUsuarioActual();
        Pageable pageable = PageRequest.of(page, 10, Sort.by("fechaVencimiento").ascending());
        
        // --- 2. FILTROS DE SEGURIDAD (TABLA) ---
        Integer filtroAbogadoSeguro = abogadoId; 
        Integer filtroGerenciaSeguro = gerenciaId;    

        if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
            filtroAbogadoSeguro = usuarioActual.getId();
        } 
        else if (usuarioActual.getRol() == RolUsuario.GERENTE || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            if (usuarioActual.getGerencia() != null) {
                filtroGerenciaSeguro = usuarioActual.getGerencia().getId();
            }
        }

        Page<Termino> paginaTerminos = terminoRepository.buscarConFiltros(
                keyword, estatus, prioridad, 
                filtroAbogadoSeguro,  
                filtroGerenciaSeguro, 
                materiaId,
                pageable
        );

        model.addAttribute("terminos", paginaTerminos);
        
        // --- 3. FILTRO DE ABOGADOS PARA EL MODAL (SEGURIDAD) ---
        List<Usuario> abogadosParaModal;
        if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
            abogadosParaModal = Collections.singletonList(usuarioActual);
        } else if (usuarioActual.getRol() == RolUsuario.GERENTE || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            abogadosParaModal = usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO).stream()
                .filter(u -> u.getGerencia() != null && usuarioActual.getGerencia() != null && 
                             u.getGerencia().getId().equals(usuarioActual.getGerencia().getId()))
                .collect(Collectors.toList());
        } else {
            abogadosParaModal = usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO);
        }
        model.addAttribute("abogados", abogadosParaModal); 

        // --- 4. LISTAS PARA FILTROS GENERALES ---
        model.addAttribute("listaAbogados", usuarioRepository.findAll());
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findAll());
        model.addAttribute("listaPrioridades", Prioridad.values());
        model.addAttribute("listaEstatus", new String[]{"Proyectista", "Revisión", "Gerencia", "Dirección", "Liberado", "Presentado", "Concluido"});
        
        // --- 5. LOGICA ANTI-FANTASMAS (EXPEDIENTES DISPONIBLES) ---
        List<Expediente> expedientesParaDropdown;

        if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
            // A) Expedientes donde soy el Dueño
            List<Expediente> misExpedientes = expedienteRepository.findAll().stream()
                .filter(e -> e.getAbogadoResponsable() != null && e.getAbogadoResponsable().getId().equals(usuarioActual.getId()))
                .collect(Collectors.toList());

            // B) Expedientes donde soy Invitado ACTIVO (Audiencia NO Concluida)
            List<Expediente> expedientesPorAudiencia = audienciaRepository.findAll().stream()
                .filter(a -> a.getAbogadoComparece() != null && 
                             a.getAbogadoComparece().getId().equals(usuarioActual.getId()) &&
                             !a.getEstatusAudiencia().equals("CONCLUIDA")) 
                .map(Audiencia::getExpediente)
                .collect(Collectors.toList());

            // Unir listas sin duplicados
            Set<Expediente> conjunto = new HashSet<>(misExpedientes);
            conjunto.addAll(expedientesPorAudiencia);
            expedientesParaDropdown = new ArrayList<>(conjunto);

        } else if (usuarioActual.getRol() == RolUsuario.GERENTE) {
            expedientesParaDropdown = expedienteRepository.findAll().stream()
                .filter(e -> e.getGerencia() != null && usuarioActual.getGerencia() != null && 
                             e.getGerencia().getId().equals(usuarioActual.getGerencia().getId()))
                .collect(Collectors.toList());
        } else if (usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            expedientesParaDropdown = expedienteRepository.findAll().stream()
                .filter(e -> e.getMateria() != null && usuarioActual.getMaterias().stream()
                             .anyMatch(m -> m.getId().equals(e.getMateria().getId())))
                .collect(Collectors.toList());
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
        model.addAttribute("paramPrioridad", prioridad);
        model.addAttribute("paramAbogadoId", abogadoId);
        model.addAttribute("paramGerenciaId", gerenciaId);
        model.addAttribute("paramMateriaId", materiaId);

        model.addAttribute("activePage", "terminos");

        return "views/terminos/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Termino terminoForm,
                          @RequestParam(value = "expedienteId", required = false) UUID expedienteId,
                          @RequestParam(value = "abogadoId", required = false) Integer abogadoId, 
                          RedirectAttributes redirectAttrs) {
        
        Usuario actor = getUsuarioActual();
        Termino terminoGuardar;
        boolean esNuevo = false;

        // 1. VINCULAR EXPEDIENTE (Si viene el ID)
        if (expedienteId != null) {
             Expediente exp = expedienteRepository.findById(expedienteId)
                 .orElseThrow(() -> new ResourceNotFoundException("El expediente asociado no existe."));
             terminoForm.setExpediente(exp);
        }

        // 2. VINCULAR ABOGADO (Si viene el ID)
        if (abogadoId != null) {
            Usuario abogado = usuarioRepository.findById(abogadoId)
                .orElseThrow(() -> new ResourceNotFoundException("El abogado asignado no existe."));
            terminoForm.setAbogadoResponsable(abogado);
        }

        if (terminoForm.getId() != null) {
            // EDICIÓN
            Termino existente = terminoRepository.findById(terminoForm.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("El término que intentas editar no existe."));
            
            // Actualizamos solo los campos editables
            existente.setActuacion(terminoForm.getActuacion());
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

            existente.setUpdatedAt(LocalDateTime.now());
            terminoGuardar = existente;
        } else {
            // CREACIÓN
            esNuevo = true;
            terminoGuardar = terminoForm;
            terminoGuardar.setFechaIngreso(LocalDate.now());
            terminoGuardar.setEstatusTermino("Proyectista");
            terminoGuardar.setCreatedAt(LocalDateTime.now());
            terminoGuardar.setUpdatedAt(LocalDateTime.now());
            
            // Si no se seleccionó abogado explícito, usar el del expediente
            if (terminoGuardar.getAbogadoResponsable() == null && terminoGuardar.getExpediente() != null) {
                 terminoGuardar.setAbogadoResponsable(terminoGuardar.getExpediente().getAbogadoResponsable());
            }
            // Si no se seleccionó prioridad, usar la del expediente
            if (terminoGuardar.getPrioridad() == null && terminoGuardar.getExpediente() != null) {
                 terminoGuardar.setPrioridad(terminoGuardar.getExpediente().getPrioridad());
            }
        }

        Termino terminoGuardado = terminoRepository.save(terminoGuardar);

        // Notificación
        if (esNuevo && terminoGuardado.getAbogadoResponsable() != null) {
            notificacionService.crearNotificacion(
                terminoGuardado.getAbogadoResponsable(),
                "Nuevo Término Asignado",
                "El usuario " + actor.getNombreCompleto() + " te ha asignado: " + terminoGuardado.getActuacion(),
                "TERMINO", 
                terminoGuardado.getPrioridad(), 
                terminoGuardado.getId().toString()
            );
        }

        redirectAttrs.addFlashAttribute("mensaje", "Término guardado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/terminos";
    }
    
    @GetMapping("/avanzar/{id}")
    public String avanzarEstado(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        // 1. BUSCAR (O LANZAR ERROR 404)
        Termino termino = terminoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Término no encontrado."));
        
        Usuario actor = getUsuarioActual();
        String etapaAnterior = termino.getEstatusTermino();

        // 2. VALIDAR PERMISOS (O LANZAR ERROR 403)
        if (!tienePermiso(etapaAnterior, actor.getRol())) {
            throw new ForbiddenException("No tienes permiso para avanzar términos en etapa: " + etapaAnterior);
        }
        
        // 3. VALIDAR REGLAS DE NEGOCIO (O LANZAR ERROR 400)
        if ("Proyectista".equals(etapaAnterior) && termino.getArchivoWord() == null) {
            throw new BadRequestException("Debes subir el archivo Word antes de enviar a Revisión.");
        }

        // 4. LÓGICA FELIZ
        String siguienteEtapa = calcularSiguienteEstado(etapaAnterior);
        termino.setEstatusTermino(siguienteEtapa);
        if ("Presentado".equals(siguienteEtapa)) termino.setFechaPresentacion(LocalDate.now());

        terminoRepository.save(termino);
        notificarCambioDeEtapa(termino, etapaAnterior, siguienteEtapa, actor);

        redirectAttrs.addFlashAttribute("mensaje", "Avanzó a: " + siguienteEtapa);
        redirectAttrs.addFlashAttribute("tipo", "success");
        
        return "redirect:/terminos";
    }

    private void notificarCambioDeEtapa(Termino termino, String etapaAnterior, String nuevaEtapa, Usuario actor) {
        String titulo = "Término en " + nuevaEtapa;
        String mensajeBase = String.format("Movimiento de '%s' a '%s' por %s", etapaAnterior, nuevaEtapa, actor.getNombreCompleto());
        
        List<Usuario> destinatarios = new ArrayList<>();
        Gerencia g = termino.getExpediente().getGerencia();

        switch (nuevaEtapa) {
            case "Revisión": destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.JEFE_DEPTO, g)); break;
            case "Gerencia": destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.GERENTE, g)); break;
            case "Dirección": destinatarios.addAll(usuarioRepository.findByRolInAndActivoTrue(Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION))); break;
            case "Liberado": 
                if (termino.getAbogadoResponsable() != null) destinatarios.add(termino.getAbogadoResponsable());
                destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.JEFE_DEPTO, g));
                break;
            case "Presentado": destinatarios.addAll(usuarioRepository.findByRolInAndActivoTrue(Arrays.asList(RolUsuario.DIRECCION))); break;
        }

        Set<Usuario> unicos = new HashSet<>(destinatarios);
        for (Usuario u : unicos) {
            if (!u.getId().equals(actor.getId())) {
                notificacionService.crearNotificacion(u, titulo, mensajeBase, "TERMINO", Prioridad.ALTA, termino.getId().toString());
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
                
                // --- CANDADO 1: VERIFICAR PERMISOS POR ROL Y ESTATUS ---
                Usuario actor = getUsuarioActual();
                if (!tienePermiso(termino.getEstatusTermino(), actor.getRol())) {
                    redirectAttrs.addFlashAttribute("mensaje", "⛔ No tienes permiso para modificar archivos en la etapa: " + termino.getEstatusTermino());
                    redirectAttrs.addFlashAttribute("tipo", "error");
                    return "redirect:/terminos";
                }

                // --- CANDADO 2: ETAPAS FINALES BLOQUEADAS ---
                String st = termino.getEstatusTermino();
                if ("Liberado".equals(st) || "Presentado".equals(st) || "Concluido".equals(st)) {
                    redirectAttrs.addFlashAttribute("mensaje", "El documento está bloqueado en esta etapa.");
                    redirectAttrs.addFlashAttribute("tipo", "error");
                    return "redirect:/terminos";
                }

                String carpetaDestino = "uploads/terminos/";
                Path rutaCarpeta = Paths.get(carpetaDestino);
                if (!Files.exists(rutaCarpeta)) Files.createDirectories(rutaCarpeta);

                String nombreFinal = id + "_" + archivo.getOriginalFilename();
                Files.copy(archivo.getInputStream(), rutaCarpeta.resolve(nombreFinal), StandardCopyOption.REPLACE_EXISTING);

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

    @PostMapping("/subir-acuse")
    public String subirAcuse(@RequestParam("id") Integer id,
                             @RequestParam("archivoAcuse") MultipartFile archivo,
                             @RequestParam(value = "observaciones", required = false) String observaciones,
                             RedirectAttributes redirectAttrs) {
        
        Usuario actor = getUsuarioActual();
        
        if (!tienePermiso("Presentado", actor.getRol())) {
             redirectAttrs.addFlashAttribute("mensaje", "No tienes permiso para concluir términos.");
             redirectAttrs.addFlashAttribute("tipo", "error");
             return "redirect:/terminos";
        }

         try {
            Termino termino = terminoRepository.findById(id).orElse(null);
            if (termino != null && "Presentado".equals(termino.getEstatusTermino())) {
                String carpeta = "uploads/acuses/";
                Path ruta = Paths.get(carpeta);
                if (!Files.exists(ruta)) Files.createDirectories(ruta);
                String nombreAcuse = "ACUSE_" + id + "_" + archivo.getOriginalFilename();
                Files.copy(archivo.getInputStream(), ruta.resolve(nombreAcuse), StandardCopyOption.REPLACE_EXISTING);

                TerminoPresentado presentado = TerminoPresentado.builder()
                        .termino(termino)
                        .expedienteNumero(termino.getExpediente().getNumero())
                        .fechaPresentacion(LocalDate.now())
                        .acuseDocumento(nombreAcuse)
                        .sincronizadoAt(LocalDateTime.now()).build();
                terminoPresentadoRepository.save(presentado);

                termino.setEstatusTermino("Concluido");
                termino.setObservaciones(observaciones);
                if (termino.getFechaPresentacion() == null) termino.setFechaPresentacion(LocalDate.now());
                terminoRepository.save(termino);

                redirectAttrs.addFlashAttribute("mensaje", "¡Término Concluido!");
                redirectAttrs.addFlashAttribute("tipo", "success");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/terminos";
    }

    private boolean tienePermiso(String etapaActual, RolUsuario rolUsuario) {
        if (etapaActual == null) return false;
        List<RolUsuario> permitidos = PERMISOS_ETAPAS.get(etapaActual);
        return permitidos != null && permitidos.contains(rolUsuario);
    }

    private String calcularSiguienteEstado(String actual) {
        if (actual == null) return "Proyectista";
        switch (actual) {
            case "Proyectista": return "Revisión";
            case "Revisión": return "Gerencia";
            case "Gerencia": return "Dirección";
            case "Dirección": return "Liberado";
            case "Liberado": return "Presentado";
            default: return actual;
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
            
            if (termino == null || termino.getArchivoWord() == null) {
                return ResponseEntity.notFound().build();
            }

            Path rutaArchivo = Paths.get("uploads/terminos").resolve(termino.getArchivoWord());
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"")
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
                              @RequestParam(required = false) String estatus,
                              @RequestParam(required = false) Prioridad prioridad,
                              @RequestParam(required = false) Integer abogadoId,
                              @RequestParam(required = false) Integer gerenciaId,
                              @RequestParam(required = false) Integer materiaId) throws IOException {
        
        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Terminos_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);
        
        Usuario usuarioActual = getUsuarioActual();
        Integer filtroAbogado = abogadoId; 
        Integer filtroGerencia = gerenciaId;

        if (usuarioActual.getRol() == RolUsuario.ABOGADO) {
            filtroAbogado = usuarioActual.getId();
        } 
        else if (usuarioActual.getRol() == RolUsuario.GERENTE || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            if (usuarioActual.getGerencia() != null) {
                filtroGerencia = usuarioActual.getGerencia().getId();
            }
        }
        
        List<Termino> listaTerminos = terminoRepository.listarParaExcel(
            keyword, estatus, prioridad, filtroAbogado, filtroGerencia, materiaId
        );

        TerminoExcelExporter excelExporter = new TerminoExcelExporter(listaTerminos);
        excelExporter.export(response);
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Usuario actor = getUsuarioActual();
        
        // VALIDACIÓN ROL
        if (actor.getRol() == RolUsuario.ABOGADO) {
            throw new ForbiddenException("Los abogados no tienen permiso para eliminar términos.");
        }
        
        // VALIDACIÓN EXISTENCIA
        if (!terminoRepository.existsById(id)) {
             throw new ResourceNotFoundException("El término a eliminar no existe.");
        }

        // ELIMINACIÓN (Si falla por FK, lo atrapa el GlobalHandler)
        terminoRepository.deleteById(id);
        
        redirectAttrs.addFlashAttribute("mensaje", "Término eliminado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/terminos";
    }

    @GetMapping("/descargar-acuse/{id}")
    public ResponseEntity<Resource> descargarAcuse(@PathVariable Integer id) {
        try {
            List<TerminoPresentado> presentados = terminoPresentadoRepository.findByTerminoExpedienteId(
                    terminoRepository.findById(id).get().getExpediente().getId()
            );
            
            TerminoPresentado acuse = presentados.stream()
                    .filter(p -> p.getTermino().getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (acuse == null || acuse.getAcuseDocumento() == null) {
                return ResponseEntity.notFound().build();
            }

            Path rutaArchivo = Paths.get("uploads/acuses").resolve(acuse.getAcuseDocumento());
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"")
                        .body(recurso);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // --- API: OBTENER ABOGADO DEL EXPEDIENTE (PARA VINCULACIÓN AUTOMÁTICA) ---
    @GetMapping("/api/responsable-expediente/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerResponsable(@PathVariable String id) {
        try {
            // Buscamos el expediente por su UUID
            Expediente exp = expedienteRepository.findById(UUID.fromString(id))
                    .orElseThrow(() -> new RuntimeException("Expediente no encontrado"));
            
            Usuario responsable = exp.getAbogadoResponsable();
            
            Map<String, Object> respuesta = new HashMap<>();
            if (responsable != null) {
                respuesta.put("id", responsable.getId());
                respuesta.put("nombre", responsable.getNombreCompleto());
            } else {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}