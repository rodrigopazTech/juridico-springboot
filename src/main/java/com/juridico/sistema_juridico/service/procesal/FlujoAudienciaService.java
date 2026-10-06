package com.juridico.sistema_juridico.service.procesal;

import org.springframework.stereotype.Service;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.exception.BadRequestException;

@Service
public class FlujoAudienciaService {

    public void validarRegistroResultado(Audiencia audiencia, String resultado) {
        if (!EstatusAudiencia.PENDIENTE.equals(audiencia.getEstatusAudiencia())) {
            throw new BadRequestException("Sólo una Audiencia pendiente puede registrar resultado.");
        }
        if (resultado == null || resultado.isBlank() || resultado.trim().length() < 5) {
            throw new BadRequestException("Describe el resultado de la Audiencia con al menos 5 caracteres.");
        }
    }

    public void validarConclusion(Audiencia audiencia) {
        if (EstatusAudiencia.CON_ACTA.equals(audiencia.getEstatusAudiencia())) {
            if (audiencia.getResultado() == null || audiencia.getResultado().isBlank()) {
                throw new BadRequestException("Registra el resultado antes de concluir.");
            }
            if (audiencia.getActaDocumento() == null || audiencia.getActaDocumento().isBlank()) {
                throw new BadRequestException("La Audiencia requiere Acta antes de concluir.");
            }
            return;
        }
        if (EstatusAudiencia.SUSPENDIDA.equals(audiencia.getEstatusAudiencia())) {
            if (audiencia.getNotaInformativaDocumento() == null || audiencia.getNotaInformativaDocumento().isBlank()) {
                throw new BadRequestException("La Audiencia suspendida requiere Nota Informativa antes de concluir.");
            }
            return;
        }
        throw new BadRequestException("Registra el resultado o suspende la Audiencia antes de concluir.");
    }
}
