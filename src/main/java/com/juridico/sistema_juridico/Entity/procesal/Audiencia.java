package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.OrigenAudiencia;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audiencias", indexes = {
    @Index(name = "idx_audiencias_expediente", columnList = "expediente_id"),
    @Index(name = "idx_audiencias_asunto_independiente", columnList = "asunto_independiente_id"),
    @Index(name = "idx_audiencias_fecha", columnList = "fecha_audiencia")
})
public class Audiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = true)
    private Expediente expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asunto_independiente_id")
    private AsuntoIndependiente asuntoIndependiente;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contexto_id")
    private AudienciaContexto contexto;

    @DateTimeFormat(pattern = "yyyy-MM-dd") 
    @Column(name = "fecha_audiencia", nullable = false)
    private LocalDate fechaAudiencia;

    @DateTimeFormat(pattern = "HH:mm") 
    @Column(name = "hora_audiencia", nullable = false)
    private LocalTime horaAudiencia;

    @ManyToOne
    @JoinColumn(name = "tipo_audiencia_id")
    private TipoAudiencia tipoAudiencia;

    @Column(name = "es_virtual")
    @Builder.Default
    private Boolean esVirtual = false;

    @Column(name = "url_reunion", columnDefinition = "TEXT")
    private String urlReunion;

    @Column(name = "sala_lugar", columnDefinition = "TEXT")
    private String salaLugar;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus_audiencia", length = 30)
    private EstatusAudiencia estatusAudiencia; // PENDIENTE, CON_ACTA, CONCLUIDA

    @Column(name = "acta_documento", length = 500)
    private String actaDocumento;

    @Column(name = "nota_informativa_documento", length = 500)
    private String notaInformativaDocumento;

    @Column(name = "recordatorio_perfil_nombre", length = 120)
    private String recordatorioPerfilNombre;

    @Column(name = "recordatorio_perfil_version")
    private Integer recordatorioPerfilVersion;

    @Column(name = "recordatorio_amarillo_horas")
    private Integer recordatorioAmarilloHoras;

    @Column(name = "recordatorio_rojo_horas")
    private Integer recordatorioRojoHoras;

    @Column(name = "fecha_desahogo")
    private LocalDate fechaDesahogo;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "abogado_comparece_id")
    private Usuario abogadoComparece;

    @ManyToOne
    @JoinColumn(name = "usuario_delegado_id")
    private Usuario usuarioDelegado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones; // Resultados o notas de la audiencia

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

    public OrigenAudiencia getOrigenAudiencia() {
        if (this.contexto != null && this.contexto.getOrigen() != null) {
            return this.contexto.getOrigen();
        }
        return OrigenAudiencia.EXPEDIENTE;
    }
}
