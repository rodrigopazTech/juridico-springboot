package com.juridico.sistema_juridico.util;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class AudienciaExcelExporter {
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private List<Audiencia> listaAudiencias;

    public AudienciaExcelExporter(List<Audiencia> listaAudiencias) {
        this.listaAudiencias = listaAudiencias;
        workbook = new XSSFWorkbook();
    }

    private void writeHeaderLine() {
        sheet = workbook.createSheet("Audiencias");
        Row row = sheet.createRow(0);
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        style.setFont(font);

        String[] headers = {"Fecha", "Hora", "Expediente", "Gerencia", "Materia", "Tipo Audiencia", "Órgano Jurisdiccional", "Estatus", "Abogado"};
        for (int i = 0; i < headers.length; i++) {
            createCell(row, i, headers[i], style);
        }
    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        Cell cell = row.createCell(columnCount);
        
        if (value == null) {
            cell.setCellValue(""); // Manejo seguro de nulos
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString()); // Usamos toString() para evitar ClassCastException
        }
        cell.setCellStyle(style);
    }

    private void writeDataLines() {
        int rowCount = 1;
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(12);
        style.setFont(font);

        for (Audiencia a : listaAudiencias) {
            Row row = sheet.createRow(rowCount++);
            int col = 0;

            // 1. Fecha y Hora
            String fecha = (a.getFechaAudiencia() != null) ? a.getFechaAudiencia().toString() : "S/F";
            String hora = (a.getHoraAudiencia() != null) ? a.getHoraAudiencia().toString() : "S/H";

            createCell(row, col++, fecha, style);
            createCell(row, col++, hora, style);
            
            // 2. Datos del Expediente
            String expNum = (a.getExpediente() != null) ? a.getExpediente().getNumero() : "N/A";
            String gerencia = (a.getExpediente() != null && a.getExpediente().getGerencia() != null) ? a.getExpediente().getGerencia().getNombre() : "N/A";
            String materia = (a.getExpediente() != null && a.getExpediente().getMateria() != null) ? a.getExpediente().getMateria().getNombre() : "N/A";
            
            createCell(row, col++, expNum, style);
            createCell(row, col++, gerencia, style); 
            createCell(row, col++, materia, style);  

            // 3. Tipo y Órgano Jurisdiccional
            String tipo = (a.getTipoAudiencia() != null) ? a.getTipoAudiencia().getNombre() : "General";
            createCell(row, col++, tipo, style);

            String organo = "N/A";
            if (a.getExpediente() != null) {
                organo = (a.getExpediente().getOrganoJurisdiccional() != null) 
                         ? a.getExpediente().getOrganoJurisdiccional().getNombre() 
                         : (a.getExpediente().getOrganoJurisdiccionalTexto() != null ? a.getExpediente().getOrganoJurisdiccionalTexto() : "N/A");
            }
            createCell(row, col++, organo, style);

            // 4. Estatus
            createCell(row, col++, a.getEstatusAudiencia(), style);

            // 5. Abogado
            String nombreAbogado = "Sin Asignar";
            if (a.getAbogadoComparece() != null) {
                nombreAbogado = a.getAbogadoComparece().getNombreCompleto();
            } else if (a.getExpediente() != null && a.getExpediente().getAbogadoResponsable() != null) {
                nombreAbogado = a.getExpediente().getAbogadoResponsable().getNombreCompleto() + " (Titular)";
            }
            
            createCell(row, col++, nombreAbogado, style);
        }
    }

    public void export(HttpServletResponse response) throws IOException {
        writeHeaderLine();
        writeDataLines();

        // Ajustar el ancho de las columnas (9 columnas)
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }

        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }
}