package com.juridico.sistema_juridico.Entity.procesal;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.TipoDocumentoTermino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "termino_archivos", indexes = {
        @Index(name = "idx_termino_archivos_termino", columnList = "termino_id"),
        @Index(name = "idx_termino_archivos_tipo", columnList = "tipo_documento"),
        @Index(name = "idx_termino_archivos_fecha", columnList = "created_at")
})
public class TerminoArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "termino_id", nullable = false)
    private Termino termino;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 20, nullable = false)
    private TipoDocumentoTermino tipoDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa", length = 30, nullable = false)
    private EstatusTermino etapa;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "ruta_archivo", nullable = false, length = 700)
    private String rutaArchivo;

    @Column(name = "tipo_mime", length = 120)
    private String tipoMime;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_subida_id")
    private Usuario usuarioSubida;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
