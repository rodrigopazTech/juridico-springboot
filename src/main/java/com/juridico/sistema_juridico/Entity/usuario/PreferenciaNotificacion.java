package com.juridico.sistema_juridico.Entity.usuario;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "preferencias_notificacion")
public class PreferenciaNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Builder.Default
    @Column(name = "notificar_nueva_audiencia")
    private boolean notificarNuevaAudiencia = true;

    @Builder.Default
    @Column(name = "notificar_termino_vencer")
    private boolean notificarTerminoVencer = true;

    @Builder.Default
    @Column(name = "notificar_nuevo_termino")
    private boolean notificarNuevoTermino = true;

    @Builder.Default
    @Column(name = "notificar_reasignacion")
    private boolean notificarReasignacion = true;

    /**
     * Preferencia propia de la cuenta. El permiso del navegador se consulta en
     * el cliente y no se almacena aquí, porque puede variar por dispositivo.
     */
    @Builder.Default
    @Column(name = "notificar_en_tiempo_real", columnDefinition = "boolean default true")
    private boolean notificarEnTiempoReal = true;
}
