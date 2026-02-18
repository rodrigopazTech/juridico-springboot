package com.juridico.sistema_juridico.Entity.expediente;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.catalogo.TipoExpediente;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expedientes", indexes = {
        @Index(name = "idx_expedientes_numero", columnList = "numero"),
        @Index(name = "idx_expedientes_etapa", columnList = "etapa_procesal"),
        @Index(name = "idx_expedientes_abogado", columnList = "abogado_responsable_id")
})
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gerencia_id")
    private Gerencia gerencia;

    @ManyToOne
    @JoinColumn(name = "organo_jurisdiccional_id")
    private OrganoJurisdiccional organoJurisdiccional;

    @Column(name = "organo_jurisdiccional_texto", columnDefinition = "TEXT")
    private String organoJurisdiccionalTexto;

    @Column(columnDefinition = "TEXT")
    private String partes;

    @Column(length = 150)
    private String sede;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Prioridad prioridad = Prioridad.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_procesal", nullable = false, length = 30)
    @Builder.Default
    private EtapaProcesal etapaProcesal = EtapaProcesal.TRAMITE;

    @ManyToOne
    @JoinColumn(name = "abogado_responsable_id")
    private Usuario abogadoResponsable;

    @Column(name = "abogado_responsable_nombre", length = 200)
    private String abogadoResponsableNombre;

    // =========================
    // RELACIONES
    // =========================
    @ToString.Exclude
    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL)
    private List<Audiencia> audiencias;

    @ToString.Exclude
    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL)
    private List<Termino> terminos;

    // =========================
    // FECHAS (OPCIÓN 2)
    // =========================
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "tipo_expediente_id")
    private TipoExpediente tipoExpediente;

    // =========================
    // CALLBACKS JPA
    // =========================
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
