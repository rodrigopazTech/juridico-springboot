package com.juridico.sistema_juridico.Entity.usuario;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.enums.OutlookSyncStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recordatorios")
public class Recordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String detalles;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_recordatorio", nullable = false)
    private LocalDate fechaRecordatorio;

    @DateTimeFormat(pattern = "HH:mm")
    @Column(name = "hora_recordatorio", nullable = false)
    private LocalTime horaRecordatorio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prioridad_id")
    private CatPrioridad prioridad;

    @Builder.Default
    private Boolean completado = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "outlook_linked")
    @Builder.Default
    private Boolean outlookLinked = false;

    @Column(name = "outlook_event_id", length = 220)
    private String outlookEventId;

    @Column(name = "outlook_calendar_id", length = 120)
    private String outlookCalendarId;

    @Column(name = "outlook_web_link", columnDefinition = "TEXT")
    private String outlookWebLink;

    @Column(name = "outlook_synced_at")
    private LocalDateTime outlookSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outlook_sync_status", length = 40)
    @Builder.Default
    private OutlookSyncStatus outlookSyncStatus = OutlookSyncStatus.NO_VINCULADO;

    @Column(name = "outlook_sync_error", columnDefinition = "TEXT")
    private String outlookSyncError;
}
