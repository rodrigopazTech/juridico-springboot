package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "terminos", indexes = {
    @Index(name = "idx_terminos_expediente", columnList = "expediente_id"),
    @Index(name = "idx_terminos_fecha_vencimiento", columnList = "fecha_vencimiento")
})
public class Termino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id")
    private Expediente expediente;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contexto_id")
    private TerminoContexto contexto;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String asunto;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus_termino", length = 30, nullable = false)
    private EstatusTermino estatusTermino; // PROYECTISTA, REVISION, etc.

    @Column(name = "acuse", length = 500)
    private String acuse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prioridad_id")
    private CatPrioridad prioridad;
    
    @ManyToOne
    @JoinColumn(name = "abogado_responsable_id")
    private Usuario abogadoResponsable;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_presentacion")
    private LocalDate fechaPresentacion;

    @Column(name = "acuse_nombre", length = 500)
    private String acuseNombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_termino_id")
    private com.juridico.sistema_juridico.Entity.catalogo.TipoTermino tipoTerminoRef;

    @Column(name = "es_tiempo_dinamico")
    @Builder.Default
    private Boolean esTiempoDinamico = false;

    @Column(name = "tipo_dias_calculado", length = 20)
    @Builder.Default
    private String tipoDiasCalculado = "HABILES";

    @Column(name = "desglose_dias_texto", columnDefinition = "TEXT")
    private String desgloseDiasTexto;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.fechaCreacion == null) {
            this.fechaCreacion = ahora.toLocalDate();
        }
        if (this.createdAt == null) {
            this.createdAt = ahora;
        }
        this.updatedAt = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getNumeroReferencia() {
        if (this.expediente != null) {
            return this.expediente.getNumero();
        }
        if (this.contexto != null
                && this.contexto.getNumeroExpedienteSnapshot() != null
                && !this.contexto.getNumeroExpedienteSnapshot().trim().isEmpty()) {
            return this.contexto.getNumeroExpedienteSnapshot();
        }
        return "Asunto único";
    }

    public String getPartesReferencia() {
        if (this.expediente != null) {
            return this.expediente.getPartes();
        }
        return this.contexto != null ? this.contexto.getPartes() : null;
    }

    public Materia getMateriaReferencia() {
        if (this.expediente != null) {
            return this.expediente.getMateria();
        }
        return this.contexto != null ? this.contexto.getMateria() : null;
    }

    public Gerencia getGerenciaReferencia() {
        if (this.expediente != null) {
            return this.expediente.getGerencia();
        }
        return this.contexto != null ? this.contexto.getGerencia() : null;
    }

    public String getDescripcionReferencia() {
        if (this.expediente != null) {
            return this.expediente.getDescripcion();
        }
        return this.contexto != null ? this.contexto.getDescripcion() : null;
    }

}
