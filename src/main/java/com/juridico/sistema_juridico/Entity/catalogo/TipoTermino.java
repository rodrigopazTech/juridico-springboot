package com.juridico.sistema_juridico.Entity.catalogo;

import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "tipos_termino")
public class TipoTermino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_plazo", nullable = false, length = 30)
    @Builder.Default
    private ModoPlazoTermino modoPlazo = ModoPlazoTermino.CALCULADO;

    @Column(name = "dias_predeterminados")
    @Builder.Default
    private Integer diasPredeterminados = 15;

    @Column(name = "tipo_dias", nullable = false, length = 20)
    @Builder.Default
    private String tipoDias = "HABILES"; // HABILES, NATURALES

    @Column(name = "ajusta_siguiente_habil")
    @Builder.Default
    private Boolean ajustaAlSiguienteHabil = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_alerta_id")
    private PerfilAlertaTermino perfilAlerta;

    @Column(name = "requiere_documento_liberado", nullable = false)
    @Builder.Default
    private Boolean requiereDocumentoLiberado = true;

    @Column(name = "requiere_acuse", nullable = false)
    @Builder.Default
    private Boolean requiereAcuse = true;

    @Column(name = "permite_excepcion_sin_acuse", nullable = false)
    @Builder.Default
    private Boolean permiteExcepcionSinAcuse = false;

    @Column(name = "requiere_validacion_acuse", nullable = false)
    @Builder.Default
    private Boolean requiereValidacionAcuse = true;

    @Builder.Default
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerencia_id")
    private Gerencia gerencia;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (codigo == null || codigo.isBlank()) {
            codigo = nombre == null ? "TIPO_TERMINO" : nombre.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        }
        if (createdAt == null) createdAt = ahora;
        if (updatedAt == null) updatedAt = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
