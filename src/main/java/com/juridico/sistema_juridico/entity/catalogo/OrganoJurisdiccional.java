package com.juridico.sistema_juridico.entity.catalogo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organos_jurisdiccionales")
public class OrganoJurisdiccional {

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
}