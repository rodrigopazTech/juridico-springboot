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

    // 1. Endpoint para Términos (Individual)
    @PostMapping("/terminos")
    public ResponseEntity<?> importarTermino(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey, 
            @RequestBody Map<String, Object> requestJson) {
        
        if (esLlaveInvalida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado: API KEY inválida"));
        }

        String numExp = requestJson.containsKey("numeroExpediente") && requestJson.get("numeroExpediente") != null 
                ? requestJson.get("numeroExpediente").toString() 
                : "SIN_NUMERO";

        loteMigracionService.registrarDatoEnLote("TERMINO", requestJson, numExp);
        
        return ResponseEntity.ok(Map.of(
                "status", "success", 
                "mensaje", "Término histórico recibido y enviado al Asistente de Migración (Lote)"
        ));
    }

    // 2. Endpoint para Audiencias (Individual)
    @PostMapping("/audiencias")
    public ResponseEntity<?> importarAudiencia(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey, 
            @RequestBody Map<String, Object> requestJson) {
        
        if (esLlaveInvalida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado: API KEY inválida"));
        }

        String numExp = requestJson.containsKey("numeroExpediente") && requestJson.get("numeroExpediente") != null 
                ? requestJson.get("numeroExpediente").toString() 
                : "SIN_NUMERO";

        loteMigracionService.registrarDatoEnLote("AUDIENCIA", requestJson, numExp);
        
        return ResponseEntity.ok(Map.of(
                "status", "success", 
                "mensaje", "Audiencia histórica recibida y enviada al Asistente de Migración (Lote)"
        ));
    }

    // 3. Endpoint para Lote de Términos (Soporta formato keyed o lista simple)
    @PostMapping("/lote/terminos")
    public ResponseEntity<?> importarLoteTerminos(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody Object body) {

        if (esLlaveInvalida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado: API KEY inválida"));
        }

        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cuerpo vacío"));
        }

        java.util.List<Map<String, Object>> listaFinal = new java.util.ArrayList<>();
        String gerenciaFinal = "Gerencia de Asuntos Laborales"; // Default

        if (body instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) body;
            if (map.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Map vacío"));
            
            // Caso 1: { "Gerencia X": [ ... ] }
            String firstKey = map.keySet().iterator().next();
            Object value = map.get(firstKey);
            if (value instanceof java.util.List) {
                gerenciaFinal = firstKey;
                listaFinal = (java.util.List<Map<String, Object>>) value;
            } else {
                // Caso 2: Es un objeto individual {...} mandado al endpoint de lote
                listaFinal.add(map);
            }
        } else if (body instanceof java.util.List) {
            // Caso 3: Es una lista simple [ {...}, {...} ]
            listaFinal = (java.util.List<Map<String, Object>>) body;
        }

        int count = 0;
        for (Map<String, Object> item : listaFinal) {
            item.putIfAbsent("gerencia", gerenciaFinal);
            String numExp = item.containsKey("numeroExpediente") && item.get("numeroExpediente") != null
                    ? item.get("numeroExpediente").toString()
                    : "SIN_NUMERO";
            loteMigracionService.registrarDatoEnLote("TERMINO", item, numExp);
            count++;
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "registros", count,
                "mensaje", String.format("%d términos procesados (Gerencia: %s)", count, gerenciaFinal)
        ));
    }

    // 4. Endpoint para Lote de Audiencias (Soporta formato keyed o lista simple)
    @PostMapping("/lote/audiencias")
    public ResponseEntity<?> importarLoteAudiencias(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody Object body) {

        if (esLlaveInvalida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado: API KEY inválida"));
        }

        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cuerpo vacío"));
        }

        java.util.List<Map<String, Object>> listaFinal = new java.util.ArrayList<>();
        String gerenciaFinal = "Gerencia de Asuntos Laborales"; // Default

        if (body instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) body;
            if (map.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Map vacío"));

            // Caso 1: { "Gerencia X": [ ... ] }
            String firstKey = map.keySet().iterator().next();
            Object value = map.get(firstKey);
            if (value instanceof java.util.List) {
                gerenciaFinal = firstKey;
                listaFinal = (java.util.List<Map<String, Object>>) value;
            } else {
                // Caso 2: Es un objeto individual {...}
                listaFinal.add(map);
            }
        } else if (body instanceof java.util.List) {
            // Caso 3: Es una lista simple [ {...}, {...} ]
            listaFinal = (java.util.List<Map<String, Object>>) body;
        }

        int count = 0;
        for (Map<String, Object> item : listaFinal) {
            item.putIfAbsent("gerencia", gerenciaFinal);
            String numExp = item.containsKey("numeroExpediente") && item.get("numeroExpediente") != null
                    ? item.get("numeroExpediente").toString()
                    : "SIN_NUMERO";
            loteMigracionService.registrarDatoEnLote("AUDIENCIA", item, numExp);
            count++;
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "registros", count,
                "mensaje", String.format("%d audiencias procesadas (Gerencia: %s)", count, gerenciaFinal)
        ));
    }
}