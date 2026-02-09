package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.dto.response.calendario.EventoResponse;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
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
            @RequestParam(required = false, defaultValue = "todos") String tipo) {
        
        // Obtener usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioActual = null;
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            usuarioActual = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        }
        
        List<EventoResponse> eventos = calendarioService.obtenerEventosCalendario(usuarioActual, tipo);
        
        return ResponseEntity.ok(eventos);
    }
}
