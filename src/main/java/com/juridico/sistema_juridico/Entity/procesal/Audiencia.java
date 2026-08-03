package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

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
    @Index(name = "idx_audiencias_fecha", columnList = "fecha_audiencia")
})
public class Audiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = true)
    private Expediente expediente;

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

   public String getSemaforoColor() {
        // Calculamos los días restantes
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), this.fechaAudiencia);
        
        // El color ahora depende 100% del tiempo restante
        if (dias < 0) return "bg-gray-400"; // Pasada (Gris)
        if (dias <= 1) return "bg-red-600 animate-pulse"; // ¡HOY o MAÑANA! (Rojo parpadeante)
        if (dias <= 3) return "bg-yellow-400"; // Próxima (Amarillo)
        return "bg-green-500"; // Lejana (Verde)
    }

    public String getTextoDiasRestantes() {
        if (EstatusAudiencia.CONCLUIDA.equals(this.estatusAudiencia)) return "Concluida";
        
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), this.fechaAudiencia);
        
        if (dias == 0) return "¡ES HOY!";
        if (dias == 1) return "¡MAÑANA!";
        if (dias < 0) return "Fue hace " + Math.abs(dias) + " días";
        return "Faltan " + dias + " días";
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

    public OrigenAudiencia getOrigenAudiencia() {
        if (this.contexto != null && this.contexto.getOrigen() != null) {
            return this.contexto.getOrigen();
        }
        return OrigenAudiencia.EXPEDIENTE;
    }
}
