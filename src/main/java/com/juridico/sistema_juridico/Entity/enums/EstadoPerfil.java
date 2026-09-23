package com.juridico.sistema_juridico.Entity.enums;

public enum EstadoPerfil {
    BORRADOR("Borrador"),
    PUBLICADO("Publicado"),
    INACTIVO("Inactivo");

    private final String descripcion;

    EstadoPerfil(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
