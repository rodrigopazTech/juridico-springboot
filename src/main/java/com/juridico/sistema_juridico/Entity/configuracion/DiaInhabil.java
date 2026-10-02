package com.juridico.sistema_juridico.Entity.configuracion;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dias_inhabiles", uniqueConstraints = @UniqueConstraint(columnNames = "fecha"))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DiaInhabil {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false, length = 160)
    private String descripcion;
    @Builder.Default @Column(nullable = false)
    private Boolean activo = true;
}
