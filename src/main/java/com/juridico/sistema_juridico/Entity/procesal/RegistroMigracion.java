package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registros_migracion")
public class RegistroMigracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NUEVO: Relación con el Lote
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteMigracion lote;

    @Column(nullable = false, length = 50)
    private String tipoDato; // "TERMINO", "AUDIENCIA"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payloadJson; // El JSON crudo

    // PENDIENTE, PROCESADO, ERROR, RECHAZADO (Papelera)
    @Column(nullable = false, length = 20)
    private String estatus = "PENDIENTE"; 

    // NUEVO: Para saber de qué expediente viene (facilita la Etapa 2)
    @Column(length = 100)
    private String numeroExpedienteExtraido;

    @Column(columnDefinition = "TEXT")
    private String mensajeError;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}