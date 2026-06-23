package com.juridico.sistema_juridico.Entity.enums;

import com.juridico.sistema_juridico.Entity.usuario.PreferenciaNotificacion;

public enum NotificacionEvento {
    EXPEDIENTE_NUEVO(
            "EXPEDIENTE",
            "EXPEDIENTE",
            "expediente",
            NotificacionCategoria.EXPEDIENTES,
            "Nuevo expediente",
            "Te avisa cuando se registra un expediente relacionado contigo.",
            true),
    EXPEDIENTE_REASIGNADO(
            "ASIGNACION",
            "EXPEDIENTE",
            "expediente",
            NotificacionCategoria.EXPEDIENTES,
            "Expediente reasignado",
            "Te avisa cuando la titularidad de un expediente cambia hacia ti.",
            true),
    EXPEDIENTE_OBSERVACION_NUEVA(
            "OBSERVACION",
            "EXPEDIENTE",
            "observacion",
            NotificacionCategoria.EXPEDIENTES,
            "Nueva observacion en expediente",
            "Te avisa cuando se agrega una observacion interna en un expediente.",
            true),
    TERMINO_ASIGNADO(
            "TERMINO",
            "TERMINO",
            "termino",
            NotificacionCategoria.TERMINOS,
            "Termino asignado",
            "Te avisa cuando se te asigna un termino existente.",
            true),
    TERMINO_NUEVO(
            "TERMINO",
            "TERMINO",
            "termino",
            NotificacionCategoria.TERMINOS,
            "Nuevo termino",
            "Te avisa cuando se crea un nuevo termino para tu seguimiento.",
            true),
    TERMINO_CAMBIO_ETAPA(
            "TERMINO",
            "TERMINO",
            "termino",
            NotificacionCategoria.TERMINOS,
            "Cambio de etapa del termino",
            "Te avisa cuando un termino cambia de etapa dentro del flujo.",
            true),
    TERMINO_VALIDADO_CONCLUIDO(
            "TERMINO",
            "TERMINO",
            "termino",
            NotificacionCategoria.TERMINOS,
            "Termino validado y concluido",
            "Te avisa cuando un termino queda validado y concluido.",
            true),
    TERMINO_RECORDATORIO(
            "TERMINO",
            "TERMINO",
            "termino",
            NotificacionCategoria.TERMINOS,
            "Recordatorios de vencimiento",
            "Te recuerda terminos proximos a vencer segun el semaforo configurado.",
            true),
    AUDIENCIA_NUEVA(
            "AUDIENCIA",
            "AUDIENCIA",
            "audiencia",
            NotificacionCategoria.AUDIENCIAS,
            "Nueva audiencia",
            "Te avisa cuando se programa una audiencia donde participas.",
            true),
    AUDIENCIA_COMPARECENCIA_ASIGNADA(
            "AUDIENCIA",
            "AUDIENCIA",
            "audiencia",
            NotificacionCategoria.AUDIENCIAS,
            "Comparecencia asignada",
            "Te avisa cuando quedas asignado a una comparecencia.",
            true),
    AUDIENCIA_RECORDATORIO(
            "AUDIENCIA",
            "AUDIENCIA",
            "audiencia",
            NotificacionCategoria.AUDIENCIAS,
            "Recordatorios de audiencia",
            "Te recuerda audiencias proximas en las ventanas definidas por el sistema.",
            true),
    AUDIENCIA_ACTA_DISPONIBLE(
            "AUDIENCIA",
            "AUDIENCIA",
            "audiencia",
            NotificacionCategoria.AUDIENCIAS,
            "Acta disponible",
            "Te avisa cuando el acta de una audiencia queda disponible.",
            true),
    AUDIENCIA_VALIDADA_CONCLUIDA(
            "AUDIENCIA",
            "AUDIENCIA",
            "audiencia",
            NotificacionCategoria.AUDIENCIAS,
            "Audiencia validada y concluida",
            "Te avisa cuando una audiencia termina su validacion y cierre.",
            true),
    SEGURIDAD_PASSWORD_POR_VENCER(
            "SISTEMA",
            "SEGURIDAD",
            "alerta de seguridad",
            NotificacionCategoria.SEGURIDAD,
            "Contrasena por vencer",
            "Te recuerda actualizar tu contrasena antes del vencimiento.",
            false),
    TAREA_RECORDATORIO(
            "TAREA",
            "RECORDATORIO",
            "tarea",
            NotificacionCategoria.TERMINOS,
            "Recordatorio de tarea",
            "Te recuerda tus tareas personales programadas para el dia.",
            true);

    private final String tipoVisual;
    private final String entidadTipo;
    private final String etiquetaActividad;
    private final NotificacionCategoria categoria;
    private final String nombreVisible;
    private final String descripcionVisible;
    private final boolean configurable;

    NotificacionEvento(
            String tipoVisual,
            String entidadTipo,
            String etiquetaActividad,
            NotificacionCategoria categoria,
            String nombreVisible,
            String descripcionVisible,
            boolean configurable) {
        this.tipoVisual = tipoVisual;
        this.entidadTipo = entidadTipo;
        this.etiquetaActividad = etiquetaActividad;
        this.categoria = categoria;
        this.nombreVisible = nombreVisible;
        this.descripcionVisible = descripcionVisible;
        this.configurable = configurable;
    }

    public String getTipoVisual() {
        return tipoVisual;
    }

    public String getEntidadTipo() {
        return entidadTipo;
    }

    public String getEtiquetaActividad() {
        return etiquetaActividad;
    }

    public NotificacionCategoria getCategoria() {
        return categoria;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public String getDescripcionVisible() {
        return descripcionVisible;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public boolean respetaPreferenciasActuales(PreferenciaNotificacion preferencias) {
        if (preferencias == null) {
            return true;
        }

        return switch (this) {
            case EXPEDIENTE_REASIGNADO -> preferencias.isNotificarReasignacion();
            case TERMINO_ASIGNADO, TERMINO_NUEVO -> preferencias.isNotificarNuevoTermino();
            case TERMINO_RECORDATORIO -> preferencias.isNotificarTerminoVencer();
            case AUDIENCIA_NUEVA,
                    AUDIENCIA_COMPARECENCIA_ASIGNADA,
                    AUDIENCIA_RECORDATORIO,
                    AUDIENCIA_ACTA_DISPONIBLE,
                    AUDIENCIA_VALIDADA_CONCLUIDA -> preferencias.isNotificarNuevaAudiencia();
            default -> true;
        };
    }
}
