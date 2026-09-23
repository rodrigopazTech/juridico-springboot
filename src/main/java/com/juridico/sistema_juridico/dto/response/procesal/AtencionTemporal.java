package com.juridico.sistema_juridico.dto.response.procesal;

import com.juridico.sistema_juridico.Entity.enums.NivelAtencionTemporal;

public record AtencionTemporal(
        NivelAtencionTemporal nivel,
        String etiqueta,
        String detalle,
        String explicacion,
        String icono,
        String clasesBadge,
        String clasesPunto,
        Long cantidadRestante,
        String unidad,
        Integer umbralAmarillo,
        Integer umbralRojo) {
}
