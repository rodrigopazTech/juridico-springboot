package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audiencias_desahogadas")
public class AudienciaDesahogada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "audiencia_id", nullable = false)
    private Audiencia audiencia;

    @Column(name = "expediente_numero", length = 50)
    private String expedienteNumero;

    @Column(name = "fecha_audiencia", nullable = false)
    private LocalDate fechaAudiencia;

    @Column(name = "hora_audiencia", nullable = false)
    private LocalTime horaAudiencia;

    @Column(name = "fecha_desahogo", nullable = false)
    private LocalDate fechaDesahogo;

    @Column(name = "sincronizado_at")
    private LocalDateTime sincronizadoAt;
}