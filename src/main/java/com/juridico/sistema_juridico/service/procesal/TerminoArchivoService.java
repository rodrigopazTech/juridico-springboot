package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.TipoDocumentoTermino;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoArchivo;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.documento.Carpeta;
import com.juridico.sistema_juridico.Entity.documento.Documento;
import com.juridico.sistema_juridico.repository.procesal.TerminoArchivoRepository;
import com.juridico.sistema_juridico.repository.documento.CarpetaRepository;
import com.juridico.sistema_juridico.repository.documento.DocumentoRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
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
    private final DocumentoRepository documentoRepository;
    private final CarpetaRepository carpetaRepository;
    private final ExpedienteRepository expedienteRepository;

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
        TerminoArchivo guardado = terminoArchivoRepository.save(ta);

        // Sincronizar con el Gestor Documental
        try {
            com.juridico.sistema_juridico.Entity.expediente.Expediente expSinc = termino.getExpediente();
            if (expSinc == null) {
                // Si el término no tiene expediente, se asocia al expediente contenedor de huérfanos
                expSinc = expedienteRepository.findByNumeroIgnoreCaseAndEsTemporal("EXP-ORFANOS", true).orElse(null);
                if (expSinc == null) {
                    expSinc = com.juridico.sistema_juridico.Entity.expediente.Expediente.builder()
                            .numero("EXP-ORFANOS")
                            .esTemporal(true)
                            .descripcion("Contenedor para documentos huérfanos de términos")
                            .abogadoResponsable(actor)
                            .gerencia(actor != null ? actor.getGerencia() : null)
                            .build();
                    expSinc = expedienteRepository.save(expSinc);
                }
            }

            // Buscar o crear la carpeta raíz "Términos Legales" para el expediente correspondiente
            Carpeta carpetaSist = carpetaRepository.findByExpedienteIdAndNombreAndCarpetaPadreIsNull(
                    expSinc.getId(), "Términos Legales").orElse(null);

            if (carpetaSist == null) {
                carpetaSist = Carpeta.builder()
                        .expedienteId(expSinc.getId())
                        .nombre("Términos Legales")
                        .carpetaPadre(null)
                        .usuarioCreador(actor)
                        .orden(1)
                        .esSistema(true)
                        .build();
                carpetaSist = carpetaRepository.save(carpetaSist);
            }

            // Organizar archivos en subcarpetas por término para mantener una estructura persistente
            String asuntoLimpiado = termino.getAsunto() != null ? termino.getAsunto().trim() : "Sin Asunto";
            if (asuntoLimpiado.length() > 30) {
                asuntoLimpiado = asuntoLimpiado.substring(0, 30) + "...";
            }
            String nombreSubcarpeta = "Término " + termino.getId() + " - " + asuntoLimpiado;

            Carpeta carpetaTermino = carpetaRepository.findByExpedienteIdAndNombreAndCarpetaPadreId(
                    expSinc.getId(), nombreSubcarpeta, carpetaSist.getId()).orElse(null);

            if (carpetaTermino == null) {
                carpetaTermino = Carpeta.builder()
                        .expedienteId(expSinc.getId())
                        .nombre(nombreSubcarpeta)
                        .carpetaPadre(carpetaSist)
                        .usuarioCreador(actor)
                        .orden(1)
                        .esSistema(false)
                        .build();
                carpetaTermino = carpetaRepository.save(carpetaTermino);
            }

            String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
            boolean existe = documentoRepository.existeNombreDuplicado(
                    expSinc.getId(),
                    carpetaTermino.getId(),
                    nombreOriginal);

            if (!existe) {
                Documento doc = Documento.builder()
                        .expedienteId(expSinc.getId())
                        .nombreArchivo(nombreOriginal)
                        .nombreOriginal(nombreOriginal)
                        .rutaArchivo(rutaRelativa)
                        .tipoMime(archivo.getContentType())
                        .tamanioBytes(archivo.getSize())
                        .carpeta(carpetaTermino)
                        .descripcion("Documento (" + tipoDocumento + ") del término: " + termino.getAsunto())
                        .usuarioSubida(actor)
                        .eliminado(false)
                        .build();
                documentoRepository.save(doc);
            }
        } catch (Exception e) {
            System.err.println("Error al sincronizar archivo de término con expediente: " + e.getMessage());
            e.printStackTrace();
        }

        return guardado;
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
