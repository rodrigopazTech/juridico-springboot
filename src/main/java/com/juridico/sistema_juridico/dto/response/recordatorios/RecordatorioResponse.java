package com.juridico.sistema_juridico.dto.response.recordatorios;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RecordatorioResponse {
    private Long id;
    private String titulo;
    private LocalDate fecha;
    private LocalTime hora;
    private String prioridad; // urgent, normal
    private String detalles;
}