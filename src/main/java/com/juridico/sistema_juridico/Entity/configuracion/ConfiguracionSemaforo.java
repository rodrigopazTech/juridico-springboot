package com.juridico.sistema_juridico.Entity.configuracion;

import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.NivelConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "configuraciones_semaforo")
public class ConfiguracionSemaforo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoAlerta tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NivelConfiguracionAlerta nivel;

    @ManyToOne
    @JoinColumn(name = "gerencia_id")
    private Gerencia gerencia;

    @Column(name = "rojo_hasta", nullable = false)
    private Integer rojoHasta;

    @Column(name = "amarillo_hasta", nullable = false)
    private Integer amarilloHasta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        createdAt = ahora;
        updatedAt = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
