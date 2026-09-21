package com.juridico.sistema_juridico.Entity.enums;

public enum NotificacionCategoria {
    EXPEDIENTES("Expedientes", 1),
    TERMINOS("Terminos", 2),
    AUDIENCIAS("Audiencias", 3),
    TAREAS("Tareas", 4),
    SEGURIDAD("Seguridad", 5);

    private final String nombreVisible;
    private final int orden;

    NotificacionCategoria(String nombreVisible, int orden) {
        this.nombreVisible = nombreVisible;
        this.orden = orden;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public int getOrden() {
        return orden;
    }
}
