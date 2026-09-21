package com.juridico.sistema_juridico.dto.response.recordatorios;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RecordatorioResponse {
    private Integer id;
    private String titulo;
    private LocalDate fechaRecordatorio;
    private LocalTime horaRecordatorio;
    private Integer prioridadId;
    private String prioridad;
    private String detalles;
    private boolean completado;
    private Integer recordatorioRevision;
}
