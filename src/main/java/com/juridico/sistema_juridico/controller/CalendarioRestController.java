package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.response.calendario.EventoResponse;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.CalendarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/calendario")
public class CalendarioRestController {

    @Autowired
    private CalendarioService calendarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GerenciaRepository gerenciaRepository;

    @GetMapping("/eventos")
    public ResponseEntity<List<EventoResponse>> getEventos(
            @RequestParam(required = false, defaultValue = "todos") String tipo,
            @RequestParam(required = false, defaultValue = "todos") String gerencia,
            @RequestParam(required = false, defaultValue = "todos") String usuario,
            @RequestParam(required = false, defaultValue = "todos") String materia,
            @RequestParam(required = false, defaultValue = "todos") String estatus,
            @RequestParam(required = false, defaultValue = "todos") String prioridadId,
            @RequestParam(required = false, defaultValue = "todos") String tipoAudiencia,
            @RequestParam(required = false, defaultValue = "false") boolean misAsuntos) {

        Usuario usuarioActual = usuarioActual();

        Long gerenciaId = null;
        String gerenciaNombre = null;
        if (gerencia != null && !"todos".equals(gerencia)) {
            try {
                gerenciaId = Long.parseLong(gerencia);
            } catch (NumberFormatException e) {
                gerenciaNombre = gerencia;
            }
        }

        if (usuarioActual != null
                && (usuarioActual.getRol() == RolUsuario.GERENTE
                        || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO
                        || usuarioActual.getRol() == RolUsuario.ABOGADO)
                && usuarioActual.getGerencia() != null) {
            gerenciaId = usuarioActual.getGerencia().getId().longValue();
            gerenciaNombre = usuarioActual.getGerencia().getNombre();
        }

        Long usuarioId = null;
        if (misAsuntos && usuarioActual != null) {
            usuarioId = usuarioActual.getId().longValue();
        } else if (usuario != null && !"todos".equals(usuario)) {
            try {
                usuarioId = Long.parseLong(usuario);
            } catch (NumberFormatException e) {
                usuarioId = null;
            }
        }

        if (usuarioId != null && usuarioActual != null && !misAsuntos) {
            Set<Long> usuariosPermitidosIds = usuariosPermitidos(usuarioActual, gerenciaId).stream()
                    .map(Usuario::getId)
                    .map(Integer::longValue)
                    .collect(Collectors.toSet());
            if (!usuariosPermitidosIds.contains(usuarioId)) {
                return ResponseEntity.ok(List.of());
            }
        }

        Integer materiaId = null;
        String materiaNombre = null;
        if (materia != null && !"todos".equals(materia)) {
            try {
                materiaId = Integer.parseInt(materia);
            } catch (NumberFormatException e) {
                materiaNombre = materia;
            }
        }

        Integer priorityId = null;
        if (prioridadId != null && !"todos".equals(prioridadId)) {
            try {
                priorityId = Integer.parseInt(prioridadId);
            } catch (NumberFormatException e) {
                priorityId = null;
            }
        }

        List<EventoResponse> eventos = calendarioService.obtenerEventosCalendario(
                usuarioActual, tipo, gerenciaId, gerenciaNombre, usuarioId, materiaNombre, materiaId, estatus,
                priorityId, tipoAudiencia);

        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/gerencias")
    public ResponseEntity<List<Gerencia>> getGerencias() {
        Usuario usuarioActual = usuarioActual();

        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.ABOGADO) {
            return ResponseEntity.ok(List.of());
        }

        if (usuarioActual != null
                && (usuarioActual.getRol() == RolUsuario.GERENTE || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO)) {
            if (usuarioActual.getGerencia() != null) {
                return ResponseEntity.ok(List.of(usuarioActual.getGerencia()));
            }
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(gerenciaRepository.findAll());
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioOption>> getUsuarios(
            @RequestParam(required = false, defaultValue = "todos") String gerencia) {
        Usuario usuarioActual = usuarioActual();
        if (usuarioActual == null || usuarioActual.getRol() == RolUsuario.ABOGADO) {
            return ResponseEntity.ok(List.of());
        }

        Long gerenciaId = resolverGerenciaSolicitada(usuarioActual, gerencia);
        if (gerenciaId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<UsuarioOption> usuarios = usuariosPermitidos(usuarioActual, gerenciaId).stream()
                .map(UsuarioOption::from)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return usuarioRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    private Long resolverGerenciaSolicitada(Usuario usuarioActual, String gerencia) {
        if (usuarioActual == null) {
            return null;
        }
        if (usuarioActual.getRol() == RolUsuario.GERENTE
                || usuarioActual.getRol() == RolUsuario.JEFE_DEPTO
                || usuarioActual.getRol() == RolUsuario.ABOGADO) {
            return usuarioActual.getGerencia() != null ? usuarioActual.getGerencia().getId().longValue() : null;
        }
        if (gerencia == null || "todos".equals(gerencia)) {
            return null;
        }
        try {
            return Long.parseLong(gerencia);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<Usuario> usuariosPermitidos(Usuario usuarioActual, Long gerenciaId) {
        Long gerenciaPermitida = resolverGerenciaSolicitada(usuarioActual,
                gerenciaId == null ? "todos" : gerenciaId.toString());
        if (gerenciaPermitida == null) {
            return List.of();
        }
        return usuarioRepository.findByGerenciasIdAndRolInAndActivoTrueOrderByNombreCompletoAsc(
                gerenciaPermitida.intValue(), rolesVisiblesPara(usuarioActual.getRol()));
    }

    private List<RolUsuario> rolesVisiblesPara(RolUsuario rol) {
        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL) {
            return List.of(RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR, RolUsuario.GERENTE,
                    RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO);
        }
        if (rol == RolUsuario.DIRECTOR) {
            return List.of(RolUsuario.SUBDIRECTOR, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO);
        }
        if (rol == RolUsuario.SUBDIRECTOR) {
            return List.of(RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO);
        }
        if (rol == RolUsuario.GERENTE) {
            return List.of(RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO);
        }
        if (rol == RolUsuario.JEFE_DEPTO) {
            return List.of(RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO);
        }
        return List.of(RolUsuario.ABOGADO);
    }

    public record UsuarioOption(Integer id, String nombreCompleto, String rol) {
        public static UsuarioOption from(Usuario usuario) {
            return new UsuarioOption(usuario.getId(), usuario.getNombreCompleto(), usuario.getRol().name());
        }
    }
}
