package com.juridico.sistema_juridico.Entity.enums;

public enum EstatusAudiencia {
    PENDIENTE("Pendiente"),
    CON_ACTA("Resultado registrado"),
    SUSPENDIDA("Suspendida"),
    CONCLUIDA("Concluida");

    private final String descripcion;

    EstatusAudiencia(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
