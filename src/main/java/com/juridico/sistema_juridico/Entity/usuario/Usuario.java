package com.juridico.sistema_juridico.Entity.usuario;

import jakarta.persistence.*;
import lombok.*; // Importa todo lombok
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.PermisoSistema;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;

// --- CAMBIO 1: QUITAMOS @Data Y USAMOS ESTOS 3 ---
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "creadoPor"})
@Table(name = "usuarios", indexes = {
        @Index(name = "idx_usuarios_email", columnList = "email"),
        @Index(name = "idx_usuarios_rol", columnList = "rol")
})
// --- CAMBIO 2: AGREGAMOS ESTOS 2 PARA EVITAR ERRORES DE GUARDADO ---
@ToString(exclude = {"materias", "permisosAdicionales", "creadoPor"})
@EqualsAndHashCode(exclude = {"materias", "permisosAdicionales", "creadoPor"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ... (Mantén tus otros campos: nombreCompleto, email, password, rol, gerencia,
    // activo, fechas) ...
    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "username", unique = true, length = 80)
    private String username;

    @Column(name = "numero_empleado", unique = true, length = 40)
    private String numeroEmpleado;

    @Column(name = "puesto", length = 160)
    private String puesto;

    @Column(name = "telefono_institucional", length = 120)
    private String telefonoInstitucional;

    @Column(name = "telefono_movil", length = 80)
    private String telefonoMovil;

    @Column(name = "ubicacion_oficina", length = 180)
    private String ubicacionOficina;

    @Column(name = "microsoft_object_id", unique = true, length = 80)
    private String microsoftObjectId;

    @Column(name = "microsoft_user_principal_name", length = 180)
    private String microsoftUserPrincipalName;

    @Column(name = "microsoft_display_name", length = 200)
    private String microsoftDisplayName;

    @Column(name = "microsoft_job_title", length = 160)
    private String microsoftJobTitle;

    @Column(name = "microsoft_business_phones", length = 300)
    private String microsoftBusinessPhones;

    @Column(name = "microsoft_mobile_phone", length = 80)
    private String microsoftMobilePhone;

    @Column(name = "microsoft_office_location", length = 180)
    private String microsoftOfficeLocation;

    @Column(name = "microsoft_linked_at")
    private LocalDateTime microsoftLinkedAt;

    @Column(name = "microsoft_last_sync_at")
    private LocalDateTime microsoftLastSyncAt;

    @Column(name = "microsoft_login_enabled")
    @Builder.Default
    private Boolean microsoftLoginEnabled = false;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RolUsuario rol;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_gerencias",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "gerencia_id")
    )
    @Builder.Default
    private Set<Gerencia> gerencias = new HashSet<>();

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

    @Column(name = "security_version")
    @Builder.Default
    private Integer securityVersion = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private Usuario creadoPor;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_materias", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "materia_id"))
    @Builder.Default
    private Set<Materia> materias = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_permisos_adicionales", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "permiso", nullable = false, length = 80)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<PermisoSistema> permisosAdicionales = new HashSet<>();

    public boolean tieneMateria(Integer materiaId) {
        if (this.materias == null)
            return false;
        return this.materias.stream().anyMatch(m -> m.getId().equals(materiaId));
    }

    // Helper methods para retrocompatibilidad
    public Gerencia getGerencia() {
        if (this.gerencias == null || this.gerencias.isEmpty()) {
            return null;
        }
        return this.gerencias.iterator().next();
    }

    public void setGerencia(Gerencia gerencia) {
        if (this.gerencias == null) {
            this.gerencias = new HashSet<>();
        }
        this.gerencias.clear();
        if (gerencia != null) {
            this.gerencias.add(gerencia);
        }
    }

    @PrePersist
    protected void onCreate() {
        normalizeIdentityFields();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeIdentityFields();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeIdentityFields() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (username != null) {
            username = username.trim().toLowerCase();
        }
        if (numeroEmpleado != null) {
            numeroEmpleado = numeroEmpleado.trim().toUpperCase();
            if (numeroEmpleado.isBlank()) {
                numeroEmpleado = null;
            }
        }
    }
}
