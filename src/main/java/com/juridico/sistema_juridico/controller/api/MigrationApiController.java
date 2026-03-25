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

    // 3. Endpoint para Lote de Términos (formato { "Gerencia X": [ {...}, ... ] })
    @PostMapping("/lote/terminos")
    public ResponseEntity<?> importarLoteTerminos(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody Map<String, Object> requestJson) {

        if (esLlaveInvalida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado: API KEY inválida"));
        }

        if (requestJson == null || requestJson.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El cuerpo de la solicitud no puede estar vacío."));
        }

        // Expecting a single key which is the gerencia name
        String gerencia = requestJson.keySet().iterator().next();
        if (gerencia == null || gerencia.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato de lote inválido: No se encontró el nombre de la gerencia."));
        }

        Object recordsObj = requestJson.get(gerencia);
        if (!(recordsObj instanceof java.util.List)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato de lote inválido: El valor para la gerencia debe ser una lista de objetos."));
        }

        java.util.List<Map<String, Object>> terminos = (java.util.List<Map<String, Object>>) recordsObj;
        int count = 0;
        for (Map<String, Object> termino : terminos) {
            termino.put("gerencia", gerencia); // Add gerencia to each record

            String numExp = termino.containsKey("numeroExpediente") && termino.get("numeroExpediente") != null
                    ? termino.get("numeroExpediente").toString()
                    : "SIN_NUMERO";

            loteMigracionService.registrarDatoEnLote("TERMINO", termino, numExp);
            count++;
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "mensaje", String.format("%d términos históricos de '%s' recibidos y enviados al Asistente de Migración (Lote)", count, gerencia)
        ));
    }

    // 4. Endpoint para Lote de Audiencias (formato { "Gerencia X": [ {...}, ... ] })
    @PostMapping("/lote/audiencias")
    public ResponseEntity<?> importarLoteAudiencias(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody Map<String, Object> requestJson) {

        if (esLlaveInvalida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado: API KEY inválida"));
        }

        if (requestJson == null || requestJson.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El cuerpo de la solicitud no puede estar vacío."));
        }

        // Expecting a single key which is the gerencia name
        String gerencia = requestJson.keySet().iterator().next();
        if (gerencia == null || gerencia.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato de lote inválido: No se encontró el nombre de la gerencia."));
        }

        Object recordsObj = requestJson.get(gerencia);
        if (!(recordsObj instanceof java.util.List)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato de lote inválido: El valor para la gerencia debe ser una lista de objetos."));
        }

        java.util.List<Map<String, Object>> audiencias = (java.util.List<Map<String, Object>>) recordsObj;
        int count = 0;
        for (Map<String, Object> audiencia : audiencias) {
            audiencia.put("gerencia", gerencia); // Add gerencia to each record

            String numExp = audiencia.containsKey("numeroExpediente") && audiencia.get("numeroExpediente") != null
                    ? audiencia.get("numeroExpediente").toString()
                    : "SIN_NUMERO";

            loteMigracionService.registrarDatoEnLote("AUDIENCIA", audiencia, numExp);
            count++;
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "mensaje", String.format("%d audiencias históricas de '%s' recibidas y enviadas al Asistente de Migración (Lote)", count, gerencia)
        ));
    }
}