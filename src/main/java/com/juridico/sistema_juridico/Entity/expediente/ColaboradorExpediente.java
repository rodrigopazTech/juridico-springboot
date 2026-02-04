package com.juridico.sistema_juridico.Entity.expediente;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "colaboradores_expediente")
public class ColaboradorExpediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "permiso_nivel")
    private String permisoNivel; // "LECTURA_TOTAL"

    @Column(columnDefinition = "TEXT")
    private String motivo;
}