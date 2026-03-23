package com.juridico.sistema_juridico.controller.api;

import com.juridico.sistema_juridico.service.procesal.LoteMigracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/migration")
public class MigrationApiController {

    @Autowired 
    private LoteMigracionService loteMigracionService;

    private final String API_KEY_SECRETA = "n8nAgendaLegalF1N4813NGDSA6D54";

    private boolean esLlaveInvalida(String apiKey) {
        return apiKey == null || !apiKey.equals(API_KEY_SECRETA);
    }

    // 1. Endpoint para Términos
    @PostMapping("/terminos")
    public ResponseEntity<?> importarTermino(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey, 
            @RequestBody Map<String, Object> requestJson) {
        
        if (esLlaveInvalida(apiKey)) {
            // Cambiado a JSON
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado: API KEY inválida"));
        }

        String numExp = requestJson.containsKey("numeroExpediente") && requestJson.get("numeroExpediente") != null 
                ? requestJson.get("numeroExpediente").toString() 
                : "SIN_NUMERO";

        loteMigracionService.registrarDatoEnLote("TERMINO", requestJson, numExp);
        
        // Cambiado a JSON
        return ResponseEntity.ok(Map.of(
                "status", "success", 
                "mensaje", "Término histórico recibido y enviado al Asistente de Migración (Lote)"
        ));
    }

    // 2. Endpoint para Audiencias
    @PostMapping("/audiencias")
    public ResponseEntity<?> importarAudiencia(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey, 
            @RequestBody Map<String, Object> requestJson) {
        
        if (esLlaveInvalida(apiKey)) {
            // Cambiado a JSON
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado: API KEY inválida"));
        }

        String numExp = requestJson.containsKey("numeroExpediente") && requestJson.get("numeroExpediente") != null 
                ? requestJson.get("numeroExpediente").toString() 
                : "SIN_NUMERO";

        loteMigracionService.registrarDatoEnLote("AUDIENCIA", requestJson, numExp);
        
        // Cambiado a JSON
        return ResponseEntity.ok(Map.of(
                "status", "success", 
                "mensaje", "Audiencia histórica recibida y enviada al Asistente de Migración (Lote)"
        ));
    }
}