package com.juridico.sistema_juridico.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "terminos")
public class Termino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el Expediente (guardamos el ID)
    private Long asuntoId; 
    
    // Campos de información
    private String expediente;
    private String actor;        // Actor/Quejoso
    private String asunto;       // La "Actuación" o descripción del término
    private String prestacion;
    private String abogado;
    
    // Estado y Prioridad
    private String estatus;      // Ej: Proyectista, Revisión, Concluido
    private String prioridad;    // Alta, Media, Baja
    
    // Fechas importantes
    private LocalDate fechaIngreso;
    private LocalDate fechaVencimiento;
    
    // Archivos
    private String acuseDocumento; // Nombre del archivo

    // Auditoría (para no borrar físicamente el registro)
    private boolean activo = true;

    // --- CONSTRUCTORES ---
    public Termino() {}

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAsuntoId() { return asuntoId; }
    public void setAsuntoId(Long asuntoId) { this.asuntoId = asuntoId; }
    public String getExpediente() { return expediente; }
    public void setExpediente(String expediente) { this.expediente = expediente; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public String getPrestacion() { return prestacion; }
    public void setPrestacion(String prestacion) { this.prestacion = prestacion; }
    public String getAbogado() { return abogado; }
    public void setAbogado(String abogado) { this.abogado = abogado; }
    public String getEstatus() { return estatus; }
    public void setEstatus(String estatus) { this.estatus = estatus; }
    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public String getAcuseDocumento() { return acuseDocumento; }
    public void setAcuseDocumento(String acuseDocumento) { this.acuseDocumento = acuseDocumento; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}