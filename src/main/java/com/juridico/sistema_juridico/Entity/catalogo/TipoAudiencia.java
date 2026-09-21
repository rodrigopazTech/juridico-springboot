package com.juridico.sistema_juridico.Entity.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipos_audiencia")
public class TipoAudiencia {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerencia_id")
    private Gerencia gerencia;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Builder.Default
    private Boolean activo = true;

    @Column(name = "dias_notificacion_previa")
    @Builder.Default
    private Integer diasNotificacionPrevia = 3;

    @Column(name = "semaforo_rojo_dias")
    @Builder.Default
    private Integer semaforoRojoDias = 1;

    @Column(name = "semaforo_amarillo_dias")
    @Builder.Default
    private Integer semaforoAmarilloDias = 3;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private com.juridico.sistema_juridico.Entity.usuario.Usuario usuario;
}