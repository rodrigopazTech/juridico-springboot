package com.juridico.sistema_juridico.Entity.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.util.CodigoNormalizer;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipos_audiencia")
public class TipoAudiencia {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerencia_id")
    private Gerencia gerencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Builder.Default
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_recordatorio_id")
    private PerfilRecordatorioAudiencia perfilRecordatorio;

    @Column(name = "duracion_predeterminada_minutos")
    @Builder.Default
    private Integer duracionPredeterminadaMinutos = 60;

    @Column(name = "modalidades_permitidas", nullable = false, length = 80)
    @Builder.Default
    private String modalidadesPermitidas = "PRESENCIAL,VIRTUAL,HIBRIDA";

    @Column(name = "modalidad_predeterminada", length = 20)
    @Builder.Default
    private String modalidadPredeterminada = "PRESENCIAL";

    @Column(name = "requiere_acta", nullable = false)
    @Builder.Default
    private Boolean requiereActa = true;

    @Column(name = "permite_excepcion_sin_acta", nullable = false)
    @Builder.Default
    private Boolean permiteExcepcionSinActa = false;

    @Column(name = "requiere_validacion_acta", nullable = false)
    @Builder.Default
    private Boolean requiereValidacionActa = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (codigo == null || codigo.isBlank()) {
            codigo = CodigoNormalizer.normalizar(nombre != null ? nombre : "TIPO_AUDIENCIA", id);
        } else {
            codigo = CodigoNormalizer.normalizar(codigo);
        }
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private com.juridico.sistema_juridico.Entity.usuario.Usuario usuario;
}
