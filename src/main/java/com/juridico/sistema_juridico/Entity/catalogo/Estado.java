package com.juridico.sistema_juridico.Entity.catalogo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cat_estados")
public class Estado {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerencia_id")
    private Gerencia gerencia;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
    }

    
    @Column(unique = true)
    private String nombre; // Ej: "Ciudad de México", "Jalisco"
    
    @Builder.Default
    private Boolean activo = true;
}