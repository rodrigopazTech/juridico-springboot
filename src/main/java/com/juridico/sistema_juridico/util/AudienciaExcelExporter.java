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

        String[] headers = {"Fecha", "Hora", "Expediente", "Gerencia", "Materia", "Tipo Audiencia", "Ubicación", "Estatus", "Abogado"};
        for (int i = 0; i < headers.length; i++) {
            createCell(row, i, headers[i], style);
        }
    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        sheet.autoSizeColumn(columnCount);
        Cell cell = row.createCell(columnCount);
        if (value instanceof Integer) cell.setCellValue((Integer) value);
        else if (value instanceof Boolean) cell.setCellValue((Boolean) value);
        else cell.setCellValue((String) value);
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

            createCell(row, col++, a.getFechaAudiencia().toString(), style);
            createCell(row, col++, a.getHoraAudiencia().toString(), style);
            
            // Datos del Expediente
            String expNum = (a.getExpediente() != null) ? a.getExpediente().getNumero() : "N/A";
            String gerencia = (a.getExpediente() != null && a.getExpediente().getGerencia() != null) ? a.getExpediente().getGerencia().getNombre() : "N/A";
            String materia = (a.getExpediente() != null && a.getExpediente().getMateria() != null) ? a.getExpediente().getMateria().getNombre() : "N/A";
            
            createCell(row, col++, expNum, style);
            createCell(row, col++, gerencia, style); // COLUMNA NUEVA
            createCell(row, col++, materia, style);  // COLUMNA NUEVA

            String tipo = (a.getTipoAudiencia() != null) ? a.getTipoAudiencia().getNombre() : "General";
            createCell(row, col++, tipo, style);

            String ubicacion = a.getEsVirtual() ? "VIRTUAL: " + a.getUrlReunion() : "SALA: " + a.getSalaLugar();
            createCell(row, col++, ubicacion, style);

            createCell(row, col++, a.getEstatusAudiencia(), style);
            createCell(row, col++, a.getAbogadoComparece(), style);
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