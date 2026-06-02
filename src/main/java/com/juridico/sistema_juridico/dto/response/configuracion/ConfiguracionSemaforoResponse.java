package com.juridico.sistema_juridico.dto.response.configuracion;

import com.juridico.sistema_juridico.Entity.enums.NivelConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;

public record ConfiguracionSemaforoResponse(
        TipoEventoAlerta tipoEvento,
        NivelConfiguracionAlerta nivel,
        Integer gerenciaId,
        Integer rojoHasta,
        Integer amarilloHasta) {
}
