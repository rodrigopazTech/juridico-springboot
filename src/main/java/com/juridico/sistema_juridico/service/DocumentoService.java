package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.documento.Carpeta;
import com.juridico.sistema_juridico.Entity.documento.Documento;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.documento.CarpetaRepository;
import com.juridico.sistema_juridico.repository.documento.DocumentoRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private AudienciaRepository audienciaRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AuditoriaService auditoriaService;

    /**
     * Verifica si un usuario tiene permiso para acceder a documentos de un
     * expediente
     */
    public boolean tienePermiso(Usuario usuario, UUID expedienteId) {
        RolUsuario rol = usuario.getRol();

        // DIRECCION y SUBDIRECCION tienen acceso total
        if (rol == RolUsuario.DIRECCION || rol == RolUsuario.SUBDIRECCION) {
            return true;
        }

        Expediente expediente = expedienteRepository.findById(expedienteId).orElse(null);
        if (expediente == null) {
            return false;
        }

        // Abogado responsable tiene acceso
        if (expediente.getAbogadoResponsable() != null &&
                expediente.getAbogadoResponsable().getId().equals(usuario.getId())) {
            return true;
        }

        // Abogado compareciente: verificar si tiene audiencia activa HOY
        if (rol == RolUsuario.ABOGADO) {
            List<Audiencia> audienciasHoy = audienciaRepository
                    .findByExpedienteIdAndFechaAudiencia(expedienteId, LocalDate.now());

            for (Audiencia audiencia : audienciasHoy) {
                if (audiencia.getAbogadoComparece() != null &&
                        audiencia.getAbogadoComparece().getId().equals(usuario.getId())) {
                    return true;
                }
            }
        }

        return false;
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
                .eliminado(false)
                .build();

        Documento guardado = documentoRepository.save(documento);

        // Auditoría
        Expediente exp = expedienteRepository.findById(expedienteId).orElse(null);
        if (exp != null) {
            auditoriaService.registrar(exp, "ARCHIVO CARGADO",
                    "Subido al gestor documental: " + documento.getNombreOriginal() +
                            (carpeta != null ? " en la carpeta " + carpeta.getNombre() : ""),
                    "UPLOAD", usuario);
        }

        return guardado;
    }

    /**
     * Lista documentos activos de una carpeta específica (excluye papelera)
     */
    public List<Documento> listarDocumentosPorCarpeta(UUID carpetaId) {
        return documentoRepository.findByCarpetaIdAndEliminadoFalseOrderByFechaSubidaDesc(carpetaId);
    }

    /**
     * Lista todos los documentos activos de un expediente (excluye papelera)
     */
    public List<Documento> listarDocumentos(UUID expedienteId) {
        return documentoRepository.findByExpedienteIdAndEliminadoFalseOrderByFechaSubidaDesc(expedienteId);
    }

    /**
     * Lista documentos activos en la raíz del expediente (sin carpeta, excluye papelera)
     */
    public List<Documento> listarDocumentosRaiz(UUID expedienteId) {
        return documentoRepository.findByExpedienteIdAndCarpetaIsNullAndEliminadoFalseOrderByFechaSubidaDesc(expedienteId);
    }

    /**
     * Busca documentos activos en un expediente (excluye papelera)
     */
    public List<Documento> buscarDocumentos(UUID expedienteId, String busqueda) {
        return documentoRepository.buscarEnExpedienteActivos(expedienteId, busqueda);
    }

    /**
     * Busca documentos en una carpeta específica
     */
    public List<Documento> buscarDocumentosEnCarpeta(UUID carpetaId, String busqueda) {
        return documentoRepository.buscarEnCarpeta(carpetaId, busqueda);
    }

    /**
     * Calcula el espacio ocupado por documentos activos de un expediente (excluye papelera)
     */
    public Long calcularEspacioOcupado(UUID expedienteId) {
        Long espacio = documentoRepository.calcularEspacioOcupadoActivo(expedienteId);
        return espacio != null ? espacio : 0L;
    }

    /**
     * Mueve un documento a la papelera (soft-delete).
     * Libera inmediatamente la FK de carpeta (carpeta_id = null) para que
     * la carpeta original pueda eliminarse sin violaciones de constraint.
     * Guarda el ID y nombre de la carpeta para poder recrearla al restaurar.
     */
    @Transactional
    public void eliminarDocumento(UUID documentoId, Usuario usuario) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        // Guardar info de carpeta original ANTES de limpiar la referencia
        Carpeta carpetaOriginal = documento.getCarpeta();
        UUID carpetaOriginalId = carpetaOriginal != null ? carpetaOriginal.getId() : null;
        String carpetaOriginalRuta = carpetaOriginal != null ? carpetaOriginal.getRutaString() : null;

        // Auditoría (ANTES de marcar como eliminado)
        Expediente exp = expedienteRepository.findById(documento.getExpedienteId()).orElse(null);
        if (exp != null) {
            auditoriaService.registrar(exp, "ARCHIVO ENVIADO A PAPELERA",
                    "Movido a papelera desde el gestor documental: " + documento.getNombreOriginal(),
                    "TRASH", usuario);
        }

        // Soft-delete: marcar como eliminado y limpiar FK de carpeta
        documento.setEliminado(true);
        documento.setFechaEliminado(LocalDateTime.now());
        documento.setCarpetaOriginalId(carpetaOriginalId);      // UUID para verificar si aún existe
        documento.setCarpetaOriginalRuta(carpetaOriginalRuta); // ruta completa para recrear jerarquía
        documento.setCarpeta(null); // ← liberar FK inmediatamente

        documentoRepository.save(documento);
    }

    // ========================================
    // PAPELERA TEMPORAL
    // ========================================

    /**
     * Lista los documentos en la papelera de un expediente
     */
    public List<Documento> listarPapelera(UUID expedienteId) {
        return documentoRepository.findByExpedienteIdAndEliminadoTrueOrderByFechaEliminadoDesc(expedienteId);
    }

    /**
     * Restaura un documento desde la papelera a su ubicación original.
     *
     * Lógica de restauración:
     *  1. Si la carpeta original aún existe → restaurar ahí.
     *  2. Si la carpeta original fue eliminada pero guardamos su nombre → recrearla y restaurar.
     *  3. Si no hay información de carpeta → restaurar en raíz.
     */
    @Transactional
    public Documento restaurarDocumento(UUID documentoId, Usuario usuario) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        if (!documento.getEliminado()) {
            throw new IllegalArgumentException("El documento no está en la papelera");
        }

        Carpeta carpetaRestaurar = null;
        String mensajeDestino;

        if (documento.getCarpetaOriginalId() != null) {
            // Intentar recuperar la carpeta original por su ID
            carpetaRestaurar = carpetaRepository.findById(documento.getCarpetaOriginalId()).orElse(null);

            if (carpetaRestaurar != null) {
                // Carpeta original existe → restaurar ahí
                mensajeDestino = "carpeta '" + carpetaRestaurar.getNombre() + "'";
            } else if (documento.getCarpetaOriginalRuta() != null) {
                // Carpeta original fue eliminada → recrear jerarquía completa
                String[] carpetasPath = documento.getCarpetaOriginalRuta().split("/");
                Carpeta parentFolder = null;
                
                for (String nombreCarpeta : carpetasPath) {
                    final Carpeta currentParent = parentFolder;
                    // Buscar si esta carpeta ya existe en este nivel
                    Carpeta existingFolder = null;
                    if (currentParent == null) {
                        List<Carpeta> rootFolders = carpetaRepository.findByExpedienteIdAndCarpetaPadreIsNullOrderByOrdenAsc(documento.getExpedienteId());
                        existingFolder = rootFolders.stream().filter(c -> c.getNombre().equals(nombreCarpeta)).findFirst().orElse(null);
                    } else {
                        existingFolder = currentParent.getSubcarpetas().stream().filter(c -> c.getNombre().equals(nombreCarpeta)).findFirst().orElse(null);
                    }
                    
                    if (existingFolder != null) {
                        parentFolder = existingFolder;
                    } else {
                        // Crear la carpeta faltante
                        Carpeta nuevaCarpeta = Carpeta.builder()
                                .nombre(nombreCarpeta)
                                .expedienteId(documento.getExpedienteId())
                                .carpetaPadre(currentParent)
                                .esSistema(false)
                                .orden(0)
                                .usuarioCreador(usuario)
                                .build();
                        parentFolder = carpetaRepository.save(nuevaCarpeta);
                    }
                }
                
                carpetaRestaurar = parentFolder;
                mensajeDestino = "carpeta recreada en ruta '" + documento.getCarpetaOriginalRuta() + "'";
            } else {
                // Sin ruta guardada → raíz
                mensajeDestino = "raíz";
            }
        } else {
            // Sin carpeta original → restaurar en raíz
            mensajeDestino = "raíz";
        }

        // Restaurar el documento
        documento.setCarpeta(carpetaRestaurar);
        documento.setEliminado(false);
        documento.setFechaEliminado(null);
        documento.setCarpetaOriginalId(null);
        documento.setCarpetaOriginalRuta(null);

        Documento restaurado = documentoRepository.save(documento);

        // Auditoría
        Expediente exp = expedienteRepository.findById(documento.getExpedienteId()).orElse(null);
        if (exp != null) {
            auditoriaService.registrar(exp, "ARCHIVO RESTAURADO",
                    "Restaurado desde papelera: " + documento.getNombreOriginal() + " → " + mensajeDestino,
                    "UNDO", usuario);
        }

        return restaurado;
    }

    /**
     * Elimina permanentemente un documento de la papelera (borra archivo físico y registro BD).
     * Este método es llamado manualmente o por el scheduler de purga.
     */
    @Transactional
    public void eliminarPermanente(UUID documentoId, Usuario usuario) throws IOException {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        if (!documento.getEliminado()) {
            throw new IllegalArgumentException("Solo se pueden eliminar permanentemente documentos en la papelera");
        }

        // Auditoría
        Expediente exp = expedienteRepository.findById(documento.getExpedienteId()).orElse(null);
        if (exp != null) {
            auditoriaService.registrar(exp, "ARCHIVO ELIMINADO PERMANENTEMENTE",
                    "Eliminado definitivamente desde papelera: " + documento.getNombreOriginal(),
                    "TRASH", usuario);
        }

        // Eliminar archivo del filesystem
        fileStorageService.eliminarArchivo(documento.getRutaArchivo());

        // Eliminar registro de BD
        documentoRepository.delete(documento);
    }

    /**
     * Purga automática: elimina permanentemente documentos que llevan más de 3 meses en la papelera.
     * Llamado por el scheduler. Sin auditoría por usuario (es proceso automático).
     */
    @Transactional
    public int purgarDocumentosVencidos() throws IOException {
        LocalDateTime fechaLimite = LocalDateTime.now().minusMonths(3);
        List<Documento> vencidos = documentoRepository.findDocumentosVencidosEnPapelera(fechaLimite);

        int contador = 0;
        for (Documento doc : vencidos) {
            try {
                fileStorageService.eliminarArchivo(doc.getRutaArchivo());
                documentoRepository.delete(doc);
                contador++;
            } catch (IOException e) {
                // Loguear error pero continuar con los demás
                System.err.println("[Papelera] Error al eliminar físicamente: " + doc.getNombreOriginal() + " — " + e.getMessage());
                // Eliminar el registro de BD aunque falle el FS para evitar acumulación
                documentoRepository.delete(doc);
                contador++;
            }
        }

        return contador;
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
    public Documento moverDocumento(UUID documentoId, UUID nuevaCarpetaId, Usuario usuario) {
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

        // Determinar carpeta origen antes de cambiarla
        String origen = documento.getCarpeta() != null ? documento.getCarpeta().getNombre() : "Raíz";

        documento.setCarpeta(nuevaCarpeta);
        Documento guardado = documentoRepository.save(documento);

        // Auditoría
        Expediente exp = expedienteRepository.findById(documento.getExpedienteId()).orElse(null);
        if (exp != null) {
            String destino = nuevaCarpeta != null ? nuevaCarpeta.getNombre() : "Raíz";

            auditoriaService.registrar(exp, "ARCHIVO MOVIDO",
                    "Movido de " + origen + " a " + destino + ": " + documento.getNombreOriginal(),
                    "ARROWS_ALT", usuario);
        }

        return guardado;
    }
}
