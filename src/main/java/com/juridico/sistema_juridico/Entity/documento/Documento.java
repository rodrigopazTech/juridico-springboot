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
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "materias"})
    private Usuario usuarioSubida;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

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
