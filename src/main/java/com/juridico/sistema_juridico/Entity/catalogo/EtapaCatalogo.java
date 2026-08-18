package com.juridico.sistema_juridico.Entity.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cat_etapas_procesales", indexes = {
        @Index(name = "idx_etapas_materia", columnList = "materia_id"),
        @Index(name = "idx_etapas_parent", columnList = "parent_id"),
        @Index(name = "idx_etapas_orden", columnList = "materia_id, orden")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"materia_id", "nombre", "parent_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapaCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String clave;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 1;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Builder.Default
    private Boolean activo = true;

    @Column(name = "cierra_expediente")
    @Builder.Default
    private Boolean cierraExpediente = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_legacy", length = 30)
    @Builder.Default
    private EtapaProcesal etapaLegacy = EtapaProcesal.TRAMITE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id", nullable = false)
    @JsonIgnore
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private EtapaCatalogo parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Usuario usuario;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean esSubetapa() {
        return parent != null;
    }

    public String getNombreJerarquico() {
        return parent != null ? parent.getNombre() + " / " + nombre : nombre;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (clave == null || clave.isBlank()) {
            clave = normalizarClave(nombre);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (clave == null || clave.isBlank()) {
            clave = normalizarClave(nombre);
        }
    }

    private String normalizarClave(String value) {
        if (value == null) {
            return "ETAPA";
        }
        return value.trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
