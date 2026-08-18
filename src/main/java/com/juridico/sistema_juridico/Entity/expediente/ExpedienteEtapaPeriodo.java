package com.juridico.sistema_juridico.Entity.expediente;

import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.catalogo.EtapaCatalogo;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "expediente_etapa_periodos", indexes = {
        @Index(name = "idx_etapa_periodo_expediente", columnList = "expediente_id"),
        @Index(name = "idx_etapa_periodo_etapa", columnList = "etapa_procesal"),
        @Index(name = "idx_etapa_periodo_activo", columnList = "expediente_id, fecha_fin")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteEtapaPeriodo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_procesal", nullable = false, length = 30)
    private EtapaProcesal etapaProcesal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_catalogo_id")
    private EtapaCatalogo etapaCatalogo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_inicio_id")
    private Usuario usuarioInicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_fin_id")
    private Usuario usuarioFin;

    @Column(name = "motivo_cambio", columnDefinition = "TEXT")
    private String motivoCambio;

    public boolean estaActivo() {
        return fechaFin == null;
    }
}
