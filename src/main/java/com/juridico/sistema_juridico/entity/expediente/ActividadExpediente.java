package com.juridico.sistema_juridico.entity.expediente;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.juridico.sistema_juridico.entity.usuario.Usuario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "actividad_expedientes")
public class ActividadExpediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "tipo_icono", nullable = false, length = 30)
    private String tipoIcono; // UPLOAD, EDIT, STATUS...

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;
}