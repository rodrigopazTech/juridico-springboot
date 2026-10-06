package com.juridico.sistema_juridico.service.procesal;

import org.springframework.stereotype.Service;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.TipoEvidenciaPresentacion;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.exception.BadRequestException;

/** Reglas únicas de transición del flujo institucional de Términos. */
@Service
public class FlujoTerminoService {

    public EstatusTermino validarYResolverAvance(Termino termino, boolean tieneDocumentoTrabajo) {
        if (termino == null || termino.getEstatusTermino() == null) {
            throw new BadRequestException("El Término no tiene una etapa válida.");
        }
        if (!tieneDocumentoTrabajo) {
            throw new BadRequestException("Adjunta el documento de trabajo antes de avanzar.");
        }
        return switch (termino.getEstatusTermino()) {
            case PROYECTISTA -> EstatusTermino.REVISION;
            case REVISION -> EstatusTermino.GERENCIA;
            case GERENCIA -> EstatusTermino.DIRECCION;
            case DIRECCION -> EstatusTermino.LIBERADO;
            case LIBERADO -> throw new BadRequestException(
                    "Para pasar a Presentado debes adjuntar un Acuse o una Nota Informativa.");
            case PRESENTADO -> throw new BadRequestException(
                    "Utiliza la acción Finalizar Término para concluirlo después de revisar la evidencia.");
            case CONCLUIDO -> throw new BadRequestException("El Término ya está concluido.");
        };
    }

    public void validarPresentacion(Termino termino, TipoEvidenciaPresentacion evidencia) {
        if (termino == null || termino.getEstatusTermino() != EstatusTermino.LIBERADO) {
            throw new BadRequestException("Sólo un Término Liberado puede registrarse como Presentado.");
        }
        if (evidencia == null) {
            throw new BadRequestException("Adjunta un Acuse o una Nota Informativa para registrar la presentación.");
        }
    }

    public void validarConclusion(Termino termino) {
        if (termino == null || termino.getEstatusTermino() != EstatusTermino.PRESENTADO) {
            throw new BadRequestException("Sólo un Término Presentado puede concluirse.");
        }
        if (termino.getTipoEvidenciaPresentacion() == null
                || termino.getAcuseNombre() == null || termino.getAcuseNombre().isBlank()) {
            throw new BadRequestException("No se puede concluir sin revisar el Acuse o la Nota Informativa.");
        }
    }
}
