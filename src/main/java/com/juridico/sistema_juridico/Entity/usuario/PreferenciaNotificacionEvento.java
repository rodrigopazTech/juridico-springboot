package com.juridico.sistema_juridico.Entity.usuario;

import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "preferencias_notificacion_evento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pref_notif_evento_usuario", columnNames = { "usuario_id", "evento" })
})
public class PreferenciaNotificacionEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 80)
    private NotificacionEvento evento;

    @Builder.Default
    @Column(name = "habilitada", nullable = false)
    private boolean habilitada = true;
}
