package com.juridico.sistema_juridico.Entity.enums;

public enum ModoPlazoTermino {
    CALCULADO("Calculado por el sistema"),
    CAPTURADO("Capturado al registrar"),
    SIN_VENCIMIENTO("Sin vencimiento");

    private final String descripcion;

    ModoPlazoTermino(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
