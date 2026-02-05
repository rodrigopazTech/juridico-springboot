package com.juridico.sistema_juridico.Entity.documento;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carpetas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Carpeta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID expedienteId;

    @Column(nullable = false, length = 255)
    private String nombre;

    // Relación jerárquica auto-referencial
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carpeta_padre_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "subcarpetas", "carpetaPadre", "usuarioCreador"})
    private Carpeta carpetaPadre;

    @OneToMany(mappedBy = "carpetaPadre", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "subcarpetas", "carpetaPadre", "usuarioCreador"})
    private List<Carpeta> subcarpetas = new ArrayList<>();

    // Metadatos
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_creador_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "materias"})
    private Usuario usuarioCreador;

    // Orden de visualización
    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    // Indica si es una carpeta del sistema (no se puede eliminar)
    @Column(nullable = false)
    @Builder.Default
    private Boolean esSistema = false;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    // Método helper para obtener la ruta completa
    public String getRutaCompleta() {
        if (carpetaPadre == null) {
            return nombre;
        }
        return carpetaPadre.getRutaCompleta() + " > " + nombre;
    }

    // Método helper para obtener el nivel de profundidad
    public int getNivel() {
        if (carpetaPadre == null) {
            return 0;
        }
        return carpetaPadre.getNivel() + 1;
    }
}
