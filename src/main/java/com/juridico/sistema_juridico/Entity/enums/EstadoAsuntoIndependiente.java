package com.juridico.sistema_juridico.Entity.enums;

import lombok.Getter;

@Getter
public enum EstadoAsuntoIndependiente {
    ACTIVO("Activo"),
    VINCULADO("Vinculado a expediente"),
    INACTIVO("Inactivo");

    private final String descripcion;

    EstadoAsuntoIndependiente(String descripcion) {
        this.descripcion = descripcion;
    }
}
