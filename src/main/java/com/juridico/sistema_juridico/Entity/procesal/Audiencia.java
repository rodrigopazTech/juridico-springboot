package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.springframework.format.annotation.DateTimeFormat;

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;

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

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones; // Resultados o notas de la audiencia

    public String getSemaforoColor() {
        if (EstatusAudiencia.CONCLUIDA.equals(this.estatusAudiencia))
            return "bg-gray-800"; // Concluida (Negro/Gris fuerte)
        if (EstatusAudiencia.CON_ACTA.equals(this.estatusAudiencia))
            return "bg-purple-600"; // Con Acta (Morado)

        long dias = ChronoUnit.DAYS.between(LocalDate.now(), this.fechaAudiencia);

        if (dias < 0)
            return "bg-gray-400"; // Pasada (Gris)
        if (dias <= 1)
            return "bg-red-600 animate-pulse"; // ¡HOY o MAÑANA! (Rojo parpadeante)
        if (dias <= 3)
            return "bg-yellow-400"; // Próxima (Amarillo)
        return "bg-green-500"; // Lejana (Verde)
    }

    public String getTextoDiasRestantes() {
        if (EstatusAudiencia.CONCLUIDA.equals(this.estatusAudiencia))
            return "Concluida";

        long dias = ChronoUnit.DAYS.between(LocalDate.now(), this.fechaAudiencia);

        if (dias == 0)
            return "¡ES HOY!";
        if (dias == 1)
            return "¡MAÑANA!";
        if (dias < 0)
            return "Fue hace " + Math.abs(dias) + " días";
        return "Faltan " + dias + " días";
    }
}