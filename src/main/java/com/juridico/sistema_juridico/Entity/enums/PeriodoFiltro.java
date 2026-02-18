package com.juridico.sistema_juridico.Entity.enums;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

public enum PeriodoFiltro {
    HOY,
    MANANA,
    SEMANA,
    MES,
    OTRO_MES,
    ANIO,
    TODOS;

    public static LocalDate[] calcularRango(PeriodoFiltro periodo, String mes, Integer anio) {
        int year = (anio != null) ? anio : LocalDate.now().getYear();
        LocalDate hoy = LocalDate.now();
        LocalDate baseDate = hoy;

        // Si se especifica un año diferente al actual para filtros que dependen de la
        // fecha base "anual" (como ANIO u OTRO_MES)
        if (anio != null && anio != hoy.getYear()) {
            baseDate = LocalDate.of(year, 1, 1);
        }

        LocalDate inicio = hoy;
        LocalDate fin = hoy;

        switch (periodo) {
            case MANANA:
                inicio = hoy.plusDays(1);
                fin = hoy.plusDays(1);
                break;
            case SEMANA:
                // Próximos 7 días a partir de hoy (según lógica vista en AudienciasController)
                // OJO: AgendaService usaba lógica de "Semana actual" (Lunes a Domingo).
                // Vamos a estandarizar a lo que dice la UI: "Próx. 7 Días"
                // (AudienciasController)
                // Si preferimos la lógica de AgendaService (Lunes-Domingo), descomentar abajo:
                /*
                 * inicio =
                 * baseDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                 * fin =
                 * baseDate.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
                 */
                // Lógica "Próximos 7 días" (más útil para agenda operativa)
                inicio = hoy;
                fin = hoy.plusDays(7);
                break;
            case MES:
                inicio = hoy.with(TemporalAdjusters.firstDayOfMonth());
                fin = hoy.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case OTRO_MES:
                if (mes != null) {
                    try {
                        int mesInt = Integer.parseInt(mes);
                        YearMonth anioMes = YearMonth.of(year, mesInt);
                        inicio = anioMes.atDay(1);
                        fin = anioMes.atEndOfMonth();
                    } catch (NumberFormatException e) {
                        // Fallback a mes actual si hay error
                        inicio = hoy.with(TemporalAdjusters.firstDayOfMonth());
                        fin = hoy.with(TemporalAdjusters.lastDayOfMonth());
                    }
                }
                break;
            case ANIO:
                inicio = LocalDate.of(year, 1, 1);
                fin = LocalDate.of(year, 12, 31);
                break;
            case TODOS:
                inicio = LocalDate.of(1900, 1, 1);
                fin = LocalDate.of(2100, 12, 31);
                break;
            case HOY:
            default:
                inicio = hoy;
                fin = hoy;
                break;
        }

        return new LocalDate[] { inicio, fin };
    }
}
