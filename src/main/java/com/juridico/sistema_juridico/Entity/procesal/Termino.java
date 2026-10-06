package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.Entity.enums.TipoEvidenciaPresentacion;
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
    @Index(name = "idx_terminos_asunto_independiente", columnList = "asunto_independiente_id"),
    @Index(name = "idx_terminos_fecha_vencimiento", columnList = "fecha_vencimiento")
})
public class Termino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id")
    private Expediente expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asunto_independiente_id")
    private AsuntoIndependiente asuntoIndependiente;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evidencia_presentacion", length = 30)
    private TipoEvidenciaPresentacion tipoEvidenciaPresentacion;

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

    @Column(name = "alerta_perfil_nombre", length = 120)
    private String alertaPerfilNombre;

    @Column(name = "alerta_perfil_version")
    private Integer alertaPerfilVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "alerta_modo", length = 30)
    private ModoPerfilAlerta alertaModo;

    @Column(name = "alerta_amarillo_dias")
    private Integer alertaAmarilloDias;

    @Column(name = "alerta_rojo_dias")
    private Integer alertaRojoDias;

    @Column(name = "alerta_amarillo_porcentaje_snapshot", updatable = false)
    private Integer alertaAmarilloPorcentajeSnapshot;

    @Column(name = "alerta_amarillo_minimo_snapshot", updatable = false)
    private Integer alertaAmarilloMinimoSnapshot;

    @Column(name = "alerta_amarillo_maximo_snapshot", updatable = false)
    private Integer alertaAmarilloMaximoSnapshot;

    @Column(name = "alerta_rojo_porcentaje_snapshot", updatable = false)
    private Integer alertaRojoPorcentajeSnapshot;

    @Column(name = "alerta_rojo_minimo_snapshot", updatable = false)
    private Integer alertaRojoMinimoSnapshot;

    @Column(name = "alerta_rojo_maximo_snapshot", updatable = false)
    private Integer alertaRojoMaximoSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "plazo_modo_snapshot", length = 30, updatable = false)
    private com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino plazoModoSnapshot;

    @Column(name = "plazo_cantidad_snapshot", updatable = false)
    private Integer plazoCantidadSnapshot;

    @Column(name = "plazo_duracion_detectada", updatable = false)
    private Long plazoDuracionDetectada;

    @Column(name = "plazo_duracion_adoptada")
    private Long plazoDuracionAdoptada;

    @Column(name = "plazo_tipo_dias_snapshot", length = 20, updatable = false)
    private String plazoTipoDiasSnapshot;

    @Column(name = "plazo_calendario_version", updatable = false)
    private Integer plazoCalendarioVersion;

    @Column(name = "plazo_calendario_fechas", columnDefinition = "TEXT", updatable = false)
    private String plazoCalendarioFechas;

    @Column(name = "plazo_regla_computo", length = 120, updatable = false)
    private String plazoReglaComputo;

    @Column(name = "plazo_fecha_calculada", updatable = false)
    private LocalDate plazoFechaCalculada;

    @Column(name = "plazo_fecha_adoptada")
    private LocalDate plazoFechaAdoptada;

    @Column(name = "plazo_explicacion", columnDefinition = "TEXT", updatable = false)
    private String plazoExplicacion;

    @Column(name = "plazo_excepcion_motivo", columnDefinition = "TEXT")
    private String plazoExcepcionMotivo;

    @Column(name = "plazo_excepcion_autor_id")
    private Integer plazoExcepcionAutorId;

    @Column(name = "plazo_excepcion_autor", length = 180)
    private String plazoExcepcionAutor;

    @Column(name = "plazo_excepcion_fecha")
    private LocalDateTime plazoExcepcionFecha;

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
        if (this.asuntoIndependiente != null) {
            return this.asuntoIndependiente.getFolioSiaj();
        }
        if (this.contexto != null
                && this.contexto.getNumeroExpedienteSnapshot() != null
                && !this.contexto.getNumeroExpedienteSnapshot().trim().isEmpty()) {
            return this.contexto.getNumeroExpedienteSnapshot();
        }
        return "Asunto independiente";
    }

    public String getPartesReferencia() {
        if (this.expediente != null) {
            return this.expediente.getPartes();
        }
        if (this.asuntoIndependiente != null) {
            return this.asuntoIndependiente.getParteInteresada();
        }
        return this.contexto != null ? this.contexto.getPartes() : null;
    }

    public Materia getMateriaReferencia() {
        if (this.expediente != null) {
            return this.expediente.getMateria();
        }
        if (this.asuntoIndependiente != null) {
            return this.asuntoIndependiente.getMateria();
        }
        return this.contexto != null ? this.contexto.getMateria() : null;
    }

    public Gerencia getGerenciaReferencia() {
        if (this.expediente != null) {
            return this.expediente.getGerencia();
        }
        if (this.asuntoIndependiente != null) {
            return this.asuntoIndependiente.getGerencia();
        }
        return this.contexto != null ? this.contexto.getGerencia() : null;
    }

    public String getDescripcionReferencia() {
        if (this.expediente != null) {
            return this.expediente.getDescripcion();
        }
        if (this.asuntoIndependiente != null) {
            return this.asuntoIndependiente.getDescripcion();
        }
        return this.contexto != null ? this.contexto.getDescripcion() : null;
    }

}
