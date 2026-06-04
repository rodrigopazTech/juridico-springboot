package com.juridico.sistema_juridico.Entity.usuario;

import jakarta.persistence.*;
import lombok.*; // Importa todo lombok
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;

// --- CAMBIO 1: QUITAMOS @Data Y USAMOS ESTOS 3 ---
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "usuarios", indexes = {
        @Index(name = "idx_usuarios_email", columnList = "email"),
        @Index(name = "idx_usuarios_rol", columnList = "rol")
})
// --- CAMBIO 2: AGREGAMOS ESTOS 2 PARA EVITAR ERRORES DE GUARDADO ---
@ToString(exclude = "materias")
@EqualsAndHashCode(exclude = "materias")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ... (Mantén tus otros campos: nombreCompleto, email, password, rol, gerencia,
    // activo, fechas) ...
    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password;

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

    @Column(name = "force_password_change")
    @Builder.Default
    private Boolean forcePasswordChange = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Fecha en que el usuario cambió su contraseña por última vez.
     * Se usa para detectar contraseñas vencidas (> 3 meses).
     * NULL = nunca ha cambiado (se inicializa en createdAt).
     */
    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_materias", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "materia_id"))
    @Builder.Default
    private Set<Materia> materias = new HashSet<>();

    public boolean tieneMateria(Integer materiaId) {
        if (this.materias == null)
            return false;
        return this.materias.stream().anyMatch(m -> m.getId().equals(materiaId));
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}