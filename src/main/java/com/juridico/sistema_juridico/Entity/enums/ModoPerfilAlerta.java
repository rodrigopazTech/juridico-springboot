package com.juridico.sistema_juridico.Entity.enums;

public enum ModoPerfilAlerta {
    DIAS_RESTANTES("Días restantes"),
    PROPORCIONAL_LIMITADO("Proporcional con límites");

    private final String descripcion;

    ModoPerfilAlerta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
