package com.juridico.sistema_juridico.entity.usuario;

import com.juridico.sistema_juridico.entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.entity.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuarios_email", columnList = "email"),
    @Index(name = "idx_usuarios_rol", columnList = "rol")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RolUsuario rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerencia_id")
    private Gerencia gerencia;

    @Builder.Default
    private Boolean activo = true;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}