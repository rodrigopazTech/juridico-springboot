package com.juridico.sistema_juridico.util;

import com.juridico.sistema_juridico.Entity.expediente.ActividadExpediente;
import com.juridico.sistema_juridico.Entity.expediente.BitacoraDato;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.expediente.ExpedienteEtapaPeriodo;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExpedienteTrazabilidadExcelExporter {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Expediente expediente;
    private final List<ExpedienteEtapaPeriodo> periodos;
    private final List<ActividadExpediente> actividades;
    private final List<BitacoraDato> bitacora;
    private final XSSFWorkbook workbook = new XSSFWorkbook();

    public ExpedienteTrazabilidadExcelExporter(Expediente expediente, List<ExpedienteEtapaPeriodo> periodos,
            List<ActividadExpediente> actividades, List<BitacoraDato> bitacora) {
        this.expediente = expediente;
        this.periodos = periodos;
        this.actividades = actividades;
        this.bitacora = bitacora;
    }

    public void export(HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=trazabilidad-expediente-"
                + expediente.getNumero().replaceAll("[^a-zA-Z0-9_-]", "_") + ".xlsx");

        escribirResumen();
        escribirPeriodos();
        escribirHistorial();
        escribirCambiosDatos();

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private void escribirResumen() {
        Sheet sheet = workbook.createSheet("Resumen");
        int rowIndex = 0;
        rowIndex = filaClaveValor(sheet, rowIndex, "Numero de expediente", expediente.getNumero());
        rowIndex = filaClaveValor(sheet, rowIndex, "Etapa actual", nombreEtapa(expediente.getEtapaCatalogo(), expediente.getEtapaProcesal()));
        rowIndex = filaClaveValor(sheet, rowIndex, "Gerencia", expediente.getGerencia() != null ? expediente.getGerencia().getNombre() : "");
        rowIndex = filaClaveValor(sheet, rowIndex, "Materia", expediente.getMateria() != null ? expediente.getMateria().getNombre() : "");
        rowIndex = filaClaveValor(sheet, rowIndex, "Abogado responsable",
                expediente.getAbogadoResponsable() != null ? expediente.getAbogadoResponsable().getNombreCompleto() : "");
        rowIndex = filaClaveValor(sheet, rowIndex, "Total periodos", String.valueOf(periodos.size()));
        filaClaveValor(sheet, rowIndex, "Total movimientos", String.valueOf(actividades.size()));
        autoSize(sheet, 2);
    }

    private void escribirPeriodos() {
        Sheet sheet = workbook.createSheet("Periodos por etapa");
        crearEncabezado(sheet, "Etapa", "Inicio", "Fin", "Duracion", "Inicio por", "Cierre por", "Motivo");
        int rowIndex = 1;
        for (ExpedienteEtapaPeriodo periodo : periodos) {
            Row row = sheet.createRow(rowIndex++);
            int col = 0;
            celda(row, col++, nombreEtapa(periodo.getEtapaCatalogo(), periodo.getEtapaProcesal()));
            celda(row, col++, fecha(periodo.getFechaInicio()));
            celda(row, col++, periodo.getFechaFin() != null ? fecha(periodo.getFechaFin()) : "Activo");
            celda(row, col++, duracion(periodo.getFechaInicio(), periodo.getFechaFin()));
            celda(row, col++, periodo.getUsuarioInicio() != null ? periodo.getUsuarioInicio().getNombreCompleto() : "");
            celda(row, col++, periodo.getUsuarioFin() != null ? periodo.getUsuarioFin().getNombreCompleto() : "");
            celda(row, col, texto(periodo.getMotivoCambio()));
        }
        autoSize(sheet, 7);
    }

    private void escribirHistorial() {
        Sheet sheet = workbook.createSheet("Historial");
        crearEncabezado(sheet, "Fecha", "Etapa", "Periodo", "Tipo", "Titulo", "Descripcion", "Usuario");
        int rowIndex = 1;
        for (ActividadExpediente actividad : actividades) {
            Row row = sheet.createRow(rowIndex++);
            int col = 0;
            celda(row, col++, fecha(actividad.getFechaRegistro()));
            celda(row, col++, nombreEtapa(actividad.getEtapaCatalogo(), actividad.getEtapaProcesal()));
            celda(row, col++, periodoTexto(actividad.getEtapaPeriodo()));
            celda(row, col++, texto(actividad.getTipoIcono()));
            celda(row, col++, texto(actividad.getTitulo()));
            celda(row, col++, texto(actividad.getDescripcion()));
            celda(row, col, actividad.getUsuario() != null ? actividad.getUsuario().getNombreCompleto() : "");
        }
        autoSize(sheet, 7);
    }

    private void escribirCambiosDatos() {
        Sheet sheet = workbook.createSheet("Cambios de datos");
        crearEncabezado(sheet, "Fecha", "Etapa", "Periodo", "Campo", "Valor anterior", "Valor nuevo", "Usuario");
        int rowIndex = 1;
        for (BitacoraDato cambio : bitacora) {
            Row row = sheet.createRow(rowIndex++);
            int col = 0;
            celda(row, col++, fecha(cambio.getFecha()));
            celda(row, col++, nombreEtapa(cambio.getEtapaCatalogo(), cambio.getEtapaProcesal()));
            celda(row, col++, periodoTexto(cambio.getEtapaPeriodo()));
            celda(row, col++, texto(cambio.getCampo()));
            celda(row, col++, texto(cambio.getValorAnterior()));
            celda(row, col++, texto(cambio.getValorNuevo()));
            celda(row, col, cambio.getUsuario() != null ? cambio.getUsuario().getNombreCompleto() : "");
        }
        autoSize(sheet, 7);
    }

    private int filaClaveValor(Sheet sheet, int rowIndex, String clave, String valor) {
        Row row = sheet.createRow(rowIndex++);
        celda(row, 0, clave);
        celda(row, 1, valor);
        return rowIndex;
    }

    private void crearEncabezado(Sheet sheet, String... headers) {
        Row row = sheet.createRow(0);
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void celda(Row row, int column, String value) {
        row.createCell(column).setCellValue(value != null ? value : "");
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String fecha(LocalDateTime fecha) {
        return fecha != null ? fecha.format(FORMATO_FECHA) : "";
    }

    private String duracion(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null) {
            return "";
        }
        LocalDateTime cierre = fin != null ? fin : LocalDateTime.now();
        Duration duration = Duration.between(inicio, cierre);
        long dias = duration.toDays();
        long horas = duration.toHoursPart();
        long minutos = duration.toMinutesPart();
        return dias + "d " + horas + "h " + minutos + "m";
    }

    private String periodoTexto(ExpedienteEtapaPeriodo periodo) {
        if (periodo == null) {
            return "";
        }
        return fecha(periodo.getFechaInicio()) + " - " + (periodo.getFechaFin() != null ? fecha(periodo.getFechaFin()) : "Activo");
    }

    private String texto(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private String nombreEtapa(com.juridico.sistema_juridico.Entity.catalogo.EtapaCatalogo etapaCatalogo, Object etapaLegacy) {
        if (etapaCatalogo != null) {
            return etapaCatalogo.getNombreJerarquico();
        }
        return texto(etapaLegacy);
    }
}
