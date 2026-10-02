package com.juridico.sistema_juridico.Entity.enums;

public enum TipoEvidenciaPresentacion {
    ACUSE("Acuse"),
    NOTA_INFORMATIVA("Nota Informativa");

    private final String descripcion;

    TipoEvidenciaPresentacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
