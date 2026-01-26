package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.repository.procesal.AudienciaDesahogadaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

@Service
public class AgendaService {

    @Autowired private AudienciaDesahogadaRepository desahogadaRepository;
    @Autowired private TerminoRepository terminoRepository;

    // Modificamos para recibir 'anio' también
    public List<AudienciaDesahogada> getAudienciasPorFiltro(String filtro, String mes, Integer anio) {
        LocalDate[] rango = calcularRango(filtro, mes, anio);
        return desahogadaRepository.findByFechaDesahogoBetweenOrderByFechaDesahogoDesc(rango[0], rango[1]);
    }

    public List<Termino> getTerminosPorFiltro(String filtro, String mes, Integer anio) {
        LocalDate[] rango = calcularRango(filtro, mes, anio);
        List<String> estatusFinales = Arrays.asList("Presentado", "Concluido");
        return terminoRepository.findByEstatusTerminoInAndFechaVencimientoBetweenOrderByFechaVencimientoDesc(estatusFinales, rango[0], rango[1]);
    }

    private LocalDate[] calcularRango(String filtro, String mes, Integer anio) {
        // Si no viene año, usamos el actual
        int year = (anio != null) ? anio : LocalDate.now().getYear();
        LocalDate baseDate = LocalDate.of(year, LocalDate.now().getMonth(), LocalDate.now().getDayOfMonth());
        
        // Si el año seleccionado es diferente al actual, ajustamos "hoy" a ese año (mismo día/mes si existe, o día 1)
        if (anio != null && anio != LocalDate.now().getYear()) {
             baseDate = LocalDate.of(year, 1, 1); // Reset para evitar errores en años bisiestos o fechas futuras
        } else {
             baseDate = LocalDate.now();
        }

        LocalDate inicio = baseDate;
        LocalDate fin = baseDate;

        if ("hoy".equals(filtro)) {
            // Si piden "hoy" pero de un año pasado, mostramos todo ese día específico
            // Pero "hoy" suele tener sentido solo para el presente. 
            // Para históricos, mejor forzamos a ver el "mes" o "año completo" si se selecciona un año pasado.
            if (anio != null && anio != LocalDate.now().getYear()) {
                // Estrategia: Si cambia de año, el filtro "hoy" se convierte en "todo el año" o se resetea.
                // Para simplificar: "Hoy" siempre es HOY real. Si quieres ver 2024, usas filtro mes o año.
                inicio = LocalDate.now();
                fin = LocalDate.now();
            }
        } 
        else if ("semana".equals(filtro)) {
            inicio = baseDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            fin = baseDate.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        } 
        else if ("mes".equals(filtro)) {
            inicio = baseDate.with(TemporalAdjusters.firstDayOfMonth());
            fin = baseDate.with(TemporalAdjusters.lastDayOfMonth());
        } 
        else if ("otro-mes".equals(filtro) && mes != null) {
            int mesInt = Integer.parseInt(mes);
            YearMonth anioMes = YearMonth.of(year, mesInt);
            inicio = anioMes.atDay(1);
            fin = anioMes.atEndOfMonth();
        }
        else if ("anio".equals(filtro)) { // NUEVO FILTRO: Año Completo
            inicio = LocalDate.of(year, 1, 1);
            fin = LocalDate.of(year, 12, 31);
        }

        return new LocalDate[]{inicio, fin};
    }

}