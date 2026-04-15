package com.juridico.sistema_juridico.Entity.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organos_jurisdiccionales")
public class OrganoJurisdiccional {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerencia_id")
    private Gerencia gerencia;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 300)
    private String nombre;

    @Column(length = 50)
    private String tipo;

    @Column(length = 100)
    private String sede;

    @Builder.Default
    private Boolean activo = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}