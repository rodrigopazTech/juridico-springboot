package com.juridico.sistema_juridico.Entity.procesal;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.EstadoAsuntoIndependiente;
import com.juridico.sistema_juridico.Entity.enums.TipoReferenciaAsunto;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "asuntos_independientes", indexes = {
        @Index(name = "idx_asunto_ind_folio", columnList = "folio_siaj", unique = true),
        @Index(name = "idx_asunto_ind_estado", columnList = "estado"),
        @Index(name = "idx_asunto_ind_gerencia", columnList = "gerencia_id"),
        @Index(name = "idx_asunto_ind_materia", columnList = "materia_id")
})
public class AsuntoIndependiente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio_siaj", nullable = false, unique = true, length = 50)
    private String folioSiaj;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_referencia", nullable = false, length = 30)
    private TipoReferenciaAsunto tipoReferencia;

    @Column(nullable = false, length = 150)
    private String referencia;

    @Column(name = "fecha_referencia", nullable = false)
    private LocalDate fechaReferencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gerencia_id", nullable = false)
    private Gerencia gerencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Usuario responsable;

    @Column(name = "parte_interesada", nullable = false, length = 255)
    private String parteInteresada;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoAsuntoIndependiente estado = EstadoAsuntoIndependiente.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_destino_id")
    private Expediente expedienteDestino;

    @Column(name = "fecha_vinculacion")
    private LocalDateTime fechaVinculacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vinculado_por_id")
    private Usuario vinculadoPor;

    @Column(name = "motivo_vinculacion", columnDefinition = "TEXT")
    private String motivoVinculacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (createdAt == null) createdAt = ahora;
        updatedAt = ahora;
        if (estado == null) estado = EstadoAsuntoIndependiente.ACTIVO;
        if (fechaReferencia == null) fechaReferencia = LocalDate.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
