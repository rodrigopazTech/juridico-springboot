package com.juridico.sistema_juridico.entity.general;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.entity.usuario.Usuario;

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
    private Usuario usuario;
    
    @Column(name = "usuario_nombre")
    private String usuarioNombre;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}