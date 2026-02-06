package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.service.ExpedienteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/expedientes") // Ajusta esta ruta a la que uses
public class ExpedienteController {

    private final ExpedienteService expedienteService;

    public ExpedienteController(ExpedienteService expedienteService) {
        this.expedienteService = expedienteService;
    }

    @GetMapping
    public ResponseEntity<Page<Expediente>> listarExpedientes(
            @RequestParam(required = false) String folio,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestParam(required = false) Integer materiaId,
            @RequestParam(required = false) Integer tipoId,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) Integer abogadoId,
            Pageable pageable
    ) {
        // Se pasan los 7 parámetros corregidos
        Page<Expediente> expedientes = expedienteService.buscarExpedientes(
                folio, gerenciaId, materiaId, tipoId, prioridad, abogadoId, pageable);
        return ResponseEntity.ok(expedientes);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> obtenerEstadisticas() {
        // Ajustado para llamar a los nuevos métodos sin parámetros
        return ResponseEntity.ok().body(new java.util.HashMap<String, Object>() {{
            put("total", expedienteService.totalExpedientes());
            put("porEtapa", expedienteService.contarPorEtapaProcesal());
            put("porAbogado", expedienteService.contarPorAbogado());
            put("porGerencia", expedienteService.contarPorGerencia());
            put("porMes", expedienteService.contarExpedientesPorMes());
        }});
    }
}