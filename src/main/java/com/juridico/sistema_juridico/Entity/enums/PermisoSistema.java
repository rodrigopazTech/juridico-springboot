package com.juridico.sistema_juridico.Entity.enums;

public enum PermisoSistema {
    DASHBOARD_VER("Navegacion", "Dashboard", "Acceso al tablero principal del sistema."),
    AGENDA_VER("Navegacion", "Agenda", "Acceso a la agenda general y filtros de seguimiento."),
    CATALOGOS_VER("Navegacion", "Catalogos", "Acceso a catalogos y configuraciones operativas."),
    USUARIOS_VER("Administracion", "Usuarios", "Gestion de usuarios, roles y estructura operativa."),
    ADMIN_PANEL("Administracion", "Panel Admin", "Acceso a funciones exclusivas del panel administrativo."),
    EXPEDIENTES_VER("Operacion", "Expedientes", "Consulta y trabajo operativo en expedientes."),
    EXPEDIENTES_SUPERVISION("Operacion", "Supervision de Expedientes",
            "Vision ampliada para supervisar expedientes fuera del alcance base del rol."),
    TERMINOS_VER("Operacion", "Terminos", "Consulta y seguimiento de terminos procesales."),
    TERMINOS_SUPERVISION("Operacion", "Supervision de Terminos",
            "Facultad ampliada para revisar y supervisar terminos de otros equipos."),
    AUDIENCIAS_VER("Operacion", "Audiencias", "Consulta y seguimiento de audiencias."),
    AUDIENCIAS_SUPERVISION("Operacion", "Supervision de Audiencias",
            "Permite gestionar audiencias con alcance de supervision."),
    DOCUMENTOS_SUPERVISION("Operacion", "Gestor Documental Global",
            "Acceso reforzado para revisar y administrar documentos con alcance ampliado."),
    ALERTAS_CONFIGURAR("Configuracion", "Configurar Alertas",
            "Permite administrar ajustes funcionales de alertas y seguimiento.");

    private final String categoria;
    private final String titulo;
    private final String descripcion;

    PermisoSistema(String categoria, String titulo, String descripcion) {
        this.categoria = categoria;
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getAuthority() {
        return "PERM_" + name();
    }
}
