package com.juridico.sistema_juridico.dto.response.expedientes;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO para representar una acción en la línea de tiempo del expediente.
 */
@Data
public class ActividadDTO {
    private LocalDateTime fecha;
    private String titulo;
    private String descripcion;
    private String tipo; // 'upload', 'delete', 'edit', 'status'
}