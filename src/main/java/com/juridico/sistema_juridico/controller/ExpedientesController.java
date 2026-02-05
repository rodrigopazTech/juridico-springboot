package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.EstadoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoExpedienteRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.General.ComentarioRepository;
import com.juridico.sistema_juridico.Entity.general.Comentario;
import org.springframework.http.ResponseEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private TipoExpedienteRepository tipoExpedienteRepository;
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

    @GetMapping
    public String index(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) Integer tipoId,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) EtapaProcesal etapa,
            @RequestParam(required = false) Integer abogadoId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        Integer secGerenciaId = null;
        List<Integer> secMateriaIds = null;
        Integer secUsuarioId = usuario.getId();

        if (usuario.getRol() == RolUsuario.DIRECCION || usuario.getRol() == RolUsuario.SUBDIRECCION) {
        } else if (usuario.getRol() == RolUsuario.GERENTE) {
            if (usuario.getGerencia() != null)
                secGerenciaId = usuario.getGerencia().getId();
        } else {
            if (usuario.getGerencia() != null)
                secGerenciaId = usuario.getGerencia().getId();
            if (usuario.getMaterias() != null && !usuario.getMaterias().isEmpty()) {
                secMateriaIds = usuario.getMaterias().stream().map(m -> m.getId()).collect(Collectors.toList());
            } else {
                secMateriaIds = Collections.emptyList();
            }
        }

        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());

        Page<Expediente> paginaExpedientes = expedienteRepository.buscarConSeguridad(
                keyword, gerenciaId, materiaId, tipoId, prioridad, etapa, abogadoId,
                secGerenciaId, secMateriaIds, secUsuarioId,
                pageable);

        model.addAttribute("expedientes", paginaExpedientes);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Gestión de Expedientes");
        model.addAttribute("activePage", "expedientes");

        model.addAttribute("rolActual", usuario.getRol());

        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findAll());
        model.addAttribute("listaTipos", tipoExpedienteRepository.findAll());
        model.addAttribute("listaPrioridades", Prioridad.values());
        model.addAttribute("listaEtapas", EtapaProcesal.values());
        model.addAttribute("listaAbogados", usuarioRepository.findAll());

        model.addAttribute("gerencias", gerenciaRepository.findAll());
        model.addAttribute("materias", materiaRepository.findAll());
        model.addAttribute("tipos", tipoExpedienteRepository.findAll());
        model.addAttribute("organos", organoRepository.findAll());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("prioridades", Prioridad.values());
        model.addAttribute("estados", estadoRepository.findAllByOrderByNombreAsc());

        return "views/expedientes/index";
    }

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

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        model.addAttribute("usuario", usuario);
        model.addAttribute("rolActual", usuario.getRol());

        Expediente expediente = expedienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + id));

        model.addAttribute("expediente", expediente);

        Audiencia proxima = audienciaRepository.findTopByExpedienteIdAndFechaAudienciaAfterOrderByFechaAudienciaAsc(
                id,
                LocalDate.now()).orElse(null);
        model.addAttribute("proximaAudiencia", proxima);

        model.addAttribute("gerencias", gerenciaRepository.findAll());
        model.addAttribute("materias", materiaRepository.findAll());
        model.addAttribute("tipos", tipoExpedienteRepository.findAll());
        model.addAttribute("organos", organoRepository.findAll());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("prioridades", Prioridad.values());
        model.addAttribute("estados", estadoRepository.findAll());
        model.addAttribute("etapas", EtapaProcesal.values());

        model.addAttribute("activePage", "expedientes");

        return "views/expedientes/detalle";
    }

    // --- ENDPOINTS PARA COMENTARIOS (NOTAS) ---

    @GetMapping("/{id}/comentarios")
    @ResponseBody
    public List<Comentario> getComentarios(@PathVariable String id) {
        return comentarioRepository.findByEntidadTipoAndEntidadIdOrderByCreatedAtDesc("EXPEDIENTE", id);
    }

    @PostMapping("/{id}/comentarios")
    @ResponseBody
    public ResponseEntity<?> addComentario(@PathVariable String id, @RequestBody Comentario nuevo) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        nuevo.setEntidadTipo("EXPEDIENTE");
        nuevo.setEntidadId(id);
        nuevo.setCreatedAt(LocalDateTime.now());
        nuevo.setUsuario(usuario);
        nuevo.setUsuarioNombre(usuario != null ? usuario.getNombreCompleto() : "Sistema");

        comentarioRepository.save(nuevo);
        return ResponseEntity.ok(nuevo);
    }

    // --- ACTUALIZACIÓN DE ESTADO (ETAPA) ---

    @PostMapping("/{id}/cambiar-etapa")
    @ResponseBody
    public ResponseEntity<?> cambiarEtapa(@PathVariable UUID id, @RequestParam EtapaProcesal etapa) {
        Expediente exp = expedienteRepository.findById(id).orElseThrow();
        exp.setEtapaProcesal(etapa);
        exp.setUpdatedAt(LocalDateTime.now());
        expedienteRepository.save(exp);
        return ResponseEntity.ok().build();
    }
}