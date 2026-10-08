package com.juridico.sistema_juridico.dto.response.configuracion;

import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionCorreoSmtp;
import com.juridico.sistema_juridico.Entity.enums.EstadoConfiguracionCorreo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConfiguracionCorreoSmtpView(
        Long id,
        EstadoConfiguracionCorreo estado,
        String correoPrueba,
        LocalDate proximaFechaCambio,
        Boolean pruebaExitosa,
        String ultimoResultadoPrueba,
        LocalDateTime fechaPrueba,
        String probadaPor,
        LocalDateTime fechaActivacion,
        String activadaPor,
        String creadaPor,
        LocalDateTime fechaCreacion) {

    public static ConfiguracionCorreoSmtpView from(ConfiguracionCorreoSmtp entity) {
        return new ConfiguracionCorreoSmtpView(
                entity.getId(), entity.getEstado(), entity.getCorreoPrueba(), entity.getProximaFechaCambio(),
                entity.getPruebaExitosa(), entity.getUltimoResultadoPrueba(), entity.getFechaPrueba(),
                entity.getProbadaPor(), entity.getFechaActivacion(), entity.getActivadaPor(),
                entity.getCreadaPor(), entity.getFechaCreacion());
    }
}
