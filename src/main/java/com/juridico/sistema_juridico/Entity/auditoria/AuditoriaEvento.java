package com.juridico.sistema_juridico.Entity.auditoria;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auditoria_eventos", indexes = {
        @Index(name = "idx_auditoria_eventos_fecha", columnList = "fecha"),
        @Index(name = "idx_auditoria_eventos_usuario", columnList = "usuario_id"),
        @Index(name = "idx_auditoria_eventos_resultado", columnList = "resultado"),
        @Index(name = "idx_auditoria_eventos_modulo", columnList = "modulo"),
        @Index(name = "idx_auditoria_eventos_session", columnList = "session_id")
})
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String rol;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(length = 80)
    private String ip;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 80)
    private String modulo;

    @Column(length = 120, nullable = false)
    private String accion;

    @Column(length = 500)
    private String ruta;

    @Column(name = "metodo_http", length = 12)
    private String metodoHttp;

    @Column(length = 30, nullable = false)
    private String resultado;

    @Column(name = "codigo_http")
    private Integer codigoHttp;

    @Column(name = "entidad_tipo", length = 80)
    private String entidadTipo;

    @Column(name = "entidad_id", length = 120)
    private String entidadId;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "detalle_json", columnDefinition = "TEXT")
    private String detalleJson;

    @PrePersist
    protected void onCreate() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public String getAccionLegible() {
        Map<String, String> acciones = Map.ofEntries(
                Map.entry("LOGIN_EXITOSO", "Inicio de sesion correcto"),
                Map.entry("LOGIN_FALLIDO", "Intento de inicio fallido"),
                Map.entry("LOGOUT", "Cierre de sesion"),
                Map.entry("ACCESO_PANTALLA", "Abrio una pantalla"),
                Map.entry("CONSULTA_API", "Consulto informacion"),
                Map.entry("ACCION_HTTP", "Realizo una accion"),
                Map.entry("ACCESO_DENEGADO", "Acceso denegado"),
                Map.entry("ERROR_SISTEMA", "Error del sistema"),
                Map.entry("PASSWORD_RESET_SOLICITADO", "Solicito recuperar contrasena"),
                Map.entry("PASSWORD_RESET_EMAIL_ERROR", "No se pudo enviar correo de recuperacion"),
                Map.entry("PASSWORD_RESET_RATE_LIMIT", "Recuperacion bloqueada por demasiados intentos"),
                Map.entry("PASSWORD_RESET_VALIDACION_FALLIDA", "Validacion de contrasena fallida"),
                Map.entry("PASSWORD_RESET_TOKEN_INVALIDO", "Enlace de recuperacion invalido"),
                Map.entry("PASSWORD_RESET_CONFIRMADO", "Contrasena restablecida"),
                Map.entry("PASSWORD_RESET_ADMIN_ENVIADO", "Admin envio enlace de restablecimiento"),
                Map.entry("PASSWORD_RESET_ADMIN_EMAIL_ERROR", "No se pudo enviar enlace solicitado por admin"),
                Map.entry("CUENTA_ACTIVACION_ENVIADA", "Enlace de activacion enviado"),
                Map.entry("CUENTA_ACTIVACION_EMAIL_ERROR", "No se pudo enviar enlace de activacion"),
                Map.entry("USERNAME_RECOVERY_SOLICITADO", "Solicito recordar usuario"),
                Map.entry("USERNAME_RECOVERY_EMAIL_ERROR", "No se pudo enviar recordatorio de usuario"),
                Map.entry("USERNAME_RECOVERY_RATE_LIMIT", "Recordatorio bloqueado por demasiados intentos"));
        return acciones.getOrDefault(accion, accion);
    }

    public String getDiagnosticoAdmin() {
        if (descripcion != null && !descripcion.isBlank()) {
            return descripcion;
        }
        if ("LOGIN_FALLIDO".equals(accion)) {
            return "El usuario no pudo iniciar sesion. Revisa si el usuario existe, esta activo o si la contrasena fue incorrecta.";
        }
        if ("ERROR".equals(resultado)) {
            return "El sistema registro un error tecnico en esta operacion.";
        }
        if ("DENEGADO".equals(resultado)) {
            return "El sistema bloqueo la accion por permisos, sesion o politica de seguridad.";
        }
        return "Operacion registrada sin detalle adicional.";
    }

    public String getResultadoLegible() {
        if ("EXITOSO".equals(resultado)) {
            return "Exitoso";
        }
        if ("FALLIDO".equals(resultado)) {
            return "Fallido";
        }
        if ("DENEGADO".equals(resultado)) {
            return "Denegado";
        }
        if ("ERROR".equals(resultado)) {
            return "Error del sistema";
        }
        return resultado;
    }

    public String getActorLegible() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return "Sistema / anonimo";
    }

    public String getUbicacionLegible() {
        String metodo = metodoHttp != null && !metodoHttp.isBlank() ? metodoHttp : "-";
        String path = ruta != null && !ruta.isBlank() ? ruta : "-";
        return metodo + " " + path;
    }
}
