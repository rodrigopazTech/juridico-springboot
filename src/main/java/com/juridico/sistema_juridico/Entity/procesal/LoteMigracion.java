package com.juridico.sistema_juridico.Entity.procesal;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lotes_migracion")
public class LoteMigracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_responsable_id")
    private Usuario usuarioResponsable;

    // Etapa en la que se quedó el usuario: 1 (Catálogos), 2 (Expedientes), 3 (Audiencias), 4 (Términos), 5 (Completado)
    @Column(nullable = false)
    private Integer pasoActual = 1;

    // PENDIENTE, PROCESANDO, COMPLETADO
    @Column(nullable = false, length = 20)
    private String estatus = "PENDIENTE";

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}