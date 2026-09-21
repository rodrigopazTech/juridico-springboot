package com.juridico.sistema_juridico.Entity.usuario;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "manual_recovery_codes", indexes = {
        @Index(name = "idx_manual_recovery_user", columnList = "usuario_id"),
        @Index(name = "idx_manual_recovery_expiration", columnList = "fecha_expiracion")
})
public class ManualRecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emitido_por_usuario_id", nullable = false)
    private Usuario emitidoPor;

    @Column(name = "codigo_hash", nullable = false, length = 255)
    private String codigoHash;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "origen", nullable = false, length = 50)
    private String origen;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_uso")
    private LocalDateTime fechaUso;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
