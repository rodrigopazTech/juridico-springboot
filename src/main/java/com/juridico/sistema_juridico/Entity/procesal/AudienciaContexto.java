package com.juridico.sistema_juridico.Entity.procesal;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.enums.OrigenAudiencia;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audiencia_contextos", indexes = {
    @Index(name = "idx_audiencia_contexto_origen", columnList = "origen"),
    @Index(name = "idx_audiencia_contexto_materia", columnList = "materia_id"),
    @Index(name = "idx_audiencia_contexto_gerencia", columnList = "gerencia_id")
})
public class AudienciaContexto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", length = 30, nullable = false)
    private OrigenAudiencia origen;

    @Column(name = "numero_expediente_snapshot", length = 50)
    private String numeroExpedienteSnapshot;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gerencia_id")
    private Gerencia gerencia;

    @ManyToOne
    @JoinColumn(name = "organo_jurisdiccional_id")
    private OrganoJurisdiccional organoJurisdiccional;

    @Column(name = "organo_jurisdiccional_texto", columnDefinition = "TEXT")
    private String organoJurisdiccionalTexto;

    @Column(columnDefinition = "TEXT")
    private String partes;

    @Column(length = 150)
    private String sede;

    public String getNumeroReferencia() {
        return this.numeroExpedienteSnapshot;
    }
}
