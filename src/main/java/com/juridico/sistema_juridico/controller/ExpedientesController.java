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

    // MÉTODO INDEX ACTUALIZADO (Con Búsqueda y Paginación)
    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String keyword,
                        // Parámetros de los Filtros (Dropdowns)
                        @RequestParam(required = false) Integer gerenciaId,
                        @RequestParam(required = false) Integer materiaId,
                        @RequestParam(required = false) Integer tipoId,
                        @RequestParam(required = false) Prioridad prioridad,
                        @RequestParam(required = false) Integer abogadoId) {
        
        model.addAttribute("pageTitle", "Gestión de Expedientes");

        // 1. Cargar Listas para los Selects (Modal y Filtros)
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findAll());
        model.addAttribute("listaTipos", tipoExpedienteRepository.findAll());
        model.addAttribute("listaOrganos", organoRepository.findAll());
        model.addAttribute("listaAbogados", usuarioRepository.findAll());
        model.addAttribute("listaPrioridades", Prioridad.values());
        model.addAttribute("listaEstados", estadoRepository.findAllByOrderByNombreAsc());
        model.addAttribute("activePage", "expedientes");

        // 2. Configurar Paginación
        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());

        // 3. LLAMAR A LA CONSULTA MAESTRA
        Page<Expediente> paginaExpedientes = expedienteRepository.buscarExpedientes(
                keyword, gerenciaId, materiaId, tipoId, prioridad, abogadoId, pageable
        );

        // 4. Enviar datos a la vista
        model.addAttribute("expedientes", paginaExpedientes);
        
        // Mantener los valores seleccionados en los inputs después de recargar
        model.addAttribute("keyword", keyword);
        model.addAttribute("gerenciaId", gerenciaId); // Nota: Thymeleaf param.gerenciaId funciona, pero esto es más seguro
        model.addAttribute("pageTitle", "Gestión de Expedientes - Agenda Legal");

        return "views/expedientes/index";
    }

    // MÉTODO GUARDAR
    @PostMapping("/guardar")
    public String guardarExpediente(@ModelAttribute Expediente expediente, RedirectAttributes flash) {
        try {
            if (expediente.getId() == null) {
                expediente.setCreatedAt(LocalDateTime.now());
                expediente.setEtapaProcesal(EtapaProcesal.TRAMITE);
            }
            expediente.setUpdatedAt(LocalDateTime.now());

            expedienteRepository.save(expediente);
            
        } catch (Exception e) {
            e.printStackTrace();
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/expedientes";
    }
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable UUID id, Model model) {
        Expediente expediente = expedienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + id));

        model.addAttribute("expediente", expediente);
        
        // Si tu detalle tiene botones de editar, también necesita los catálogos:
        model.addAttribute("gerencias", gerenciaRepository.findAll());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("estados", estadoRepository.findAll());

        model.addAttribute("activePage", "expedientes");

        return "views/expedientes/detalle";
    }
}