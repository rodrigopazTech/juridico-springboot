package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.documento.Carpeta;
import com.juridico.sistema_juridico.Entity.documento.Documento;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.documento.CarpetaRepository;
import com.juridico.sistema_juridico.repository.documento.DocumentoRepository;
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

    private static final int DIAS_RETENCION_PAPELERA = 90;

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

    @Autowired
    private com.juridico.sistema_juridico.repository.Expediente.ColaboradorExpedienteRepository colaboradorExpedienteRepository;

    public boolean tienePermiso(Usuario usuario, UUID expedienteId) {
        RolUsuario rol = usuario.getRol();

        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECCION || rol == RolUsuario.SUBDIRECCION) {
            return true;
        }

        Expediente expediente = expedienteRepository.findById(expedienteId).orElse(null);
        if (expediente == null) {
            return false;
        }

        // Si es el abogado responsable directo, tiene acceso
        if (expediente.getAbogadoResponsable() != null &&
                expediente.getAbogadoResponsable().getId().equals(usuario.getId())) {
            return true;
        }

        // Restricción de Gerencia para roles no globales
        if (usuario.getGerencia() != null) {
            if (expediente.getGerencia() == null ||
                    !expediente.getGerencia().getId().equals(usuario.getGerencia().getId())) {
                return false;
            }
        } else {
            return false;
        }

        // Restricciones específicas por rol
        if (rol == RolUsuario.JEFE_DEPTO) {
            if (expediente.getMateria() == null || usuario.getMaterias().isEmpty()) {
                return false;
            }
            return usuario.getMaterias().stream()
                    .anyMatch(m -> m.getId().equals(expediente.getMateria().getId()));
        }

        if (rol == RolUsuario.GERENTE) {
            return true;
        }

        if (rol == RolUsuario.ABOGADO) {
            return colaboradorExpedienteRepository.existsByExpedienteIdAndUsuarioIdAndFechaExpiracionAfter(
                    expedienteId, usuario.getId(), LocalDateTime.now());
        }

        return false;
    }

    @Transactional
    public Documento guardarDocumento(MultipartFile file, UUID expedienteId, UUID carpetaId,
            String descripcion, Usuario usuario) throws IOException {

        Carpeta carpeta = null;
        if (carpetaId != null) {
            carpeta = carpetaRepository.findById(carpetaId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta no encontrada"));

            if (!carpeta.getExpedienteId().equals(expedienteId)) {
                throw new IllegalArgumentException("La carpeta no pertenece a este expediente");
            }
        }

        // Verificar duplicado de nombre en la ubicación actual
        String nombreOriginal = file.getOriginalFilename();
        if (documentoRepository.existeNombreDuplicado(expedienteId, carpetaId, nombreOriginal)) {
            throw new IllegalArgumentException("Ya existe un archivo activo con el nombre '" + nombreOriginal + "' en esta ubicación");
        }

        // Guardar archivo en filesystem
        String nombreCarpeta = carpeta != null ? carpeta.getNombre() : "raiz";
        String rutaArchivo = fileStorageService.guardarArchivo(file, expedienteId, nombreCarpeta);

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
        return documentoRepository.findByCarpetaIdAndEliminadoFalseAndEliminadoDefinitivoFalseOrderByFechaSubidaDesc(carpetaId);
    }

    /**
     * Lista todos los documentos activos de un expediente (excluye papelera)
     */
    public List<Documento> listarDocumentos(UUID expedienteId) {
        return documentoRepository.findByExpedienteIdAndEliminadoFalseAndEliminadoDefinitivoFalseOrderByFechaSubidaDesc(expedienteId);
    }

    /**
     * Lista documentos activos en la raíz del expediente (sin carpeta, excluye papelera)
     */
    public List<Documento> listarDocumentosRaiz(UUID expedienteId) {
        return documentoRepository.findByExpedienteIdAndCarpetaIsNullAndEliminadoFalseAndEliminadoDefinitivoFalseOrderByFechaSubidaDesc(expedienteId);
    }
    public List<Documento> buscarDocumentos(UUID expedienteId, String busqueda) {
        return documentoRepository.buscarEnExpedienteActivos(expedienteId, busqueda);
    }

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

    @Transactional
    public Documento eliminarDocumento(UUID documentoId, Usuario usuario) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        if (Boolean.TRUE.equals(documento.getEliminado())) {
            throw new IllegalArgumentException("El documento ya se encuentra en la papelera");
        }

        // Guardar info de carpeta original ANTES de limpiar la referencia
        Carpeta carpetaOriginal = documento.getCarpeta();
        UUID carpetaOriginalId = carpetaOriginal != null ? carpetaOriginal.getId() : null;
        String carpetaOriginalRuta = carpetaOriginal != null ? carpetaOriginal.getRutaString() : null;

        LocalDateTime ahora = LocalDateTime.now();
        documento.setEliminado(true);
        documento.setFechaEliminacion(ahora);
        documento.setFechaEliminacionDefinitiva(ahora.plusDays(DIAS_RETENCION_PAPELERA));
        documento.setUsuarioEliminacion(usuario);
        documento.setCarpetaOriginalId(carpetaOriginalId);      // UUID para verificar si aún existe
        documento.setCarpetaOriginalRuta(carpetaOriginalRuta); // ruta completa para recrear jerarquía
        documento.setCarpeta(null); // ← liberar FK inmediatamente

        Documento guardado = documentoRepository.save(documento);

        Expediente exp = expedienteRepository.findById(documento.getExpedienteId()).orElse(null);
        if (exp != null) {
            auditoriaService.registrar(exp, "ARCHIVO EN PAPELERA",
                    "Enviado a la papelera por 90 dias: " + documento.getNombreOriginal(),
                    "TRASH", usuario);
        }

        return guardado;
    }

    // ========================================
    // PAPELERA TEMPORAL
    // ========================================

    /**
     * Lista los documentos en la papelera de un expediente
     */
    public List<Documento> listarPapelera(UUID expedienteId) {
        return documentoRepository.findByExpedienteIdAndEliminadoTrueAndEliminadoDefinitivoFalseOrderByFechaEliminacionDesc(expedienteId);
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
        documento.setFechaEliminacion(null);
        documento.setFechaEliminacionDefinitiva(null);
        documento.setUsuarioEliminacion(null);
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
     * Elimina permanentemente un documento de la papelera (marca como eliminado definitivo).
     * Este método es llamado manualmente o por el scheduler de purga.
     */
    @Transactional
    public void eliminarPermanente(UUID documentoId, Usuario usuario) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        if (!documento.getEliminado()) {
            throw new IllegalArgumentException("Solo se pueden eliminar permanentemente documentos en la papelera");
        }

        // Auditoría
        Expediente exp = expedienteRepository.findById(documento.getExpedienteId()).orElse(null);
        if (exp != null) {
            auditoriaService.registrar(exp, "ARCHIVO ELIMINADO PERMANENTEMENTE",
                    "Eliminado definitivamente desde papelera (oculto en UI): " + documento.getNombreOriginal(),
                    "TRASH", usuario);
        }

        try {
            fileStorageService.eliminarArchivo(documento.getRutaArchivo());
        } catch (IOException e) {
            System.err.println("No se pudo eliminar el archivo físico: " + e.getMessage());
        }
        documentoRepository.delete(documento);
    }

    @Transactional
    public Documento recuperarDocumento(UUID documentoId, Usuario usuario) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        if (!Boolean.TRUE.equals(documento.getEliminado())) {
            throw new IllegalArgumentException("El documento no esta en la papelera");
        }

        documento.setEliminado(false);
        documento.setFechaEliminacion(null);
        documento.setFechaEliminacionDefinitiva(null);
        documento.setUsuarioEliminacion(null);
        Documento recuperado = documentoRepository.save(documento);

        Expediente exp = expedienteRepository.findById(documento.getExpedienteId()).orElse(null);
        if (exp != null) {
            auditoriaService.registrar(exp, "ARCHIVO RECUPERADO",
                    "Recuperado desde la papelera: " + documento.getNombreOriginal(),
                    "UNDO", usuario);
        }

        return recuperado;
    }

    @Transactional
    public int purgarDocumentosVencidos() {
        List<Documento> vencidos = documentoRepository
                .findByEliminadoTrueAndFechaEliminacionDefinitivaBefore(LocalDateTime.now());

        int eliminados = 0;
        for (Documento documento : vencidos) {
            try {
                fileStorageService.eliminarArchivo(documento.getRutaArchivo());
                documentoRepository.delete(documento);
                eliminados++;
            } catch (IOException e) {
                // Mantener el registro si el archivo no pudo borrarse del disco.
            }
        }

        return eliminados;
    }

    /**
     * Obtiene un documento por ID
     */
    public Documento obtenerDocumento(UUID documentoId) {
        return documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
    }

    @Transactional
    public Documento moverDocumento(UUID documentoId, UUID nuevaCarpetaId, Usuario usuario) {
        Documento documento = obtenerDocumento(documentoId);

        if (Boolean.TRUE.equals(documento.getEliminado())) {
            throw new IllegalArgumentException("No se puede mover un documento que esta en la papelera");
        }

        Carpeta nuevaCarpeta = null;
        if (nuevaCarpetaId != null) {
            nuevaCarpeta = carpetaRepository.findById(nuevaCarpetaId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta destino no encontrada"));

            if (!nuevaCarpeta.getExpedienteId().equals(documento.getExpedienteId())) {
                throw new IllegalArgumentException("La carpeta destino no pertenece al mismo expediente");
            }
        }

        String origen = documento.getCarpeta() != null ? documento.getCarpeta().getNombre() : "Raiz";

        documento.setCarpeta(nuevaCarpeta);
        Documento guardado = documentoRepository.save(documento);

        Expediente exp = expedienteRepository.findById(documento.getExpedienteId()).orElse(null);
        if (exp != null) {
            String destino = nuevaCarpeta != null ? nuevaCarpeta.getNombre() : "Raiz";

            auditoriaService.registrar(exp, "ARCHIVO MOVIDO",
                    "Movido de " + origen + " a " + destino + ": " + documento.getNombreOriginal(),
                    "ARROWS_ALT", usuario);
        }

        return guardado;
    }
}
