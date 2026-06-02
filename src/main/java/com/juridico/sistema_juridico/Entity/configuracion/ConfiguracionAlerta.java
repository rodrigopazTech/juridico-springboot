package com.juridico.sistema_juridico.Entity.configuracion;

import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.NivelConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "configuraciones_alerta", indexes = {
        @Index(name = "idx_config_alerta_tipo_activa", columnList = "tipo_evento, activa"),
        @Index(name = "idx_config_alerta_gerencia", columnList = "gerencia_id"),
        @Index(name = "idx_config_alerta_usuario", columnList = "usuario_id"),
        @Index(name = "idx_config_alerta_asunto", columnList = "asunto_tipo, asunto_id")
})
public class ConfiguracionAlerta {

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

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "asunto_tipo", length = 30)
    private String asuntoTipo;

    @Column(name = "asunto_id", length = 100)
    private String asuntoId;

    @Column(name = "dias_anticipacion", nullable = false)
    private Integer diasAnticipacion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean obligatoria = false;

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
