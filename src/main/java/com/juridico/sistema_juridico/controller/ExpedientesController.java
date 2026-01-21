package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.repository.Catalogo.EstadoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoExpedienteRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/expedientes")
public class ExpedientesController {

    @Autowired private ExpedienteRepository expedienteRepository;
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private TipoExpedienteRepository tipoExpedienteRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstadoRepository estadoRepository;

    // MÉTODO INDEX ACTUALIZADO (Con nombres de variables corregidos)
    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String keyword,
                        // Parámetros de los Filtros
                        @RequestParam(required = false) Integer gerenciaId,
                        @RequestParam(required = false) Integer materiaId,
                        @RequestParam(required = false) Integer tipoId,
                        @RequestParam(required = false) Prioridad prioridad,
                        @RequestParam(required = false) Integer abogadoId) {
        
        // 1. Configurar Paginación
        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());

        // 2. LLAMAR A LA CONSULTA MAESTRA
        Page<Expediente> paginaExpedientes = expedienteRepository.buscarExpedientes(
                keyword, 
                gerenciaId, 
                materiaId, 
                tipoId, 
                prioridad, 
                abogadoId, 
                pageable
        );

        model.addAttribute("expedientes", paginaExpedientes);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Gestión de Expedientes - Agenda Legal");
        model.addAttribute("activePage", "expedientes");

        // 3. Cargar Listas para los Selects (CORREGIDO A MINÚSCULAS para coincidir con el Modal)
        model.addAttribute("gerencias", gerenciaRepository.findAll());
        model.addAttribute("materias", materiaRepository.findAll());
        model.addAttribute("tipos", tipoExpedienteRepository.findAll());
        model.addAttribute("organos", organoRepository.findAll());
        // En tu modal usas 'usuarios' para los abogados, así que lo mandamos como 'usuarios'
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("prioridades", Prioridad.values());
        model.addAttribute("estados", estadoRepository.findAllByOrderByNombreAsc());

        return "views/expedientes/index";
    }

    // MÉTODO GUARDAR (Restaurada la protección contra duplicados)
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Expediente expediente, RedirectAttributes redirectAttrs) {
        try {
            if (expediente.getId() == null) {
                expediente.setCreatedAt(LocalDateTime.now());
                expediente.setEtapaProcesal(EtapaProcesal.TRAMITE);
            }
            expediente.setUpdatedAt(LocalDateTime.now());

            expedienteRepository.save(expediente);
            
            redirectAttrs.addFlashAttribute("mensaje", "Expediente guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
            
        } catch (DataIntegrityViolationException e) {
            // Error específico de duplicado
            redirectAttrs.addFlashAttribute("mensaje", "Error: El número de expediente '" + expediente.getNumero() + "' ya existe.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Ocurrió un error inesperado al guardar.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        
        return "redirect:/expedientes";
    }

    @GetMapping("/{id}")
    public String verDetalle(@PathVariable UUID id, Model model) {
        Expediente expediente = expedienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + id));

        model.addAttribute("expediente", expediente);
        
        // Listas necesarias para modales de edición en el detalle
        model.addAttribute("gerencias", gerenciaRepository.findAll());
        model.addAttribute("materias", materiaRepository.findAll());
        model.addAttribute("tipos", tipoExpedienteRepository.findAll());
        model.addAttribute("organos", organoRepository.findAll());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("prioridades", Prioridad.values());
        model.addAttribute("estados", estadoRepository.findAll());

        model.addAttribute("activePage", "expedientes");

        return "views/expedientes/detalle";
    }
}