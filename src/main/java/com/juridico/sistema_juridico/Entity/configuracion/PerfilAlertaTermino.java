package com.juridico.sistema_juridico.Entity.configuracion;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "perfiles_alerta_termino",
        uniqueConstraints = @UniqueConstraint(name = "uk_perfil_alerta_codigo_version", columnNames = {"codigo", "version"}))
public class PerfilAlertaTermino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModoPerfilAlerta modo;

    @Column(name = "amarillo_dias")
    private Integer amarilloDias;

    @Column(name = "rojo_dias")
    private Integer rojoDias;

    @Column(name = "amarillo_porcentaje")
    private Integer amarilloPorcentaje;

    @Column(name = "amarillo_minimo_dias")
    private Integer amarilloMinimoDias;

    @Column(name = "amarillo_maximo_dias")
    private Integer amarilloMaximoDias;

    @Column(name = "rojo_porcentaje")
    private Integer rojoPorcentaje;

    @Column(name = "rojo_minimo_dias")
    private Integer rojoMinimoDias;

    @Column(name = "rojo_maximo_dias")
    private Integer rojoMaximoDias;

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private EstadoPerfil estado = EstadoPerfil.BORRADOR;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (createdAt == null) createdAt = ahora;
        if (updatedAt == null) updatedAt = ahora;
        if (version == null) version = 1;
        if (estado == null) estado = EstadoPerfil.BORRADOR;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean estaPublicado() {
        return EstadoPerfil.PUBLICADO == estado;
    }
}
