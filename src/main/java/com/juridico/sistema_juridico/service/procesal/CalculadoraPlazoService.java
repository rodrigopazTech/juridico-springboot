package com.juridico.sistema_juridico.service.procesal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.exception.BadRequestException;

@Service
public class CalculadoraPlazoService {

    public LocalDate resolverVencimiento(TipoTermino tipo, LocalDate fechaOrigen,
            LocalDate fechaVencimientoCapturada) {
        if (tipo == null || tipo.getModoPlazo() == null) {
            throw new BadRequestException("El Tipo de Término no tiene un modo de plazo configurado.");
        }
        if (fechaOrigen == null) {
            throw new BadRequestException("La fecha origen es obligatoria.");
        }

        return switch (tipo.getModoPlazo()) {
            case SIN_VENCIMIENTO -> null;
            case CAPTURADO -> validarCapturado(fechaOrigen, fechaVencimientoCapturada);
            case CALCULADO -> calcular(fechaOrigen, tipo.getDiasPredeterminados(), tipo.getTipoDias());
        };
    }

    private LocalDate validarCapturado(LocalDate fechaOrigen, LocalDate vencimiento) {
        if (vencimiento == null) {
            throw new BadRequestException("Captura la fecha de vencimiento para este Tipo de Término.");
        }
        if (vencimiento.isBefore(fechaOrigen)) {
            throw new BadRequestException("La fecha de vencimiento no puede ser anterior a la fecha origen.");
        }
        return vencimiento;
    }

    private LocalDate calcular(LocalDate origen, Integer dias, String tipoDias) {
        if (dias == null || dias <= 0) {
            throw new BadRequestException("El Tipo de Término no tiene una cantidad de días válida.");
        }
        if (!"HABILES".equalsIgnoreCase(tipoDias)) {
            return origen.plusDays(dias);
        }

        LocalDate resultado = origen;
        int pendientes = dias;
        while (pendientes > 0) {
            resultado = resultado.plusDays(1);
            DayOfWeek dia = resultado.getDayOfWeek();
            if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) {
                pendientes--;
            }
        }
        return resultado;
    }

    public long diasEntre(LocalDate origen, LocalDate destino, String tipoDias) {
        if (origen == null || destino == null) return 0;
        if (!"HABILES".equalsIgnoreCase(tipoDias)) {
            return ChronoUnit.DAYS.between(origen, destino);
        }
        if (origen.equals(destino)) return 0;

        int signo = destino.isAfter(origen) ? 1 : -1;
        LocalDate cursor = origen;
        long dias = 0;
        while (!cursor.equals(destino)) {
            cursor = cursor.plusDays(signo);
            DayOfWeek dia = cursor.getDayOfWeek();
            if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) dias += signo;
        }
        return dias;
    }
}
