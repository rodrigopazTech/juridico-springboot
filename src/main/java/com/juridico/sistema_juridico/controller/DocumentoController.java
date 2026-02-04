package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.documento.Documento;
import com.juridico.sistema_juridico.Entity.enums.CategoriaDocumento;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.DocumentoService;
import com.juridico.sistema_juridico.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Subir documento
     */
    @PostMapping("/expedientes/{expedienteId}")
    public ResponseEntity<?> subirDocumento(
            @PathVariable UUID expedienteId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("categoria") CategoriaDocumento categoria,
            @RequestParam(value = "descripcion", required = false) String descripcion) {

        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, expedienteId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos para subir documentos a este expediente"));
            }

            // Validar archivo
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El archivo está vacío"));
            }

            // Guardar documento
            Documento documento = documentoService.guardarDocumento(file, expedienteId, categoria, descripcion,
                    usuario);

            return ResponseEntity.ok(documento);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar el archivo: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inesperado: " + e.getMessage()));
        }
    }

    /**
     * Listar documentos de un expediente
     */
    @GetMapping("/expedientes/{expedienteId}")
    public ResponseEntity<?> listarDocumentos(
            @PathVariable UUID expedienteId,
            @RequestParam(value = "categoria", required = false) CategoriaDocumento categoria) {

        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, expedienteId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos para ver documentos de este expediente"));
            }

            List<Documento> documentos = documentoService.listarDocumentos(expedienteId, categoria);
            return ResponseEntity.ok(documentos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al listar documentos: " + e.getMessage()));
        }
    }

    /**
     * Buscar documentos
     */
    @GetMapping("/expedientes/{expedienteId}/buscar")
    public ResponseEntity<?> buscarDocumentos(
            @PathVariable UUID expedienteId,
            @RequestParam("q") String busqueda) {

        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, expedienteId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos"));
            }

            List<Documento> documentos = documentoService.buscarDocumentos(expedienteId, busqueda);
            return ResponseEntity.ok(documentos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en búsqueda: " + e.getMessage()));
        }
    }

    /**
     * Obtener información de espacio ocupado
     */
    @GetMapping("/expedientes/{expedienteId}/storage-info")
    public ResponseEntity<?> obtenerInfoEspacio(@PathVariable UUID expedienteId) {
        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, expedienteId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos"));
            }

            Long espacioBytes = documentoService.calcularEspacioOcupado(expedienteId);

            Map<String, Object> info = new HashMap<>();
            info.put("espacioBytes", espacioBytes);
            info.put("espacioMB", espacioBytes / (1024.0 * 1024.0));
            info.put("espacioGB", espacioBytes / (1024.0 * 1024.0 * 1024.0));

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al calcular espacio: " + e.getMessage()));
        }
    }

    /**
     * Descargar documento
     */
    @GetMapping("/{documentoId}/download")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable UUID documentoId) {
        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            Documento documento = documentoService.obtenerDocumento(documentoId);

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, documento.getExpedienteId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Obtener archivo
            Path rutaArchivo = fileStorageService.obtenerArchivo(documento.getRutaArchivo());
            Resource resource = new UrlResource(rutaArchivo.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(documento.getTipoMime()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + documento.getNombreOriginal() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Vista previa de documento (inline)
     */
    @GetMapping("/{documentoId}/preview")
    public ResponseEntity<Resource> previsualizarDocumento(@PathVariable UUID documentoId) {
        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            Documento documento = documentoService.obtenerDocumento(documentoId);

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, documento.getExpedienteId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Obtener archivo
            Path rutaArchivo = fileStorageService.obtenerArchivo(documento.getRutaArchivo());
            Resource resource = new UrlResource(rutaArchivo.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(documento.getTipoMime()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + documento.getNombreOriginal() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Eliminar documento
     */
    @DeleteMapping("/{documentoId}")
    public ResponseEntity<?> eliminarDocumento(@PathVariable UUID documentoId) {
        try {
            // Obtener usuario actual
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            Documento documento = documentoService.obtenerDocumento(documentoId);

            // Verificar permisos
            if (!documentoService.tienePermiso(usuario, documento.getExpedienteId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tiene permisos para eliminar este documento"));
            }

            documentoService.eliminarDocumento(documentoId);
            return ResponseEntity.ok(Map.of("message", "Documento eliminado correctamente"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar documento: " + e.getMessage()));
        }
    }
}
