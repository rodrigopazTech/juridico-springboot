package com.juridico.sistema_juridico.Entity.documento;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documentos")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "expediente_id", nullable = false)
    private UUID expedienteId;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;

    @Column(name = "tamanio_bytes", nullable = false)
    private Long tamanioBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carpeta_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "subcarpetas", "carpetaPadre", "usuarioCreador"})
    private Carpeta carpeta;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_subida_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "materias", "gerencia"})
    private Usuario usuarioSubida;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    // ===== PAPELERA TEMPORAL =====
    /** Indica si el documento está en la papelera (soft-delete) */
    @Column(name = "eliminado", nullable = false)
    @Builder.Default
    private Boolean eliminado = false;

    /** Fecha en que se movió a la papelera. Null = no está eliminado. */
    @Column(name = "fecha_eliminado")
    private LocalDateTime fechaEliminado;

    /** ID de la carpeta original antes de ser eliminado (para restauración). */
    @Column(name = "carpeta_original_id")
    private UUID carpetaOriginalId;

    /** Ruta de la carpeta original (separada por /) para recrear subcarpetas. */
    @Column(name = "carpeta_original_ruta", length = 1024)
    private String carpetaOriginalRuta;

    /** Indica si el documento está eliminado de manera definitiva (invisible en UI pero conservado en BD/filesystem) */
    @Column(name = "eliminado_definitivo", nullable = false)
    @Builder.Default
    private Boolean eliminadoDefinitivo = false;
    // ===========================

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fechaSubida == null) {
            fechaSubida = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
