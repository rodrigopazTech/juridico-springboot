package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoPresentado;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.util.TerminoExcelExporter;
import com.juridico.sistema_juridico.service.NotificacionService; // Importar


import jakarta.servlet.http.HttpServletResponse;

import com.juridico.sistema_juridico.repository.procesal.TerminoPresentadoRepository; 
import com.juridico.sistema_juridico.Entity.enums.Prioridad;

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
import java.net.MalformedURLException;
 
import java.io.IOException;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List; 
import java.util.Date;

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
    private NotificacionService notificacionService; // Inyectar

    // 1. VISTA PRINCIPAL
    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String estatus,
                        @RequestParam(required = false) Prioridad prioridad,
                        @RequestParam(required = false) Integer abogadoId) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("fechaVencimiento").ascending());
        
        Page<Termino> paginaTerminos = terminoRepository.buscarConFiltros(
                keyword, estatus, prioridad, abogadoId, pageable
        );

        model.addAttribute("terminos", paginaTerminos);
        model.addAttribute("listaAbogados", usuarioRepository.findAll());
        model.addAttribute("listaPrioridades", Prioridad.values());
        model.addAttribute("listaEstatus", new String[]{"Proyectista", "Revisión", "Gerencia", "Dirección", "Liberado", "Presentado", "Concluido"});

        // Listas para Modal
        model.addAttribute("expedientesList", expedienteRepository.findAll());
        model.addAttribute("abogados", usuarioRepository.findAll());

        // Mantener filtros
        model.addAttribute("keyword", keyword);
        model.addAttribute("paramEstatus", estatus);
        model.addAttribute("paramPrioridad", prioridad);
        model.addAttribute("paramAbogadoId", abogadoId);

        return "views/terminos/index";
    }

    // 2. GUARDAR (CON BLOQUEO DE EDICIÓN)
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Termino terminoForm,
                          @RequestParam(value = "expedienteId", required = false) UUID expedienteId,
                          RedirectAttributes redirectAttrs) {
        try {
            Termino terminoGuardar;

            if (terminoForm.getId() != null) {
                // EDICIÓN
                Termino existente = terminoRepository.findById(terminoForm.getId()).orElse(null);
                if (existente != null) {
                    
                    // --- REGLA DE NEGOCIO: BLOQUEO ---
                    String st = existente.getEstatusTermino();
                    if ("Liberado".equals(st) || "Presentado".equals(st) || "Concluido".equals(st)) {
                        redirectAttrs.addFlashAttribute("mensaje", "No se puede editar un término que ya está Liberado o Concluido.");
                        redirectAttrs.addFlashAttribute("tipo", "error");
                        return "redirect:/terminos";
                    }
                    // ---------------------------------

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
                    return "redirect:/terminos";
                }
            } else {
                // CREACIÓN
                terminoGuardar = terminoForm;
                terminoGuardar.setFechaIngreso(LocalDate.now());
                terminoGuardar.setEstatusTermino("Proyectista");
                terminoGuardar.setCreatedAt(LocalDateTime.now());
                terminoGuardar.setUpdatedAt(LocalDateTime.now());

                if (expedienteId != null) {
                    Expediente exp = expedienteRepository.findById(expedienteId).orElse(null);
                    if (exp != null) {
                        terminoGuardar.setExpediente(exp);
                        if (terminoGuardar.getPrioridad() == null) terminoGuardar.setPrioridad(exp.getPrioridad());
                    }
                }
            }

            terminoRepository.save(terminoGuardar);
            redirectAttrs.addFlashAttribute("mensaje", "Término guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }
    
    // 3. AVANZAR ESTADO
    @GetMapping("/avanzar/{id}")
    public String avanzarEstado(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Termino termino = terminoRepository.findById(id).orElse(null);
        if(termino != null) {
            String actual = termino.getEstatusTermino();
            
            // Si es Proyectista y NO ha subido archivo, no dejamos avanzar.
            if ("Proyectista".equals(actual) && termino.getArchivoWord() == null) {
                redirectAttrs.addFlashAttribute("mensaje", "Debes subir el documento (Word) antes de enviar a Revisión.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            // Si está en Presentado, NO avanza con este botón, avanza subiendo el Acuse
            if("Presentado".equals(actual)) {
                redirectAttrs.addFlashAttribute("mensaje", "Para concluir, utiliza el botón de 'Subir Acuse'.");
                redirectAttrs.addFlashAttribute("tipo", "warning");
                return "redirect:/terminos";
            }
            
            String siguiente = calcularSiguienteEstado(actual);
            termino.setEstatusTermino(siguiente);
        
            if ("Presentado".equals(siguiente)) {
                termino.setFechaPresentacion(LocalDate.now());
            }

            terminoRepository.save(termino);
            
            redirectAttrs.addFlashAttribute("mensaje", "Avanzó a: " + siguiente);
            redirectAttrs.addFlashAttribute("tipo", "success");
        }
        return "redirect:/terminos";
    }

    private String calcularSiguienteEstado(String actual) {
        if (actual == null) return "Proyectista";
        switch (actual) {
            case "Proyectista": return "Revisión";
            case "Revisión": return "Gerencia";
            case "Gerencia": return "Dirección";
            case "Dirección": return "Liberado";
            case "Liberado": return "Presentado";
            // Presentado -> Concluido se hace via Acuse
            default: return actual;
        }
    }

    // 4. SUBIR ARCHIVO WORD (Bloqueado si está Liberado)
    @PostMapping("/subir-archivo")
    public String subirArchivo(@RequestParam("id") Integer id,
                               @RequestParam("archivo") MultipartFile archivo,
                               RedirectAttributes redirectAttrs) {
        if (archivo.isEmpty()) return "redirect:/terminos";

        try {
            Termino termino = terminoRepository.findById(id).orElse(null);
            if (termino != null) {
                // BLOQUEO
                String st = termino.getEstatusTermino();
                if ("Liberado".equals(st) || "Presentado".equals(st) || "Concluido".equals(st)) {
                    redirectAttrs.addFlashAttribute("mensaje", "Documento bloqueado por estatus.");
                    redirectAttrs.addFlashAttribute("tipo", "error");
                    return "redirect:/terminos";
                }

                String carpetaDestino = "uploads/terminos/";
                Path rutaCarpeta = Paths.get(carpetaDestino);
                if (!Files.exists(rutaCarpeta)) Files.createDirectories(rutaCarpeta);

                String nombreFinal = id + "_" + archivo.getOriginalFilename();
                Files.copy(archivo.getInputStream(), rutaCarpeta.resolve(nombreFinal), StandardCopyOption.REPLACE_EXISTING);

                termino.setArchivoWord(nombreFinal);
                terminoRepository.save(termino);
                redirectAttrs.addFlashAttribute("mensaje", "Borrador actualizado.");
                redirectAttrs.addFlashAttribute("tipo", "success");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/terminos";
    }

   // 5. SUBIR ACUSE Y CONCLUIR
    @PostMapping("/subir-acuse")
    public String subirAcuse(@RequestParam("id") Integer id,
                             @RequestParam("archivoAcuse") MultipartFile archivo,
                             @RequestParam(value = "observaciones", required = false) String observaciones, // NUEVO PARÁMETRO
                             RedirectAttributes redirectAttrs) {
        
        if (archivo.isEmpty()) {
            redirectAttrs.addFlashAttribute("mensaje", "Selecciona el archivo del Acuse.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }

        try {
            Termino termino = terminoRepository.findById(id).orElse(null);
            if (termino != null && "Presentado".equals(termino.getEstatusTermino())) {
                
                // 1. Guardar archivo físico
                String carpeta = "uploads/acuses/";
                Path ruta = Paths.get(carpeta);
                if (!Files.exists(ruta)) Files.createDirectories(ruta);
                
                String nombreAcuse = "ACUSE_" + id + "_" + archivo.getOriginalFilename();
                Files.copy(archivo.getInputStream(), ruta.resolve(nombreAcuse), StandardCopyOption.REPLACE_EXISTING);

                // 2. Guardar Histórico
                TerminoPresentado presentado = TerminoPresentado.builder()
                        .termino(termino)
                        .expedienteNumero(termino.getExpediente().getNumero())
                        .fechaPresentacion(LocalDate.now())
                        .acuseDocumento(nombreAcuse)
                        .sincronizadoAt(LocalDateTime.now())
                        .build();
                
                terminoPresentadoRepository.save(presentado);

                // 3. ACTUALIZAR TÉRMINO (Estatus + Observaciones)
                termino.setEstatusTermino("Concluido");
                termino.setObservaciones(observaciones); 
                
                if (termino.getFechaPresentacion() == null) {
                    termino.setFechaPresentacion(LocalDate.now());
                }

                terminoRepository.save(termino);

               if (termino.getAbogadoResponsable() != null) {
                    notificacionService.crearNotificacion(
                        termino.getAbogadoResponsable(),
                        "Acuse Subido: " + termino.getExpediente().getNumero(),
                        "Se ha cargado el acuse del término. Estatus: Concluido.",
                        "TERMINO",
                        Prioridad.MEDIA,
                        termino.getId().toString()
                    );
                }
                
                redirectAttrs.addFlashAttribute("mensaje", "¡Término Concluido! Acuse registrado.");
                redirectAttrs.addFlashAttribute("tipo", "success");
            } else {
                redirectAttrs.addFlashAttribute("mensaje", "El término no está en estatus Presentado.");
                redirectAttrs.addFlashAttribute("tipo", "error");
            }
        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al subir acuse: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    // 6. DESCARGAR ARCHIVO WORD
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

    // 7. EXPORTAR A EXCEL
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

        // Usamos la nueva consulta que devuelve LISTA (no Page)
        List<Termino> listaTerminos = terminoRepository.listarParaExcel(keyword, estatus, prioridad, abogadoId);

        TerminoExcelExporter excelExporter = new TerminoExcelExporter(listaTerminos);
        excelExporter.export(response);
    }

    // 8. DESCARGAR ACUSE (Nuevo endpoint)
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

 @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            // Verificamos si tiene el rol (Ajusta "ROLE_DIRECCION" a como se llame en tu BD, ej: "ADMIN", "DIRECTOR")
            boolean esDireccion = auth.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_DIRECCION") || r.getAuthority().equals("Dirección"));

            if (!esDireccion) {
                redirectAttrs.addFlashAttribute("mensaje", "Acceso denegado. Solo Dirección puede eliminar términos.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/terminos";
            }

            terminoRepository.deleteById(id);
            redirectAttrs.addFlashAttribute("mensaje", "Eliminado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al eliminar: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }
}