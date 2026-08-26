package com.juridico.sistema_juridico.controller.api;

import com.juridico.sistema_juridico.service.procesal.LoteMigracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/migration")
public class MigrationApiController {

    @Autowired 
    private LoteMigracionService loteMigracionService;

    @Value("${migration.api.key:}")
    private String migrationApiKey;

    private boolean esLlaveInvalida(String apiKey) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(migrationApiKey)) {
            return true;
        }
        return !MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                migrationApiKey.getBytes(StandardCharsets.UTF_8));
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

        if (body instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) body;
            listaFinal.add(map);
        } else if (body instanceof java.util.List) {
            listaFinal = (java.util.List<Map<String, Object>>) body;
        }

        int insertados = loteMigracionService.registrarLoteEnLote("TERMINO", listaFinal);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "registros_recibidos", listaFinal.size(),
                "registros_nuevos", insertados,
                "mensaje", String.format("%d términos procesados (nuevos: %d) y enviados al asistente.", listaFinal.size(), insertados)
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

        if (body instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) body;
            listaFinal.add(map);
        } else if (body instanceof java.util.List) {
            listaFinal = (java.util.List<Map<String, Object>>) body;
        }

        int insertados = loteMigracionService.registrarLoteEnLote("AUDIENCIA", listaFinal);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "registros_recibidos", listaFinal.size(),
                "registros_nuevos", insertados,
                "mensaje", String.format("%d audiencias procesadas (nuevos: %d) y enviadas al asistente.", listaFinal.size(), insertados)
        ));
    }
}
