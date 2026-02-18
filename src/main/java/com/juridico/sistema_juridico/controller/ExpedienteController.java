package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.ExpedienteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expedientes") // Ajusta esta ruta a la que uses
public class ExpedienteController {

    private final ExpedienteService expedienteService;
    private final UsuarioRepository usuarioRepository;

    public ExpedienteController(ExpedienteService expedienteService, UsuarioRepository usuarioRepository) {
        this.expedienteService = expedienteService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Expediente>> listarExpedientes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) Integer tipoId,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) Integer abogadoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
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

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Expediente> expedientes = expedienteService.buscarExpedientes(
                keyword, gerenciaId, materiaId, tipoId, prioridad, abogadoId,
                secGerenciaId, secMateriaIds, secUsuarioId, pageable);

        return ResponseEntity.ok(expedientes);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> obtenerEstadisticas() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        // ROD-33: Solo Dirección y Subdirección pueden ver estadísticas globales.
        // En el futuro se podría filtrar por gerencia para Mandos Medios.
        if (usuario.getRol() != RolUsuario.DIRECCION && usuario.getRol() != RolUsuario.SUBDIRECCION) {
            return ResponseEntity.status(403)
                    .body("Acceso denegado: Solo la Dirección puede ver estadísticas globales.");
        }

        return ResponseEntity.ok().body(new java.util.HashMap<String, Object>() {
            {
                put("total", expedienteService.totalExpedientes());
                put("porEtapa", expedienteService.contarPorEtapaProcesal());
                put("porAbogado", expedienteService.contarPorAbogado());
                put("porGerencia", expedienteService.contarPorGerencia());
                put("porMes", expedienteService.contarExpedientesPorMes());
            }
        });
    }
}