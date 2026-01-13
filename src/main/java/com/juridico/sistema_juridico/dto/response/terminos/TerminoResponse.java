package com.juridico.sistema_juridico.dto.response.terminos;

import lombok.Data;
import java.time.LocalDate;

/**
 * DTO para representar términos presentados en la Agenda General.
 */
@Data
public class TerminoResponse {
    private Long id;
    private String expediente;
    private String actuación;
    private LocalDate fechaVencimiento;
    private LocalDate fechaPresentacion;
    private String acuseUrl;
    private String abogadoResponsable;
}