package com.juridico.sistema_juridico.controller.api;

import com.juridico.sistema_juridico.dto.request.expedientes.ExpedienteRequest;
import com.juridico.sistema_juridico.dto.request.audiencias.AudienciaRequest;
import com.juridico.sistema_juridico.dto.request.terminos.TerminoRequest;
import com.juridico.sistema_juridico.service.procesal.LoteMigracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/migration")
// QUITAMOS EL @PreAuthorize PARA QUE NO INTERVENGA SPRING SECURITY
public class MigrationApiController {

    @Autowired 
    private LoteMigracionService loteMigracionService;

    // LA LLAVE MAESTRA
    private final String API_KEY_SECRETA = "n8nAgendaLegalF1N4813NGDSA6D54";

    private boolean esLlaveInvalida(String apiKey) {
        return apiKey == null || !apiKey.equals(API_KEY_SECRETA);
    }

    // 1. Endpoint para Expedientes
    @PostMapping("/expedientes")
    public ResponseEntity<?> importarExpediente(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey, 
            @Valid @RequestBody ExpedienteRequest request) {
        
        if (esLlaveInvalida(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acceso denegado: API KEY inválida");
        
        loteMigracionService.registrarDatoEnLote("EXPEDIENTE", request, request.getNumero());
        return ResponseEntity.ok("Expediente recibido y enviado al Asistente de Migración (Lote)");
    }

    // 2. Endpoint para Audiencias
    @PostMapping("/audiencias")
    public ResponseEntity<?> importarAudiencia(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey, 
            @Valid @RequestBody AudienciaRequest request) {
        
        if (esLlaveInvalida(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acceso denegado: API KEY inválida");

        loteMigracionService.registrarDatoEnLote("AUDIENCIA", request, request.getAsuntoId().toString());
        return ResponseEntity.ok("Audiencia recibida y enviada al Asistente de Migración (Lote)");
    }

    // 3. Endpoint para Términos
    @PostMapping("/terminos")
    public ResponseEntity<?> importarTermino(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey, 
            @Valid @RequestBody TerminoRequest request) {
        
        if (esLlaveInvalida(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acceso denegado: API KEY inválida");

        loteMigracionService.registrarDatoEnLote("TERMINO", request, request.getAsuntoId().toString());
        return ResponseEntity.ok("Término recibido y enviado al Asistente de Migración (Lote)");
    }
}