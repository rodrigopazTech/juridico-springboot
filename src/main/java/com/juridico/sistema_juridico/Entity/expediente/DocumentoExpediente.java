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
@Table(name = "documentos_expediente", indexes = {
    @Index(name = "idx_documentos_expediente", columnList = "expediente_id")
})
public class DocumentoExpediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Column(name = "nombre_archivo", nullable = false, length = 300)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "tipo_documento", length = 50)
    private String tipoDocumento;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private Usuario uploadedBy;
}