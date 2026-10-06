package com.juridico.sistema_juridico.service.procesal;

import java.time.LocalDate;

import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;

public record ResultadoCalculoPlazo(
        ModoPlazoTermino modo,
        Integer cantidadConfigurada,
        Long duracionDetectada,
        String tipoDias,
        Integer calendarioVersion,
        String calendarioFechas,
        String reglaComputo,
        LocalDate fechaCalculada,
        LocalDate fechaAdoptada,
        String explicacion) {
}
