package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "terminos_presentados")
public class TerminoPresentado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "termino_id", nullable = false)
    private Termino termino;

    @Column(name = "expediente_numero", length = 50)
    private String expedienteNumero;

    @Column(name = "fecha_presentacion", nullable = false)
    private LocalDate fechaPresentacion;

    @Column(name = "acuse_documento", length = 500)
    private String acuseDocumento;
    
    @Column(name = "sincronizado_at")
    private LocalDateTime sincronizadoAt;
}