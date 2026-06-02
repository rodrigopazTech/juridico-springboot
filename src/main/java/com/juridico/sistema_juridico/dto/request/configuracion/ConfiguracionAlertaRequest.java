package com.juridico.sistema_juridico.dto.request.configuracion;

import java.util.Set;

import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;

public record ConfiguracionAlertaRequest(
        TipoEventoAlerta tipoEvento,
        Set<Integer> diasAnticipacion,
        Boolean obligatoria) {
}
