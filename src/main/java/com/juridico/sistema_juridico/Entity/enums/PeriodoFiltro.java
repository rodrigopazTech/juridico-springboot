package com.juridico.sistema_juridico.Entity.enums;

import java.time.LocalDate;

public enum PeriodoFiltro {
    HOY,
    MANANA,
    SEMANA,
    MES;

    /**
     * Calcula y retorna el rango de fechas basado en el periodo seleccionado.
     * 
     * @return Arreglo de LocalDate [fechaInicio, fechaFin].
     */
    public LocalDate[] calcularRango() {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaInicio = LocalDate.of(1900, 1, 1);
        LocalDate fechaFin = LocalDate.of(2100, 12, 31);

        switch (this) {
            case HOY:
                fechaInicio = hoy;
                fechaFin = hoy;
                break;
            case MANANA:
                fechaInicio = hoy.plusDays(1);
                fechaFin = hoy.plusDays(1);
                break;
            case SEMANA:
                fechaInicio = hoy;
                fechaFin = hoy.plusDays(7);
                break;
            case MES:
                fechaInicio = hoy.withDayOfMonth(1);
                fechaFin = hoy.withDayOfMonth(hoy.lengthOfMonth());
                break;
        }

        return new LocalDate[] { fechaInicio, fechaFin };
    }
}
