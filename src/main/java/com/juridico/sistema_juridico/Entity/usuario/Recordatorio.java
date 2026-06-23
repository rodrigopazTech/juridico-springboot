package com.juridico.sistema_juridico.Entity.usuario;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recordatorios")
public class Recordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String detalles;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_recordatorio", nullable = false)
    private LocalDate fechaRecordatorio;

    @DateTimeFormat(pattern = "HH:mm")
    @Column(name = "hora_recordatorio", nullable = false)
    private LocalTime horaRecordatorio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prioridad_id")
    private CatPrioridad prioridad;

    @Builder.Default
    private Boolean completado = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}