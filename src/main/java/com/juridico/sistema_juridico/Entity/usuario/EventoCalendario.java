package com.juridico.sistema_juridico.Entity.usuario;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "eventos_calendario", indexes = {
    @Index(name = "idx_eventos_usuario", columnList = "usuario_id"),
    @Index(name = "idx_eventos_fecha_inicio", columnList = "fecha_inicio")
})
public class EventoCalendario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "todo_el_dia")
    @Builder.Default
    private Boolean todoElDia = false;

    @Column(nullable = false, length = 30)
    private String categoria; // AUDIENCIA, TERMINO...

    @Column(length = 7)
    private String color;

    @Column(name = "entidad_tipo", length = 30)
    private String entidadTipo;

    @Column(name = "entidad_id")
    private Integer entidadId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}