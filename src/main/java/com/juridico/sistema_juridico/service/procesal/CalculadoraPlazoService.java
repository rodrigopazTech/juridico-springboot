package com.juridico.sistema_juridico.service.procesal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.service.configuracion.CalendarioInstitucionalService;
import com.juridico.sistema_juridico.service.configuracion.CalendarioSnapshot;

@Service
public class CalculadoraPlazoService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final CalendarioInstitucionalService calendario;

    public CalculadoraPlazoService() {
        this.calendario = null;
    }

    @Autowired
    public CalculadoraPlazoService(CalendarioInstitucionalService calendario) {
        this.calendario = calendario;
    }

    public ResultadoCalculoPlazo calcularResultado(TipoTermino tipo, LocalDate fechaOrigen,
            LocalDate fechaVencimientoCapturada) {
        validarBase(tipo, fechaOrigen);
        CalendarioSnapshot snapshot = calendario != null
                ? calendario.snapshotActual()
                : new CalendarioSnapshot(0, Set.of());
        String tipoDias = normalizarTipoDias(tipo.getTipoDias());

        return switch (tipo.getModoPlazo()) {
            case SIN_VENCIMIENTO -> new ResultadoCalculoPlazo(
                    ModoPlazoTermino.SIN_VENCIMIENTO, null, null, tipoDias,
                    snapshot.version(), serializar(snapshot.fechasInhabiles()),
                    "SIN_VENCIMIENTO", null, null,
                    "Este Tipo de Término no tiene fecha de vencimiento.");
            case CAPTURADO -> resultadoCapturado(fechaOrigen, fechaVencimientoCapturada, snapshot, tipoDias);
            case CALCULADO -> resultadoCalculado(tipo, fechaOrigen, snapshot, tipoDias);
        };
    }

    public LocalDate resolverVencimiento(TipoTermino tipo, LocalDate fechaOrigen,
            LocalDate fechaVencimientoCapturada) {
        return calcularResultado(tipo, fechaOrigen, fechaVencimientoCapturada).fechaAdoptada();
    }

    public void aplicarSnapshot(Termino termino, ResultadoCalculoPlazo resultado) {
        termino.setFechaVencimiento(resultado.fechaAdoptada());
        termino.setPlazoModoSnapshot(resultado.modo());
        termino.setPlazoCantidadSnapshot(resultado.cantidadConfigurada());
        termino.setPlazoDuracionDetectada(resultado.duracionDetectada());
        termino.setPlazoDuracionAdoptada(resultado.duracionDetectada());
        termino.setPlazoTipoDiasSnapshot(resultado.tipoDias());
        termino.setTipoDiasCalculado(resultado.tipoDias());
        termino.setPlazoCalendarioVersion(resultado.calendarioVersion());
        termino.setPlazoCalendarioFechas(resultado.calendarioFechas());
        termino.setPlazoReglaComputo(resultado.reglaComputo());
        termino.setPlazoFechaCalculada(resultado.fechaCalculada());
        termino.setPlazoFechaAdoptada(resultado.fechaAdoptada());
        termino.setPlazoExplicacion(resultado.explicacion());
        termino.setDesgloseDiasTexto(resultado.explicacion());
        termino.setEsTiempoDinamico(resultado.modo() == ModoPlazoTermino.CAPTURADO);
    }

    public ResultadoCalculoPlazo reproducirDesdeSnapshot(Termino termino) {
        if (termino == null || termino.getPlazoModoSnapshot() == null) {
            throw new BadRequestException("El Término no contiene un snapshot de plazo reproducible.");
        }
        LocalDate origen = termino.getFechaIngreso() != null ? termino.getFechaIngreso() : termino.getFechaCreacion();
        Set<LocalDate> fechas = deserializar(termino.getPlazoCalendarioFechas());
        LocalDate calculada = termino.getPlazoFechaCalculada();
        if (termino.getPlazoModoSnapshot() == ModoPlazoTermino.CALCULADO) {
            if (origen == null || termino.getPlazoCantidadSnapshot() == null) {
                throw new BadRequestException("El snapshot no contiene fecha origen o cantidad de días.");
            }
            calculada = calcular(origen, termino.getPlazoCantidadSnapshot(),
                    termino.getPlazoTipoDiasSnapshot(), fechas);
        }
        boolean ajusta = termino.getPlazoReglaComputo() != null
                && termino.getPlazoReglaComputo().contains("AJUSTE_SIGUIENTE_HABIL");
        LocalDate adoptada = calculada != null && ajusta ? siguienteHabil(calculada, fechas) : calculada;
        return new ResultadoCalculoPlazo(termino.getPlazoModoSnapshot(), termino.getPlazoCantidadSnapshot(),
                termino.getPlazoDuracionDetectada(), termino.getPlazoTipoDiasSnapshot(),
                termino.getPlazoCalendarioVersion(), termino.getPlazoCalendarioFechas(),
                termino.getPlazoReglaComputo(), calculada, adoptada, termino.getPlazoExplicacion());
    }

    public long diasEntre(LocalDate origen, LocalDate destino, String tipoDias) {
        Set<LocalDate> fechas = calendario != null
                ? calendario.snapshotActual().fechasInhabiles()
                : Set.of();
        return diasEntre(origen, destino, tipoDias, fechas);
    }

    public long diasEntreSnapshot(Termino termino, LocalDate origen, LocalDate destino) {
        Set<LocalDate> fechas = deserializar(termino.getPlazoCalendarioFechas());
        String tipoDias = termino.getPlazoTipoDiasSnapshot() != null
                ? termino.getPlazoTipoDiasSnapshot() : termino.getTipoDiasCalculado();
        return diasEntre(origen, destino, tipoDias, fechas);
    }

    public long diasEntre(LocalDate origen, LocalDate destino, String tipoDias, Set<LocalDate> fechasInhabiles) {
        if (origen == null || destino == null) return 0;
        if (!"HABILES".equalsIgnoreCase(tipoDias)) {
            return java.time.temporal.ChronoUnit.DAYS.between(origen, destino);
        }
        if (origen.equals(destino)) return 0;

        int signo = destino.isAfter(origen) ? 1 : -1;
        LocalDate cursor = origen;
        long dias = 0;
        while (!cursor.equals(destino)) {
            cursor = cursor.plusDays(signo);
            if (esHabil(cursor, fechasInhabiles)) dias += signo;
        }
        return dias;
    }

    private ResultadoCalculoPlazo resultadoCalculado(TipoTermino tipo, LocalDate origen,
            CalendarioSnapshot snapshot, String tipoDias) {
        Integer cantidad = tipo.getDiasPredeterminados();
        if (cantidad == null || cantidad <= 0) {
            throw new BadRequestException("El Tipo de Término no tiene una cantidad de días válida.");
        }
        LocalDate calculada = calcular(origen, cantidad, tipoDias, snapshot.fechasInhabiles());
        boolean ajustar = Boolean.TRUE.equals(tipo.getAjustaAlSiguienteHabil());
        LocalDate adoptada = ajustar ? siguienteHabil(calculada, snapshot.fechasInhabiles()) : calculada;
        long excluidos = "HABILES".equals(tipoDias) || ajustar
                ? contarDiasNoHabiles(origen, adoptada, snapshot.fechasInhabiles()) : 0;
        String unidad = "HABILES".equals(tipoDias) ? "días hábiles" : "días naturales";
        String explicacion = "Vence el " + FECHA.format(adoptada) + ". Se calcularon " + cantidad + " "
                + unidad + " desde el " + FECHA.format(origen) + ", sin contar el día origen."
                + (excluidos > 0 ? " Se excluyeron " + excluidos + " día(s) no hábil(es), incluidos fines de semana y fechas del calendario v"
                        + snapshot.version() + "." : " No hubo días no hábiles que excluir. Calendario institucional v" + snapshot.version() + ".")
                + (!adoptada.equals(calculada) ? " La fecha calculada " + FECHA.format(calculada)
                        + " se ajustó al siguiente día hábil." : "");
        return new ResultadoCalculoPlazo(ModoPlazoTermino.CALCULADO, cantidad, (long) cantidad,
                tipoDias, snapshot.version(), serializar(snapshot.fechasInhabiles()),
                "ORIGEN_NO_CUENTA;" + tipoDias + (ajustar ? ";AJUSTE_SIGUIENTE_HABIL" : ""),
                calculada, adoptada, explicacion);
    }

    private ResultadoCalculoPlazo resultadoCapturado(LocalDate origen, LocalDate vencimiento,
            CalendarioSnapshot snapshot, String tipoDias) {
        if (vencimiento == null) {
            throw new BadRequestException("Captura la fecha de vencimiento para este Tipo de Término.");
        }
        if (vencimiento.isBefore(origen)) {
            throw new BadRequestException("La fecha de vencimiento no puede ser anterior a la fecha origen.");
        }
        long duracion = diasEntre(origen, vencimiento, tipoDias, snapshot.fechasInhabiles());
        String unidad = "HABILES".equals(tipoDias) ? "días hábiles" : "días naturales";
        String explicacion = "Vence el " + FECHA.format(vencimiento) + ". La fecha fue informada al registrar "
                + "el Término y comprende " + duracion + " " + unidad + " desde el " + FECHA.format(origen)
                + ", sin contar el día origen. Calendario institucional v" + snapshot.version() + ".";
        return new ResultadoCalculoPlazo(ModoPlazoTermino.CAPTURADO, null, duracion, tipoDias,
                snapshot.version(), serializar(snapshot.fechasInhabiles()), "FECHA_CAPTURADA;" + tipoDias,
                vencimiento, vencimiento, explicacion);
    }

    private void validarBase(TipoTermino tipo, LocalDate fechaOrigen) {
        if (tipo == null || tipo.getModoPlazo() == null) {
            throw new BadRequestException("El Tipo de Término no tiene un modo de plazo configurado.");
        }
        if (fechaOrigen == null) {
            throw new BadRequestException("La fecha origen es obligatoria.");
        }
    }

    private LocalDate calcular(LocalDate origen, int dias, String tipoDias, Set<LocalDate> fechasInhabiles) {
        if (!"HABILES".equals(tipoDias)) return origen.plusDays(dias);
        LocalDate resultado = origen;
        int pendientes = dias;
        while (pendientes > 0) {
            resultado = resultado.plusDays(1);
            if (esHabil(resultado, fechasInhabiles)) pendientes--;
        }
        return resultado;
    }

    private LocalDate siguienteHabil(LocalDate fecha, Set<LocalDate> fechasInhabiles) {
        LocalDate resultado = fecha;
        while (!esHabil(resultado, fechasInhabiles)) resultado = resultado.plusDays(1);
        return resultado;
    }

    private boolean esHabil(LocalDate fecha, Set<LocalDate> fechasInhabiles) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY
                && (fechasInhabiles == null || !fechasInhabiles.contains(fecha));
    }

    private long contarDiasNoHabiles(LocalDate origen, LocalDate destino, Set<LocalDate> fechasInhabiles) {
        if (origen == null || destino == null || !destino.isAfter(origen)) return 0;
        long excluidos = 0;
        LocalDate cursor = origen.plusDays(1);
        while (!cursor.isAfter(destino)) {
            if (!esHabil(cursor, fechasInhabiles)) excluidos++;
            cursor = cursor.plusDays(1);
        }
        return excluidos;
    }

    private String normalizarTipoDias(String tipoDias) {
        return "HABILES".equalsIgnoreCase(tipoDias) ? "HABILES" : "NATURALES";
    }

    private String serializar(Set<LocalDate> fechas) {
        if (calendario != null) return calendario.serializar(fechas);
        return fechas == null ? "" : fechas.stream().sorted().map(LocalDate::toString)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private Set<LocalDate> deserializar(String fechas) {
        if (calendario != null) return calendario.deserializar(fechas);
        if (fechas == null || fechas.isBlank()) return Set.of();
        Set<LocalDate> resultado = new LinkedHashSet<>();
        for (String fecha : fechas.split(",")) resultado.add(LocalDate.parse(fecha.trim()));
        return resultado;
    }
}
