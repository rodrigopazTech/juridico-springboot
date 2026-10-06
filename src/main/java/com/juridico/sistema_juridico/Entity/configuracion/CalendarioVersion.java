package com.juridico.sistema_juridico.Entity.configuracion;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "calendario_versiones",
        uniqueConstraints = @UniqueConstraint(name = "uk_calendario_version", columnNames = "version"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarioVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "fechas_inhabiles", nullable = false, columnDefinition = "TEXT")
    private String fechasInhabiles;

    @Column(name = "motivo_cambio", nullable = false, length = 300)
    private String motivoCambio;

    @Column(name = "publicado_por", nullable = false, length = 180)
    private String publicadoPor;

    @Column(name = "publicado_en", nullable = false, updatable = false)
    private LocalDateTime publicadoEn;

    @PrePersist
    void onCreate() {
        if (publicadoEn == null) publicadoEn = LocalDateTime.now();
    }
}
