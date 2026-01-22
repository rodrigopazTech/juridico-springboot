package com.juridico.sistema_juridico.util;

import com.juridico.sistema_juridico.Entity.procesal.Termino;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.List;

public class TerminoExcelExporter {
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private List<Termino> listaTerminos;

    public TerminoExcelExporter(List<Termino> listaTerminos) {
        this.listaTerminos = listaTerminos;
        workbook = new XSSFWorkbook();
    }

    private void writeHeaderLine() {
        sheet = workbook.createSheet("Términos");

        Row row = sheet.createRow(0);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        style.setFont(font);

        createCell(row, 0, "Expediente", style);
        createCell(row, 1, "Asunto / Actuación", style);
        createCell(row, 2, "Vencimiento", style);
        createCell(row, 3, "Prioridad", style);
        createCell(row, 4, "Estatus", style);
        createCell(row, 5, "Abogado", style);
    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        sheet.autoSizeColumn(columnCount);
        Cell cell = row.createCell(columnCount);
        if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue((String) value);
        }
        cell.setCellStyle(style);
    }

    private void writeDataLines() {
        int rowCount = 1;

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(12);
        style.setFont(font);

        for (Termino t : listaTerminos) {
            Row row = sheet.createRow(rowCount++);
            int columnCount = 0;

            String expedienteInfo = (t.getExpediente() != null) ? t.getExpediente().getNumero() : "N/A";
            createCell(row, columnCount++, expedienteInfo, style);
            
            createCell(row, columnCount++, t.getActuacion(), style);
            createCell(row, columnCount++, t.getFechaVencimiento().toString(), style);
            
            String prioridadStr = (t.getPrioridad() != null) ? t.getPrioridad().name() : "MEDIA";
            createCell(row, columnCount++, prioridadStr, style);
            
            createCell(row, columnCount++, t.getEstatusTermino(), style);

            String abogadoNombre = (t.getAbogadoResponsable() != null) ? t.getAbogadoResponsable().getNombreCompleto() : "Sin Asignar";
            createCell(row, columnCount++, abogadoNombre, style);
        }
    }

    public void export(HttpServletResponse response) throws IOException {
        writeHeaderLine();
        writeDataLines();

        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }
}