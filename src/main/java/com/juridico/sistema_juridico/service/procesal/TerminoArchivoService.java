package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.TipoDocumentoTermino;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoArchivo;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.procesal.TerminoArchivoRepository;
import com.juridico.sistema_juridico.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TerminoArchivoService {

    private final TerminoArchivoRepository terminoArchivoRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public TerminoArchivo guardarArchivo(Termino termino, MultipartFile archivo, TipoDocumentoTermino tipoDocumento, Usuario actor)
            throws IOException {
        String etapa = normalizarNombreEtapa(termino.getEstatusTermino());
        String baseRelativa;
        if (termino.getExpediente() != null) {
            baseRelativa = "expedientes/" + termino.getExpediente().getId() + "/Terminos/termino-" + termino.getId() + "/" + etapa;
        } else {
            baseRelativa = "terminos-sin-expediente/termino-" + termino.getId() + "/" + etapa;
        }

        String rutaRelativa = fileStorageService.guardarArchivoEnRutaRelativa(archivo, baseRelativa);

        TerminoArchivo ta = TerminoArchivo.builder()
                .termino(termino)
                .tipoDocumento(tipoDocumento)
                .etapa(termino.getEstatusTermino())
                .nombreOriginal(archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo")
                .rutaArchivo(rutaRelativa)
                .tipoMime(archivo.getContentType())
                .tamanioBytes(archivo.getSize())
                .usuarioSubida(actor)
                .build();
        return terminoArchivoRepository.save(ta);
    }

    @Transactional(readOnly = true)
    public Optional<TerminoArchivo> obtenerUltimoPorTipo(Integer terminoId, TipoDocumentoTermino tipoDocumento) {
        return terminoArchivoRepository.findFirstByTerminoIdAndTipoDocumentoOrderByCreatedAtDesc(terminoId, tipoDocumento);
    }

    @Transactional(readOnly = true)
    public List<TerminoArchivo> listarPorTermino(Integer terminoId) {
        return terminoArchivoRepository.findByTerminoIdOrderByCreatedAtDesc(terminoId);
    }

    @Transactional
    public void eliminarArchivosPorTermino(Integer terminoId) {
        List<TerminoArchivo> archivos = terminoArchivoRepository.findByTerminoIdOrderByCreatedAtDesc(terminoId);
        for (TerminoArchivo ta : archivos) {
            try {
                fileStorageService.eliminarArchivo(ta.getRutaArchivo());
            } catch (Exception e) {
                // Ignorar si el archivo no existe en el almacenamiento
            }
        }
        terminoArchivoRepository.deleteAll(archivos);
    }

    private String normalizarNombreEtapa(EstatusTermino estatus) {
        if (estatus == null) {
            return "00-SIN-ETAPA";
        }
        return switch (estatus) {
            case PROYECTISTA -> "01-PROYECTISTA";
            case REVISION -> "02-REVISION";
            case GERENCIA -> "03-GERENCIA";
            case DIRECCION -> "04-DIRECCION";
            case LIBERADO -> "05-LIBERADO";
            case PRESENTADO -> "06-PRESENTADO";
            case CONCLUIDO -> "07-CONCLUIDO";
        };
    }
}
