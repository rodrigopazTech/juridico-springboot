package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.service.procesal.AudienciaService;
// IMPORTS NUEVOS
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;

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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.util.AudienciaExcelExporter;

import jakarta.servlet.http.HttpServletResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.time.LocalDateTime;
import java.util.UUID;
import java.io.IOException;
import java.util.List;
import java.net.MalformedURLException;


@Controller
@RequestMapping("/audiencias")
public class AudienciasController {

    @Autowired private AudienciaRepository audienciaRepository;
    @Autowired private ExpedienteRepository expedienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AudienciaService audienciaService;
    
    // Inyectamos tus nuevos repositorios
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private TipoAudienciaRepository tipoAudienciaRepository;

    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String tipo,
                        @RequestParam(required = false) String gerencia,
                        @RequestParam(required = false) String materia,
                        @RequestParam(required = false) String estatus) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("fechaAudiencia").ascending());

        Page<Audiencia> audiencias = audienciaRepository.buscarConFiltros(
                keyword, tipo, gerencia, materia, estatus, pageable
        );

        model.addAttribute("audiencias", audiencias);
        model.addAttribute("pageTitle", "Gestión de Audiencias");
        model.addAttribute("activePage", "audiencias");

        // --- CARGAMOS LAS LISTAS DINÁMICAS PARA LOS FILTROS ---
        model.addAttribute("listaTiposAudiencia", tipoAudienciaRepository.findAll());
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findAll());
        // -----------------------------------------------------

        model.addAttribute("expedientesList", expedienteRepository.findAll());
        model.addAttribute("abogados", usuarioRepository.findAll()); 
        
        // Mantener filtros seleccionados
        model.addAttribute("keyword", keyword);
        model.addAttribute("paramTipo", tipo);
        model.addAttribute("paramGerencia", gerencia);
        model.addAttribute("paramMateria", materia);
        model.addAttribute("paramEstatus", estatus);

        return "views/audiencias/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Audiencia audiencia,
                          @RequestParam("expedienteId") UUID expedienteId, // Recibe UUID
                          @RequestParam("tipoAudienciaId") Integer tipoAudienciaId, // Recibe ID del tipo
                          RedirectAttributes redirectAttrs) {
        try {
            // 1. Vincular Expediente
            Expediente exp = expedienteRepository.findById(expedienteId)
                    .orElseThrow(() -> new RuntimeException("Expediente no encontrado"));
            audiencia.setExpediente(exp);

            // 2. Vincular Tipo de Audiencia
            TipoAudiencia tipo = tipoAudienciaRepository.findById(tipoAudienciaId)
                    .orElseThrow(() -> new RuntimeException("Tipo de audiencia no encontrado"));
            audiencia.setTipoAudiencia(tipo);

            // 3. Configurar Estatus Inicial (Si es nueva)
            if(audiencia.getId() == null) {
                audiencia.setEstatusAudiencia("PENDIENTE");
                audiencia.setCreatedAt(LocalDateTime.now());
            }
            audiencia.setUpdatedAt(LocalDateTime.now());

            // 4. Guardar
            audienciaRepository.save(audiencia);

            redirectAttrs.addFlashAttribute("mensaje", "Audiencia agendada correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Error al guardar: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/audiencias";
    }
    

    @PostMapping("/subir-acta")
    public String subirActa(@RequestParam("id") Integer id,
                            @RequestParam("archivo") MultipartFile archivo,
                            RedirectAttributes redirectAttrs) {
        try {
            audienciaService.subirActa(id, archivo);
            redirectAttrs.addFlashAttribute("mensaje", "Acta subida correctamente. Estatus actualizado.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al subir acta: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/audiencias";
    }

    // MODIFICADO: CONCLUIR (Ya no pide archivo, solo observaciones)
    @PostMapping("/concluir")
    public String concluir(@RequestParam("id") Integer id, 
                           @RequestParam("observaciones") String observaciones, 
                           RedirectAttributes redirectAttrs) {
        try {
            audienciaService.concluirAudiencia(id, observaciones);
            redirectAttrs.addFlashAttribute("mensaje", "Audiencia concluida exitosamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/audiencias";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        try {
            audienciaRepository.deleteById(id);
            redirectAttrs.addFlashAttribute("mensaje", "Eliminado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al eliminar.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/audiencias";
    }

    @GetMapping("/descargar-acta/{id}")
    public ResponseEntity<Resource> descargarActa(@PathVariable Integer id) {
        try {
            Audiencia audiencia = audienciaRepository.findById(id).orElse(null);
            
            if (audiencia == null || audiencia.getActaDocumento() == null) {
                return ResponseEntity.notFound().build();
            }

            // CORRECCIÓN AQUÍ: Usamos el nombre completo de la clase para evitar confusiones
            java.nio.file.Path rutaArchivo = java.nio.file.Paths.get("uploads/audiencias").resolve(audiencia.getActaDocumento());
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
                              @RequestParam(required = false) String tipo,
                              @RequestParam(required = false) String gerencia,
                              @RequestParam(required = false) String materia,
                              @RequestParam(required = false) String estatus) throws IOException {
        
        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Audiencias_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);

        List<Audiencia> listado = audienciaRepository.listarParaExcel(keyword, tipo, gerencia, materia, estatus);

        AudienciaExcelExporter excelExporter = new AudienciaExcelExporter(listado);
        excelExporter.export(response);
    }
}