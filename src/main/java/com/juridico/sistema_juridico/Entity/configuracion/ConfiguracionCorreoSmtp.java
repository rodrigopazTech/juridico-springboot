package com.juridico.sistema_juridico.Entity.configuracion;

import com.juridico.sistema_juridico.Entity.enums.EstadoConfiguracionCorreo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "configuraciones_correo_smtp", indexes = {
        @Index(name = "idx_config_correo_estado", columnList = "estado"),
        @Index(name = "idx_config_correo_creacion", columnList = "fecha_creacion")
})
public class ConfiguracionCorreoSmtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "password_cifrado", nullable = false, columnDefinition = "TEXT")
    private String passwordCifrado;

    @Column(name = "correo_prueba", nullable = false, length = 254)
    private String correoPrueba;

    @Column(name = "proxima_fecha_cambio", nullable = false)
    private LocalDate proximaFechaCambio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConfiguracionCorreo estado;

    @Column(name = "prueba_exitosa")
    private Boolean pruebaExitosa;

    @Column(name = "ultimo_resultado_prueba", length = 500)
    private String ultimoResultadoPrueba;

    @Column(name = "fecha_prueba")
    private LocalDateTime fechaPrueba;

    @Column(name = "probada_por", length = 120)
    private String probadaPor;

    @Column(name = "fecha_activacion")
    private LocalDateTime fechaActivacion;

    @Column(name = "activada_por", length = 120)
    private String activadaPor;

    @Column(name = "fecha_retiro")
    private LocalDateTime fechaRetiro;

    @Column(name = "creada_por", nullable = false, length = 120)
    private String creadaPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
        if (estado == null) {
            estado = EstadoConfiguracionCorreo.PENDIENTE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
