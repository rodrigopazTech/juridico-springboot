package com.juridico.sistema_juridico.entity.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Especialidades jurídicas asociadas a gerencias.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "materias", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"nombre", "gerencia_id"})
})
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerencia_id", nullable = false)
    private Gerencia gerencia;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Builder.Default
    private Boolean activo = true;

    // Auditoría simplificada
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}