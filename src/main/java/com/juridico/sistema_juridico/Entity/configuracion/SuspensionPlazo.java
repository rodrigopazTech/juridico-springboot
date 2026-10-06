package com.juridico.sistema_juridico.Entity.configuracion;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "suspensiones_plazo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspensionPlazo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "registrado_por", nullable = false, length = 180)
    private String registradoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (createdAt == null) createdAt = ahora;
        if (updatedAt == null) updatedAt = ahora;
        if (activo == null) activo = true;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
