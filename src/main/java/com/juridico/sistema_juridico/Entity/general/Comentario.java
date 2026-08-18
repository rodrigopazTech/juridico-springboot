package com.juridico.sistema_juridico.Entity.general;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.EtapaCatalogo;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comentarios", indexes = {
    @Index(name = "idx_comentarios_entidad", columnList = "entidad_tipo, entidad_id")
})
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entidad_tipo", nullable = false, length = 30)
    private String entidadTipo; // EXPEDIENTE, AUDIENCIA, TERMINO

    @Column(name = "entidad_id", nullable = false, length = 50)
    private String entidadId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comentario;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Usuario usuario;
    
    @Column(name = "usuario_nombre")
    private String usuarioNombre;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_procesal", length = 30)
    private EtapaProcesal etapaProcesal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_catalogo_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private EtapaCatalogo etapaCatalogo;

    @Transient
    public String getEtapaNombre() {
        if (etapaCatalogo != null) {
            return etapaCatalogo.getNombreJerarquico();
        }
        return etapaProcesal != null ? etapaProcesal.name() : null;
    }
}
