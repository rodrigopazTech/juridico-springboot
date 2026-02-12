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
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

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
    @Autowired private AudienciaRepository audienciaRepository;
    @Autowired private ComentarioRepository comentarioRepository;
    
    // INYECCIÓN DEL SERVICIO DE NOTIFICACIONES
    @Autowired private NotificacionService notificacionService;

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
            // Ven todo
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
        model.addAttribute("usuarioActual", usuario);

        return "views/expedientes/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Expediente expediente, RedirectAttributes redirectAttrs) {
        // Mantenemos este try-catch SOLO para la duplicidad, que es un error muy específico de negocio
        try {
            boolean esNuevo = (expediente.getId() == null);
            
            if (esNuevo) {
                expediente.setCreatedAt(LocalDateTime.now());
                expediente.setEtapaProcesal(EtapaProcesal.TRAMITE);
            } else {
                // Verificar que existe antes de guardar (opcional, save lo maneja, pero es bueno validar)
                if (!expedienteRepository.existsById(expediente.getId())) {
                    throw new ResourceNotFoundException("El expediente a editar no existe.");
                }
            }
            expediente.setUpdatedAt(LocalDateTime.now());

            Expediente guardado = expedienteRepository.save(expediente);

            if (esNuevo) {
                enviarNotificacionesNuevoExpediente(guardado);
            }

            redirectAttrs.addFlashAttribute("mensaje", "Expediente guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (DataIntegrityViolationException e) {
            // Este catch específico es útil para dar feedback exacto
            redirectAttrs.addFlashAttribute("mensaje", "Error: El número de expediente '" + expediente.getNumero() + "' ya existe.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        } 
        // Eliminamos el 'catch (Exception e)' general. El GlobalHandler lo hará.

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

        // A) Abogado Responsable (Verificamos que solo traiga ID y buscamos el objeto completo si es necesario, 
        // aunque para notificar solo necesitamos el objeto usuario, hibernate lo maneja bien si el ID es correcto)
        if (exp.getAbogadoResponsable() != null) {
            destinatarios.add(exp.getAbogadoResponsable());
        }

        // B) Gerente de la Gerencia asignada
        if (exp.getGerencia() != null) {
            List<Usuario> gerentes = usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.GERENTE, exp.getGerencia());
            destinatarios.addAll(gerentes);
        }

        // C) Jefe de Depto (Que tenga la Materia asignada)
        if (exp.getMateria() != null) {
            List<Usuario> jefes = usuarioRepository.findByRolAndMateriaId(RolUsuario.JEFE_DEPTO, exp.getMateria().getId());
            destinatarios.addAll(jefes);
        }

        // Enviar notificaciones
        // Usamos un Set para evitar duplicados si una persona tuviera doble rol (raro pero posible)
        for (Usuario u : new HashSet<>(destinatarios)) {
            notificacionService.crearNotificacion(
                u,
                titulo,
                mensaje,
                "EXPEDIENTE", 
                exp.getPrioridad(),
                exp.getId().toString() 
            );
        }
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

    // --- API PARA SELECTORES EN CASCADA (NUEVO EXPEDIENTE) ---

    @GetMapping("/api/abogados")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerAbogadosPorMateria(
            @RequestParam Integer gerenciaId, 
            @RequestParam Integer materiaId) {
        
        // 1. Buscamos abogados que tengan esa materia asignada
        List<Usuario> abogados = usuarioRepository.findByRolAndMateriaId(RolUsuario.ABOGADO, materiaId);
        
        // 2. Filtramos para asegurar que pertenezcan a la gerencia correcta y preparamos el JSON
        List<Map<String, Object>> response = abogados.stream()
            .filter(u -> u.getGerencia() != null && u.getGerencia().getId().equals(gerenciaId))
            .map(u -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", u.getId());
                map.put("nombreCompleto", u.getNombreCompleto());
                return map;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(response);
    }
}