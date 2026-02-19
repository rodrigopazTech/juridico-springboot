package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.AuditoriaExpediente;
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
import com.juridico.sistema_juridico.service.AuditoriaService;
import com.juridico.sistema_juridico.service.SecurityService;

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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private SecurityService securityService;

    @Autowired
    private AuditoriaService auditoriaService;

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
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

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
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actor = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            if (expediente.getId() == null) {
                if (actor.getRol() != RolUsuario.DIRECCION && actor.getRol() != RolUsuario.SUBDIRECCION) {
                    redirectAttrs.addFlashAttribute("mensaje",
                            "Acceso denegado: Solo la Dirección o Subdirección pueden crear expedientes.");
                    redirectAttrs.addFlashAttribute("tipo", "error");
                    return "redirect:/expedientes";
                }

                expediente.setCreatedAt(LocalDateTime.now());
                if (expediente.getEtapaProcesal() == null) {
                    expediente.setEtapaProcesal(EtapaProcesal.TRAMITE);
                }
                expediente.setUpdatedAt(LocalDateTime.now());
                expedienteRepository.save(expediente);
                auditoriaService.registrarAccion(expediente, actor, "CREACIÓN", "Expediente registrado en el sistema.");
            } else {
                Expediente existente = expedienteRepository.findById(expediente.getId())
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expediente no encontrado"));

                if (!securityService.tieneAccesoEscritura(actor, existente)) {
                    redirectAttrs.addFlashAttribute("mensaje",
                            "Acceso denegado: No tiene permisos para modificar este expediente.");
                    redirectAttrs.addFlashAttribute("tipo", "error");
                    return "redirect:/expedientes";
                }

                existente.setNumero(expediente.getNumero());
                existente.setPrioridad(expediente.getPrioridad());
                existente.setSede(expediente.getSede());
                existente.setEtapaProcesal(expediente.getEtapaProcesal());

                if (expediente.getGerencia() != null && expediente.getGerencia().getId() != null) {
                    gerenciaRepository.findById(expediente.getGerencia().getId()).ifPresent(existente::setGerencia);
                }
                if (expediente.getMateria() != null && expediente.getMateria().getId() != null) {
                    materiaRepository.findById(expediente.getMateria().getId()).ifPresent(existente::setMateria);
                }
                if (expediente.getTipoExpediente() != null && expediente.getTipoExpediente().getId() != null) {
                    existente.setTipoExpediente(
                            tipoExpedienteRepository.findById(expediente.getTipoExpediente().getId())
                                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                            "Tipo de expediente no encontrado")));
                }
                if (expediente.getOrganoJurisdiccional() != null
                        && expediente.getOrganoJurisdiccional().getId() != null) {
                    existente.setOrganoJurisdiccional(
                            organoRepository.findById(expediente.getOrganoJurisdiccional().getId())
                                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                            "Órgano Jurisdiccional no encontrado")));
                }

                existente.setUpdatedAt(LocalDateTime.now());
                expedienteRepository.save(existente);
                auditoriaService.registrarAccion(existente, actor, "EDICIÓN",
                        "Se actualizaron los datos generales del expediente.");
            }

            redirectAttrs.addFlashAttribute("mensaje", "Expediente guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (DataIntegrityViolationException e) {
            redirectAttrs.addFlashAttribute("mensaje",
                    "Error: El número de expediente '" + expediente.getNumero() + "' ya existe.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Error inesperado: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }

        return "redirect:/expedientes";
    }

    @GetMapping("/{id}")
    public String verDetalle(@PathVariable UUID id, @RequestParam(required = false) String from, Model model,
            RedirectAttributes redirectAttrs) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Expediente expediente = expedienteRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expediente no encontrado: " + id));

        if (!securityService.tieneAccesoLectura(usuario, expediente)) {
            redirectAttrs.addFlashAttribute("mensaje", "Acceso denegado: No tiene permisos para ver este expediente.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/expedientes";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("rolActual", usuario.getRol());
        model.addAttribute("vengoDe", from);
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

        model.addAttribute("historial", auditoriaService.obtenerHistorial(id));
        model.addAttribute("activePage", "expedientes");

        return "views/expedientes/detalle";
    }

    @GetMapping("/{id}/comentarios")
    @ResponseBody
    public List<Comentario> getComentarios(@PathVariable String id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Expediente expediente = expedienteRepository.findById(UUID.fromString(id))
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expediente no encontrado: " + id));

        if (!securityService.tieneAccesoLectura(usuario, expediente)) {
            return Collections.emptyList();
        }

        return comentarioRepository.findByEntidadTipoAndEntidadIdOrderByCreatedAtDesc("EXPEDIENTE", id);
    }

    @PostMapping("/{id}/comentarios")
    @ResponseBody
    public ResponseEntity<?> addComentario(@PathVariable String id, @RequestBody Comentario nuevo) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        Expediente expediente = expedienteRepository.findById(UUID.fromString(id))
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expediente no encontrado: " + id));

        if (usuario == null || !securityService.tieneAccesoEscritura(usuario, expediente)) {
            return ResponseEntity.status(403).body("Acceso denegado");
        }

        nuevo.setEntidadTipo("EXPEDIENTE");
        nuevo.setEntidadId(id);
        nuevo.setCreatedAt(LocalDateTime.now());
        nuevo.setUsuario(usuario);
        nuevo.setUsuarioNombre(usuario != null ? usuario.getNombreCompleto() : "Sistema");

        comentarioRepository.save(nuevo);
        auditoriaService.registrarAccion(expediente, usuario, "NOTA AGREGADA",
                "Se agregó una nueva observación interna.");
        return ResponseEntity.ok(nuevo);
    }

    @PostMapping("/{id}/cambiar-etapa")
    @ResponseBody
    public ResponseEntity<?> cambiarEtapa(@PathVariable UUID id, @RequestParam EtapaProcesal etapa) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (usuario.getRol() != RolUsuario.DIRECCION) {
            return ResponseEntity.status(403).body("Acceso denegado: Solo la Dirección puede cambiar la etapa.");
        }

        Expediente exp = expedienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expediente no encontrado"));
        exp.setEtapaProcesal(etapa);
        exp.setUpdatedAt(LocalDateTime.now());
        expedienteRepository.save(exp);
        auditoriaService.registrarAccion(exp, usuario, "CAMBIO DE ETAPA", "El expediente cambió a la etapa: " + etapa);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/historial")
    @ResponseBody
    public List<AuditoriaExpediente> getHistorial(@PathVariable UUID id) {
        return auditoriaService.obtenerHistorial(id);
    }
}