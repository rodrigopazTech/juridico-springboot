package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoPresentado;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario; 
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoPresentadoRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.util.TerminoExcelExporter;

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
import org.springframework.security.core.Authentication;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/terminos")
public class TerminosController {

    @Autowired private TerminoRepository terminoRepository;
    @Autowired private ExpedienteRepository expedienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TerminoPresentadoRepository terminoPresentadoRepository;
    @Autowired private NotificacionService notificacionService;

   // --- MATRIZ DE PERMISOS ---
    private static final Map<String, List<RolUsuario>> PERMISOS_ETAPAS = new HashMap<>();
    static {
        PERMISOS_ETAPAS.put("Proyectista", Arrays.asList(RolUsuario.ABOGADO, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Revisión",    Arrays.asList(RolUsuario.JEFE_DEPTO, RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Gerencia",    Arrays.asList(RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Dirección",   Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Liberado",    Arrays.asList(RolUsuario.ABOGADO, RolUsuario.JEFE_DEPTO, RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        PERMISOS_ETAPAS.put("Presentado",  Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION, RolUsuario.ABOGADO));
    }


   @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String estatus,
                        @RequestParam(required = false) Prioridad prioridad,
                        @RequestParam(required = false) Integer abogadoId) { // Filtro del usuario (opcional)

        Usuario usuarioActual = getUsuarioActual();
        Pageable pageable = PageRequest.of(page, 10, Sort.by("fechaVencimiento").ascending());
        
        Integer filtroAbogadoSeguro = abogadoId; 
        Integer filtroGerenciaSeguro = null;    

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
                pageable
        );

        model.addAttribute("terminos", paginaTerminos);
        
        model.addAttribute("abogados", usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO));
        model.addAttribute("listaAbogados", usuarioRepository.findAll());
        model.addAttribute("listaPrioridades", Prioridad.values());
        model.addAttribute("listaEstatus", new String[]{"Proyectista", "Revisión", "Gerencia", "Dirección", "Liberado", "Presentado", "Concluido"});
        model.addAttribute("expedientesList", expedienteRepository.findAll());
        model.addAttribute("rolActual", usuarioActual.getRol());
        model.addAttribute("mapaPermisos", PERMISOS_ETAPAS);
        
        model.addAttribute("keyword", keyword);
        model.addAttribute("paramEstatus", estatus);
        model.addAttribute("paramPrioridad", prioridad);
        model.addAttribute("paramAbogadoId", abogadoId);

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
                if (existente == null) return "redirect:/terminos";
                
                existente.setActuacion(terminoForm.getActuacion());
                existente.setFechaVencimiento(terminoForm.getFechaVencimiento());
                existente.setAbogadoResponsable(terminoForm.getAbogadoResponsable());
                existente.setUpdatedAt(LocalDateTime.now());
                
                if (expedienteId != null) {
                     Expediente exp = expedienteRepository.findById(expedienteId).orElse(null);
                     if (exp != null) existente.setExpediente(exp);
                }
                terminoGuardar = existente;
            } else {
                esNuevo = true;
                terminoGuardar = terminoForm;
                terminoGuardar.setFechaIngreso(LocalDate.now());
                terminoGuardar.setEstatusTermino("Proyectista");
                terminoGuardar.setCreatedAt(LocalDateTime.now());
                terminoGuardar.setUpdatedAt(LocalDateTime.now());

                if (expedienteId != null) {
                    Expediente exp = expedienteRepository.findById(expedienteId).orElse(null);
                    if (exp != null) {
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
                    "El usuario " + actor.getNombreCompleto() + " te ha asignado: " + terminoGuardado.getActuacion(),
                    "TERMINO", 
                    terminoGuardado.getPrioridad(), 
                    terminoGuardado.getId().toString()
                );
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
        if(termino != null) {
            Usuario actor = getUsuarioActual();
            String etapaAnterior = termino.getEstatusTermino();

            if (!tienePermiso(etapaAnterior, actor.getRol())) {
                redirectAttrs.addFlashAttribute("mensaje", "⛔ No tienes permiso para avanzar la etapa.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }
            
            if ("Proyectista".equals(etapaAnterior) && termino.getArchivoWord() == null) {
                redirectAttrs.addFlashAttribute("mensaje", "Sube el archivo Word antes de avanzar.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            String siguienteEtapa = calcularSiguienteEstado(etapaAnterior);
            termino.setEstatusTermino(siguienteEtapa);
            if ("Presentado".equals(siguienteEtapa)) termino.setFechaPresentacion(LocalDate.now());

            terminoRepository.save(termino);
            
            notificarCambioDeEtapa(termino, etapaAnterior, siguienteEtapa, actor);

            redirectAttrs.addFlashAttribute("mensaje", "Avanzó a: " + siguienteEtapa);
            redirectAttrs.addFlashAttribute("tipo", "success");
        }
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

    // 4. SUBIR ACUSE (CON SEGURIDAD RBAC)
    @PostMapping("/subir-acuse")
    public String subirAcuse(@RequestParam("id") Integer id,
                             @RequestParam("archivoAcuse") MultipartFile archivo,
                             @RequestParam(value = "observaciones", required = false) String observaciones,
                             RedirectAttributes redirectAttrs) {
        
        Usuario actor = getUsuarioActual();
        
        // --- VALIDACIÓN DE PERMISOS (Para concluir) ---
        // Usamos la regla de 'Presentado' porque es la etapa actual
        if (!tienePermiso("Presentado", actor.getRol())) {
             redirectAttrs.addFlashAttribute("mensaje", "⛔ No tienes permiso para concluir términos.");
             redirectAttrs.addFlashAttribute("tipo", "error");
             return "redirect:/terminos";
        }
        // ----------------------------------------------

        // ... (Resto de tu lógica de subir acuse se mantiene igual) ...
        // Copia tu lógica de subir archivo, crear TerminoPresentado, etc.
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
            
            // Validamos que exista el término y tenga archivo
            if (termino == null || termino.getArchivoWord() == null) {
                return ResponseEntity.notFound().build();
            }

            // Buscamos el archivo en la carpeta "uploads/terminos"
            Path rutaArchivo = Paths.get("uploads/terminos").resolve(termino.getArchivoWord());
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                // Preparamos la respuesta para forzar la descarga
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
        Integer filtroGerencia = null;     // Por defecto ve todo

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
            keyword, estatus, prioridad, filtroAbogado, filtroGerencia
        );

        TerminoExcelExporter excelExporter = new TerminoExcelExporter(listaTerminos);
        excelExporter.export(response);
    }

   @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        try {
            Usuario actor = getUsuarioActual();
            
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
                    terminoRepository.findById(id).get().getExpediente().getId()
            );
            
            // Filtramos para encontrar el que corresponde a este término específico (ID)
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
}