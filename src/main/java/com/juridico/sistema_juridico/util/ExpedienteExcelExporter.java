package com.juridico.sistema_juridico.util;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.List;

public class ExpedienteExcelExporter {
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private List<Expediente> listaExpedientes;

    public ExpedienteExcelExporter(List<Expediente> listaExpedientes) {
        this.listaExpedientes = listaExpedientes;
        workbook = new XSSFWorkbook();
    }

    private void writeHeaderLine() {
        sheet = workbook.createSheet("Expedientes");
        Row row = sheet.createRow(0);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        style.setFont(font);

        String[] headers = {"Número de Expediente", "Gerencia", "Materia", "Abogado Responsable", "Etapa Procesal", "Partes", "Descripción", "Prioridad", "Órgano Jurisdiccional"};
        for (int i = 0; i < headers.length; i++) {
            createCell(row, i, headers[i], style);
        }
    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        Cell cell = row.createCell(columnCount);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }

    private void writeDataLines() {
        int rowCount = 1;

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(12);
        style.setFont(font);

        for (Expediente e : listaExpedientes) {
            Row row = sheet.createRow(rowCount++);
            int columnCount = 0;

            createCell(row, columnCount++, e.getNumero(), style);
            createCell(row, columnCount++, e.getGerencia() != null ? e.getGerencia().getNombre() : "N/A", style);
            createCell(row, columnCount++, e.getMateria() != null ? e.getMateria().getNombre() : "N/A", style);
            
            String abogado = e.getAbogadoResponsable() != null ? e.getAbogadoResponsable().getNombreCompleto() : e.getAbogadoResponsableNombre();
            createCell(row, columnCount++, abogado != null ? abogado : "Sin Asignar", style);
            
            createCell(row, columnCount++, e.getEtapaProcesal(), style);
            createCell(row, columnCount++, e.getPartes(), style);
            createCell(row, columnCount++, e.getDescripcion(), style);
            createCell(row, columnCount++, e.getPrioridad() != null ? e.getPrioridad().getNombre() : "N/A", style);
            
            String organo = e.getOrganoJurisdiccional() != null ? e.getOrganoJurisdiccional().getNombre() : e.getOrganoJurisdiccionalTexto();
            createCell(row, columnCount++, organo != null ? organo : "N/A", style);
            
        }
    }

    public void export(HttpServletResponse response) throws IOException {
        writeHeaderLine();
        writeDataLines();

        // Ajustar el ancho de las columnas (Optimizado: una sola vez al final)
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }

        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }
}
