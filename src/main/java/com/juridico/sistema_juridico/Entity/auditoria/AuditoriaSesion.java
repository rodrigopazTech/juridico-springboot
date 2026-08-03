package com.juridico.sistema_juridico.Entity.auditoria;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auditoria_sesiones", indexes = {
        @Index(name = "idx_auditoria_sesiones_session", columnList = "session_id"),
        @Index(name = "idx_auditoria_sesiones_usuario", columnList = "usuario_id"),
        @Index(name = "idx_auditoria_sesiones_estado", columnList = "estado"),
        @Index(name = "idx_auditoria_sesiones_inicio", columnList = "fecha_inicio")
})
public class AuditoriaSesion {

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

    @Column(name = "session_id", length = 255, nullable = false)
    private String sessionId;

    @Column(length = 80)
    private String ip;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_ultima_actividad")
    private LocalDateTime fechaUltimaActividad;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(length = 30, nullable = false)
    private String estado;

    @Column(name = "motivo_cierre", length = 80)
    private String motivoCierre;

    @PrePersist
    protected void onCreate() {
        if (fechaInicio == null) {
            fechaInicio = LocalDateTime.now();
        }
        if (fechaUltimaActividad == null) {
            fechaUltimaActividad = fechaInicio;
        }
        if (estado == null) {
            estado = "ACTIVA";
        }
    }
}
