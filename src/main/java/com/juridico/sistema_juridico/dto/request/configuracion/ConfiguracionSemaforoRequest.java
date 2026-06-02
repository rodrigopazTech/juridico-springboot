package com.juridico.sistema_juridico.dto.request.configuracion;

import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;

public record ConfiguracionSemaforoRequest(
        TipoEventoAlerta tipoEvento,
        Integer rojoHasta,
        Integer amarilloHasta) {
}
