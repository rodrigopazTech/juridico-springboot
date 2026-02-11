package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.dto.response.calendario.EventoResponse;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.CalendarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestParam(required = false, defaultValue = "false") boolean misAsuntos) {
        
        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioActual = null;
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            usuarioActual = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        }
        
        // Determinar gerenciaId a filtrar
        Long gerenciaId = null;
        if (gerencia != null && !"todos".equals(gerencia)) {
            try {
                gerenciaId = Long.parseLong(gerencia);
            } catch (NumberFormatException e) {
                // Ignorar si no es un número válido
            }
        }
        
        // GERENTE: forzar filtrar por su propia gerencia (el filtro está oculto)
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.GERENTE) {
            if (usuarioActual.getGerencia() != null) {
                gerenciaId = usuarioActual.getGerencia().getId().longValue();
            }
        }
        
        // Determinar usuarioId a filtrar
        Long usuarioId = null;
        if (misAsuntos && usuarioActual != null) {
            // Si misAsuntos está activo y es ABOGADO, forzar filtrar por su propio ID
            usuarioId = usuarioActual.getId().longValue();
        } else if (usuario != null && !"todos".equals(usuario)) {
            try {
                usuarioId = Long.parseLong(usuario);
            } catch (NumberFormatException e) {
                // Ignorar si no es un número válido
            }
        }
        
        List<EventoResponse> eventos = calendarioService.obtenerEventosCalendario(
                usuarioActual, tipo, gerenciaId, usuarioId);
        
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/gerencias")
    public ResponseEntity<List<Gerencia>> getGerencias() {
        // Obtener usuario autenticado para verificar permisos
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioActual = null;
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            usuarioActual = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        }
        
        // ABOGADO no puede ver el filtro de gerencias
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.ABOGADO) {
            return ResponseEntity.ok(List.of());
        }
        
        // GERENTE solo ve su propia gerencia
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.GERENTE) {
            if (usuarioActual.getGerencia() != null) {
                return ResponseEntity.ok(List.of(usuarioActual.getGerencia()));
            }
            return ResponseEntity.ok(List.of());
        }
        
        // DIRECCIÓN y SUBDIRECCIÓN ven todas las gerencias
        List<Gerencia> gerencias = gerenciaRepository.findAll();
        return ResponseEntity.ok(gerencias);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> getUsuarios() {
        // Obtener usuario autenticado para verificar permisos
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioActual = null;
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            usuarioActual = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        }
        
        // ABOGADO no puede ver el filtro de usuarios
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.ABOGADO) {
            return ResponseEntity.ok(List.of());
        }
        
        // GERENTE solo ve usuarios de su propia gerencia
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.GERENTE) {
            if (usuarioActual.getGerencia() != null) {
                List<Usuario> usuarios = usuarioRepository.findByGerenciaId(
                    usuarioActual.getGerencia().getId().longValue());
                return ResponseEntity.ok(usuarios);
            }
            return ResponseEntity.ok(List.of());
        }
        
        // DIRECCIÓN y SUBDIRECCIÓN ven todos los usuarios activos
        List<Usuario> usuarios = usuarioRepository.findAll();
        return ResponseEntity.ok(usuarios);
    }
}
