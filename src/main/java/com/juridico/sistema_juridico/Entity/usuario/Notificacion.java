package com.juridico.sistema_juridico.Entity.usuario;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false, length = 30)
    private String tipo; // AUDIENCIA, TERMINO...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridad prioridad;

    @Builder.Default
    private Boolean leida = false;

    @Column(name = "fecha_leida")
    private LocalDateTime fechaLeida;

    @Column(name = "notificar_en", nullable = false)
    private LocalDateTime notificarEn;

    @Column(name = "entidad_tipo", length = 30)
    private String entidadTipo;

    @Column(name = "entidad_id", length = 50)
    private String entidadId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}