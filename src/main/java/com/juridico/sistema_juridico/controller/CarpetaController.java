package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.documento.Carpeta;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.CarpetaService;
import com.juridico.sistema_juridico.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/carpetas")
public class CarpetaController {

    @Autowired
    private CarpetaService carpetaService;

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Obtener árbol completo de carpetas de un expediente
     */
    @GetMapping("/expedientes/{expedienteId}/arbol")
    public ResponseEntity<?> obtenerArbolCarpetas(@PathVariable UUID expedienteId) {
        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, expedienteId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos para ver las carpetas de este expediente"));
            }

            List<Map<String, Object>> arbol = carpetaService.obtenerArbolCarpetas(expedienteId);
            return ResponseEntity.ok(arbol);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener árbol de carpetas: " + e.getMessage()));
        }
    }

    /**
     * Obtener carpetas raíz de un expediente
     */
    @GetMapping("/expedientes/{expedienteId}/raiz")
    public ResponseEntity<?> obtenerCarpetasRaiz(@PathVariable UUID expedienteId) {
        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, expedienteId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos"));
            }

            List<Carpeta> carpetas = carpetaService.obtenerCarpetasRaiz(expedienteId);
            return ResponseEntity.ok(carpetas);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener carpetas: " + e.getMessage()));
        }
    }

    /**
     * Obtener subcarpetas de una carpeta
     */
    @GetMapping("/{carpetaId}/subcarpetas")
    public ResponseEntity<?> obtenerSubcarpetas(@PathVariable UUID carpetaId) {
        try {
            Carpeta carpeta = carpetaService.obtenerCarpeta(carpetaId);

            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, carpeta.getExpedienteId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos"));
            }

            List<Carpeta> subcarpetas = carpetaService.obtenerSubcarpetas(carpetaId);
            return ResponseEntity.ok(subcarpetas);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener subcarpetas: " + e.getMessage()));
        }
    }

    /**
     * Crear nueva carpeta
     */
    @PostMapping("/expedientes/{expedienteId}")
    public ResponseEntity<?> crearCarpeta(
            @PathVariable UUID expedienteId,
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "carpetaPadreId", required = false) UUID carpetaPadreId) {

        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, expedienteId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos para crear carpetas en este expediente"));
            }

            Carpeta carpeta = carpetaService.crearCarpeta(expedienteId, nombre, carpetaPadreId, usuario);
            return ResponseEntity.ok(carpeta);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear carpeta: " + e.getMessage()));
        }
    }

    /**
     * Renombrar carpeta
     */
    @PutMapping("/{carpetaId}")
    public ResponseEntity<?> renombrarCarpeta(
            @PathVariable UUID carpetaId,
            @RequestParam("nombre") String nombre) {

        try {
            Carpeta carpeta = carpetaService.obtenerCarpeta(carpetaId);

            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, carpeta.getExpedienteId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos para renombrar esta carpeta"));
            }

            Carpeta carpetaActualizada = carpetaService.renombrarCarpeta(carpetaId, nombre);
            return ResponseEntity.ok(carpetaActualizada);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al renombrar carpeta: " + e.getMessage()));
        }
    }

    /**
     * Eliminar carpeta
     */
    @DeleteMapping("/{carpetaId}")
    public ResponseEntity<?> eliminarCarpeta(@PathVariable UUID carpetaId) {
        try {
            Carpeta carpeta = carpetaService.obtenerCarpeta(carpetaId);

            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, carpeta.getExpedienteId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos para eliminar esta carpeta"));
            }

            carpetaService.eliminarCarpeta(carpetaId);
            return ResponseEntity.ok(Map.of("message", "Carpeta eliminada correctamente"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar carpeta: " + e.getMessage()));
        }
    }

    /**
     * Mover carpeta
     */
    @PutMapping("/{carpetaId}/mover")
    public ResponseEntity<?> moverCarpeta(
            @PathVariable UUID carpetaId,
            @RequestParam(value = "nuevaCarpetaPadreId", required = false) UUID nuevaCarpetaPadreId) {

        try {
            Carpeta carpeta = carpetaService.obtenerCarpeta(carpetaId);

            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, carpeta.getExpedienteId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos para mover esta carpeta"));
            }

            Carpeta carpetaMovida = carpetaService.moverCarpeta(carpetaId, nuevaCarpetaPadreId);
            return ResponseEntity.ok(carpetaMovida);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al mover carpeta: " + e.getMessage()));
        }
    }
}
