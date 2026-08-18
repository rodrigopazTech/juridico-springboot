package com.juridico.sistema_juridico.Entity.expediente;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.juridico.sistema_juridico.Entity.catalogo.EtapaCatalogo;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bitacora_datos")
public class BitacoraDato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String campo;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_procesal", length = 30)
    private EtapaProcesal etapaProcesal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_catalogo_id")
    private EtapaCatalogo etapaCatalogo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_periodo_id")
    private ExpedienteEtapaPeriodo etapaPeriodo;
}
