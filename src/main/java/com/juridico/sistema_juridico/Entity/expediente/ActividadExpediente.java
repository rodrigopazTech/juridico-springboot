package com.juridico.sistema_juridico.Entity.expediente;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.EtapaCatalogo;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "actividad_expedientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActividadExpediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String tipoIcono; // PLUS, EDIT, CHECK, CLOCK, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_procesal", length = 30)
    private EtapaProcesal etapaProcesal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_catalogo_id")
    private EtapaCatalogo etapaCatalogo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_periodo_id")
    private ExpedienteEtapaPeriodo etapaPeriodo;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
