package com.juridico.sistema_juridico.Entity.enums;

import lombok.Getter;

@Getter
public enum TipoReferenciaAsunto {
    OFICIO("Oficio"),
    FOLIO("Folio"),
    SOLICITUD("Solicitud"),
    NOTIFICACION("Notificación"),
    CITATORIO("Citatorio"),
    DOCUMENTO_ORIGEN("Documento de origen"),
    EXPEDIENTE_EXTERNO("Expediente externo"),
    OTRO("Otro");

    private final String descripcion;

    TipoReferenciaAsunto(String descripcion) {
        this.descripcion = descripcion;
    }
}
