package com.juridico.sistema_juridico.dto.response.expedientes;

import lombok.Data;

/**
 * DTO para representar archivos adjuntos al expediente.
 */
@Data
public class DocumentoDTO {
    private String nombre;
    private String tipo; // 'PDF', 'Word', 'Anexo'
    private String comentario;
    private String fecha;
    private String folderId; // Para organizar en el explorador
}