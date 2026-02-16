package com.juridico.sistema_juridico.Entity.enums;

import lombok.Getter;

@Getter
public enum EstatusTermino {
    PROYECTISTA("Proyectista"),
    REVISION("Revisión"),
    GERENCIA("Gerencia"),
    DIRECCION("Dirección"),
    LIBERADO("Liberado"),
    PRESENTADO("Presentado"),
    CONCLUIDO("Concluido");

    private final String nombre;

    EstatusTermino(String nombre) {
        this.nombre = nombre;
    }
}
