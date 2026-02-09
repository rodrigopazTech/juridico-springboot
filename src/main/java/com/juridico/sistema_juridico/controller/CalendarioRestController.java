package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.dto.response.calendario.EventoResponse;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
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

    @GetMapping("/eventos")
    public ResponseEntity<List<EventoResponse>> getEventos(
            @RequestParam(required = false, defaultValue = "todos") String tipo,
            @RequestParam(required = false) Long gerenciaId,
            @RequestParam(required = false) Long usuarioId) {
        
        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioActual = null;
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            usuarioActual = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        }
        
        // Si es abogado, solo ve sus propios eventos
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.ABOGADO) {
            usuarioId = usuarioActual.getId().longValue();
            gerenciaId = null; // No puede filtrar por gerencia
        }
        
        // Si es gerente, solo ve su gerencia
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.GERENTE) {
            if (usuarioActual.getGerencia() != null) {
                gerenciaId = usuarioActual.getGerencia().getId().longValue();
            }
            // No puede filtrar por usuario de otras gerencias
            if (usuarioId != null) {
                Usuario usuarioFiltrado = usuarioRepository.findById(usuarioId.intValue()).orElse(null);
                if (usuarioFiltrado != null && usuarioFiltrado.getGerencia() != null &&
                    !usuarioFiltrado.getGerencia().getId().equals(usuarioActual.getGerencia().getId())) {
                    usuarioId = null; // Resetear si intenta ver usuario de otra gerencia
                }
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
        
        // ABOGADO y GERENTE no pueden ver el filtro de gerencias
        if (usuarioActual != null && 
            (usuarioActual.getRol() == RolUsuario.ABOGADO || 
             usuarioActual.getRol() == RolUsuario.GERENTE)) {
            return ResponseEntity.ok(List.of());
        }
        
        // Retornar lista de gerencias
        List<Gerencia> gerencias = calendarioService.obtenerGerencias();
        return ResponseEntity.ok(gerencias);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> getUsuarios(@RequestParam(required = false) Long gerenciaId) {
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
        
        // GERENTE solo ve usuarios de su gerencia
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.GERENTE) {
            if (usuarioActual.getGerencia() != null) {
                gerenciaId = usuarioActual.getGerencia().getId().longValue();
            }
            List<Usuario> usuarios = usuarioRepository.findByGerenciaId(gerenciaId);
            return ResponseEntity.ok(usuarios);
        }
        
        // DIRECCIÓN y SUBDIRECCIÓN ven todos los usuarios
        if (usuarioActual != null && 
            (usuarioActual.getRol() == RolUsuario.DIRECCION || 
             usuarioActual.getRol() == RolUsuario.SUBDIRECCION)) {
            if (gerenciaId != null && gerenciaId > 0) {
                return ResponseEntity.ok(usuarioRepository.findByGerenciaId(gerenciaId));
            }
            return ResponseEntity.ok(usuarioRepository.findAll());
        }
        
        return ResponseEntity.ok(List.of());
    }

    /**
     * Obtiene información del usuario actual para el frontend
     */
    @GetMapping("/usuario-actual")
    public ResponseEntity<UsuarioInfo> getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.ok(null);
        }
        
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        
        if (usuario != null) {
            Long gerenciaId = usuario.getGerencia() != null ? 
                usuario.getGerencia().getId().longValue() : null;
            String gerenciaNombre = usuario.getGerencia() != null ? 
                usuario.getGerencia().getNombre() : null;
            
            UsuarioInfo info = new UsuarioInfo(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getRol().name(),
                gerenciaId,
                gerenciaNombre
            );
            return ResponseEntity.ok(info);
        }
        
        return ResponseEntity.ok(null);
    }

    // Clase interna para respuesta de usuario actual
    public record UsuarioInfo(Integer id, String nombre, String rol, Long gerenciaId, String gerenciaNombre) {}
}

