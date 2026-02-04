package com.juridico.sistema_juridico.Entity.enums;

public enum CategoriaDocumento {
    AUDIENCIAS("Audiencias"),
    TERMINOS("Términos Legales"),
    ANEXOS("Anexos y Pruebas");

    private final String displayName;

    CategoriaDocumento(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
