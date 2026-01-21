package com.juridico.sistema_juridico.Entity.procesal;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "terminos", indexes = {
    @Index(name = "idx_terminos_expediente", columnList = "expediente_id"),
    @Index(name = "idx_terminos_fecha_vencimiento", columnList = "fecha_vencimiento")
})
public class Termino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String actuacion;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "estatus_termino", length = 30, nullable = false)
    private String estatusTermino; // PROYECTISTA, REVISION, etc.

    @Column(name = "archivo_word", length = 500)
    private String archivoWord;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Prioridad prioridad;
    
    @ManyToOne
    @JoinColumn(name = "abogado_responsable_id")
    private Usuario abogadoResponsable;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getSemaforoColor() {
        // Si ya está terminado, semáforo gris (apagado)
        if ("Concluido".equalsIgnoreCase(this.estatusTermino) || "Presentado".equalsIgnoreCase(this.estatusTermino)) {
            return "bg-gray-400"; 
        }

        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), this.fechaVencimiento);

        if (diasRestantes < 0) return "bg-red-800";      // Vencido (Rojo Oscuro)
        if (diasRestantes <= 3) return "bg-red-500";     // Crítico (Rojo Brillante)
        if (diasRestantes <= 7) return "bg-yellow-400";  // Advertencia (Amarillo)
        return "bg-green-500";                           // A tiempo (Verde)
    }

    
    public String getDiasRestantesTexto() {
        if ("Concluido".equalsIgnoreCase(this.estatusTermino)) return "Término Concluido";
        
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), this.fechaVencimiento);
        
        if (dias < 0) return "Vencido hace " + Math.abs(dias) + " días";
        if (dias == 0) return "¡Vence HOY!";
        if (dias == 1) return "Vence MAÑANA";
        return "Faltan " + dias + " días";
    }
}