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

    public boolean tienePermiso(Usuario usuario, UUID expedienteId) {
        RolUsuario rol = usuario.getRol();

        if (rol == RolUsuario.DIRECCION || rol == RolUsuario.SUBDIRECCION) {
            return true;
        }

        Expediente expediente = expedienteRepository.findById(expedienteId).orElse(null);
        if (expediente == null) {
            return false;
        }

        if (expediente.getAbogadoResponsable() != null &&
                expediente.getAbogadoResponsable().getId().equals(usuario.getId())) {
            return true;
        }

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

    public List<Documento> listarDocumentosPorCarpeta(UUID carpetaId) {
        return documentoRepository.findActivosByCarpetaIdOrderByFechaSubidaDesc(carpetaId);
    }

    public List<Documento> listarDocumentos(UUID expedienteId) {
        return documentoRepository.findActivosByExpedienteIdOrderByFechaSubidaDesc(expedienteId);
    }

    public List<Documento> listarDocumentosRaiz(UUID expedienteId) {
        return documentoRepository.findActivosByExpedienteIdAndCarpetaIsNullOrderByFechaSubidaDesc(expedienteId);
    }

    public List<Documento> listarPapelera(UUID expedienteId) {
        return documentoRepository.findPapeleraByExpedienteIdOrderByFechaEliminacionDesc(expedienteId);
    }

    public List<Documento> buscarDocumentos(UUID expedienteId, String busqueda) {
        return documentoRepository.buscarEnExpediente(expedienteId, busqueda);
    }

    public List<Documento> buscarDocumentosEnCarpeta(UUID carpetaId, String busqueda) {
        return documentoRepository.buscarEnCarpeta(carpetaId, busqueda);
    }

    public Long calcularEspacioOcupado(UUID expedienteId) {
        Long espacio = documentoRepository.calcularEspacioOcupado(expedienteId);
        return espacio != null ? espacio : 0L;
    }

    @Transactional
    public Documento eliminarDocumento(UUID documentoId, Usuario usuario) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        if (Boolean.TRUE.equals(documento.getEliminado())) {
            throw new IllegalArgumentException("El documento ya se encuentra en la papelera");
        }

        LocalDateTime ahora = LocalDateTime.now();
        documento.setEliminado(true);
        documento.setFechaEliminacion(ahora);
        documento.setFechaEliminacionDefinitiva(ahora.plusDays(DIAS_RETENCION_PAPELERA));
        documento.setUsuarioEliminacion(usuario);
        Documento guardado = documentoRepository.save(documento);

        Expediente exp = expedienteRepository.findById(documento.getExpedienteId()).orElse(null);
        if (exp != null) {
            auditoriaService.registrar(exp, "ARCHIVO EN PAPELERA",
                    "Enviado a la papelera por 90 dias: " + documento.getNombreOriginal(),
                    "TRASH", usuario);
        }

        return guardado;
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
