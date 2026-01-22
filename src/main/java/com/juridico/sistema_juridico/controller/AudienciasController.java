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

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;

import java.time.LocalDateTime;
import java.util.UUID;

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
    
    @PostMapping("/concluir")
    public String concluir(@RequestParam("id") Integer id, @RequestParam("observaciones") String observaciones, @RequestParam("archivoActa") MultipartFile archivo, RedirectAttributes redirectAttrs) {
        try {
            audienciaService.concluirAudiencia(id, observaciones, archivo);
            redirectAttrs.addFlashAttribute("mensaje", "Audiencia concluida.");
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
}