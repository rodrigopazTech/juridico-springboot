package com.juridico.sistema_juridico.controller.api;

import com.juridico.sistema_juridico.dto.request.expedientes.ExpedienteRequest;
import com.juridico.sistema_juridico.dto.request.audiencias.AudienciaRequest;
import com.juridico.sistema_juridico.dto.request.terminos.TerminoRequest;
import com.juridico.sistema_juridico.service.ExpedienteService;
import com.juridico.sistema_juridico.service.procesal.AudienciaService;
import com.juridico.sistema_juridico.service.procesal.TerminoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;


import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/migration")
@PreAuthorize("hasAuthority('ADMIN')") // Solo el ADMIN (o n8n con token de admin) puede entrar
public class MigrationApiController {

    @Autowired private ExpedienteService expedienteService;
    @Autowired private AudienciaService audienciaService;
    @Autowired private TerminoService terminoService;

    // 1. Endpoint para Expedientes
    @PostMapping("/expedientes")
    public ResponseEntity<?> importarExpediente(@Valid @RequestBody ExpedienteRequest request) {
        // Tu ExpedienteRequest pide numero, materiaId, gerenciaId y descripcion
        expedienteService.guardarExpediente(request);
        return ResponseEntity.ok("Expediente creado correctamente");
    }

    // 2. Endpoint para Audiencias
    @PostMapping("/audiencias")
    public ResponseEntity<?> importarAudiencia(@Valid @RequestBody AudienciaRequest request) {
        // Tu AudienciaRequest pide asuntoId (el ID del expediente)
        audienciaService.guardarAudiencia(request);
        return ResponseEntity.ok("Audiencia vinculada correctamente");
    }

    // 3. Endpoint para Términos
    @PostMapping("/terminos")
    public ResponseEntity<?> importarTermino(@Valid @RequestBody TerminoRequest request) {
        // Tu TerminoRequest pide asuntoId, asunto (descripcion) y fechaVencimiento
        terminoService.guardarTermino(request);
        return ResponseEntity.ok("Término legal registrado");
    }
}