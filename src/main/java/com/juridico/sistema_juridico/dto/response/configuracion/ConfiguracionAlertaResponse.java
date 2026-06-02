package com.juridico.sistema_juridico.dto.response.configuracion;

import java.util.Set;

import com.juridico.sistema_juridico.Entity.enums.NivelConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;

public record ConfiguracionAlertaResponse(
        TipoEventoAlerta tipoEvento,
        NivelConfiguracionAlerta nivel,
        Integer gerenciaId,
        Integer usuarioId,
        String asuntoTipo,
        String asuntoId,
        Set<Integer> diasAnticipacion,
        boolean obligatoria) {
}
