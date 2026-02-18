package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.documento.Carpeta;
import com.juridico.sistema_juridico.Entity.documento.Documento;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.documento.CarpetaRepository;
import com.juridico.sistema_juridico.repository.documento.DocumentoRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private CarpetaRepository carpetaRepository;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SecurityService securityService;

    /**
     * Verifica si un usuario tiene permiso para acceder a documentos de un
     * expediente
     */
    public boolean tienePermiso(Usuario usuario, UUID expedienteId) {
        if (usuario == null || expedienteId == null)
            return false;

        Expediente expediente = expedienteRepository.findById(expedienteId).orElse(null);
        if (expediente == null)
            return false;

        // Delegar a SecurityService para centralizar la lógica de lectura (ACL/RBAC)
        return securityService.tieneAccesoLectura(usuario, expediente);
    }

    /**
     * Guarda un documento en una carpeta específica
     */
    @Transactional
    public Documento guardarDocumento(MultipartFile file, UUID expedienteId, UUID carpetaId,
            String descripcion, Usuario usuario) throws IOException {

        // Validar que la carpeta existe y pertenece al expediente
        Carpeta carpeta = null;
        if (carpetaId != null) {
            carpeta = carpetaRepository.findById(carpetaId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta no encontrada"));

            if (!carpeta.getExpedienteId().equals(expedienteId)) {
                throw new IllegalArgumentException("La carpeta no pertenece a este expediente");
            }
        }

        // Guardar archivo en filesystem
        String nombreCarpeta = carpeta != null ? carpeta.getNombre() : "raiz";
        String rutaArchivo = fileStorageService.guardarArchivo(file, expedienteId, nombreCarpeta);

        // Crear registro en BD
        Documento documento = Documento.builder()
                .expedienteId(expedienteId)
                .nombreArchivo(file.getOriginalFilename())
                .nombreOriginal(file.getOriginalFilename())
                .rutaArchivo(rutaArchivo)
                .tipoMime(file.getContentType())
                .tamanioBytes(file.getSize())
                .carpeta(carpeta)
                .descripcion(descripcion)
                .usuarioSubida(usuario)
                .build();

        return documentoRepository.save(documento);
    }

    /**
     * Lista documentos de una carpeta específica
     */
    public List<Documento> listarDocumentosPorCarpeta(UUID carpetaId) {
        return documentoRepository.findByCarpetaIdOrderByFechaSubidaDesc(carpetaId);
    }

    /**
     * Lista todos los documentos de un expediente
     */
    public List<Documento> listarDocumentos(UUID expedienteId) {
        return documentoRepository.findByExpedienteIdOrderByFechaSubidaDesc(expedienteId);
    }

    /**
     * Lista documentos en la raíz del expediente (sin carpeta asignada)
     */
    public List<Documento> listarDocumentosRaiz(UUID expedienteId) {
        return documentoRepository.findByExpedienteIdAndCarpetaIsNullOrderByFechaSubidaDesc(expedienteId);
    }

    /**
     * Busca documentos en un expediente
     */
    public List<Documento> buscarDocumentos(UUID expedienteId, String busqueda) {
        return documentoRepository.buscarEnExpediente(expedienteId, busqueda);
    }

    /**
     * Busca documentos en una carpeta específica
     */
    public List<Documento> buscarDocumentosEnCarpeta(UUID carpetaId, String busqueda) {
        return documentoRepository.buscarEnCarpeta(carpetaId, busqueda);
    }

    /**
     * Calcula el espacio ocupado por un expediente
     */
    public Long calcularEspacioOcupado(UUID expedienteId) {
        Long espacio = documentoRepository.calcularEspacioOcupado(expedienteId);
        return espacio != null ? espacio : 0L;
    }

    /**
     * Elimina un documento
     */
    @Transactional
    public void eliminarDocumento(UUID documentoId) throws IOException {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        // Eliminar archivo del filesystem
        fileStorageService.eliminarArchivo(documento.getRutaArchivo());

        // Eliminar registro de BD
        documentoRepository.delete(documento);
    }

    /**
     * Obtiene un documento por ID
     */
    public Documento obtenerDocumento(UUID documentoId) {
        return documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
    }

    /**
     * Mueve un documento a otra carpeta
     */
    @Transactional
    public Documento moverDocumento(UUID documentoId, UUID nuevaCarpetaId) {
        Documento documento = obtenerDocumento(documentoId);

        Carpeta nuevaCarpeta = null;
        if (nuevaCarpetaId != null) {
            nuevaCarpeta = carpetaRepository.findById(nuevaCarpetaId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta destino no encontrada"));

            // Validar que la carpeta pertenece al mismo expediente
            if (!nuevaCarpeta.getExpedienteId().equals(documento.getExpedienteId())) {
                throw new IllegalArgumentException("La carpeta destino no pertenece al mismo expediente");
            }
        }

        documento.setCarpeta(nuevaCarpeta);
        return documentoRepository.save(documento);
    }
}
