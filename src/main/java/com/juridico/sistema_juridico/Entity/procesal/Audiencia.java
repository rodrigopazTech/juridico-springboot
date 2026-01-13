package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;

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
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Column(name = "fecha_audiencia", nullable = false)
    private LocalDate fechaAudiencia;

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

    @Column(name = "estatus_audiencia", length = 30)
    private String estatusAudiencia; // PENDIENTE, CON_ACTA, CONCLUIDA (Podría ser Enum)

    @Column(name = "acta_documento", length = 500)
    private String actaDocumento;

    @Column(name = "fecha_desahogo")
    private LocalDate fechaDesahogo;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}