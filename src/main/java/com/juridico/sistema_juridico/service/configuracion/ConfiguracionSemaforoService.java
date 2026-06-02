package com.juridico.sistema_juridico.service.configuracion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionSemaforo;
import com.juridico.sistema_juridico.Entity.enums.NivelConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.dto.response.configuracion.ConfiguracionSemaforoResponse;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.repository.configuracion.ConfiguracionSemaforoRepository;

@Service
public class ConfiguracionSemaforoService {

    private final ConfiguracionSemaforoRepository configuracionSemaforoRepository;

    public ConfiguracionSemaforoService(ConfiguracionSemaforoRepository configuracionSemaforoRepository) {
        this.configuracionSemaforoRepository = configuracionSemaforoRepository;
    }

    public ConfiguracionSemaforoResponse obtener(TipoEventoAlerta tipoEvento, Gerencia gerencia) {
        ConfiguracionSemaforo configuracion = gerencia == null ? null
                : configuracionSemaforoRepository.findFirstByTipoEventoAndNivelAndGerenciaId(
                        tipoEvento, NivelConfiguracionAlerta.GERENCIA, gerencia.getId()).orElse(null);
        if (configuracion == null) {
            configuracion = configuracionSemaforoRepository.findFirstByTipoEventoAndNivelAndGerenciaIsNull(
                    tipoEvento, NivelConfiguracionAlerta.SISTEMA).orElse(null);
        }
        if (configuracion == null) {
            return valoresPredeterminados(tipoEvento);
        }
        return crearRespuesta(configuracion);
    }

    @Transactional
    public ConfiguracionSemaforoResponse guardar(TipoEventoAlerta tipoEvento, NivelConfiguracionAlerta nivel,
            Gerencia gerencia, Integer rojoHasta, Integer amarilloHasta) {
        if (nivel != NivelConfiguracionAlerta.SISTEMA && nivel != NivelConfiguracionAlerta.GERENCIA) {
            throw new BadRequestException("Los semáforos solo se configuran a nivel sistema o gerencia.");
        }
        if (nivel == NivelConfiguracionAlerta.GERENCIA && gerencia == null) {
            throw new BadRequestException("La gerencia es obligatoria.");
        }
        if (rojoHasta == null || amarilloHasta == null || rojoHasta < 0 || amarilloHasta <= rojoHasta) {
            throw new BadRequestException("El rango amarillo debe ser mayor que el rango rojo y ambos deben ser positivos.");
        }
        ConfiguracionSemaforo configuracion = nivel == NivelConfiguracionAlerta.SISTEMA
                ? configuracionSemaforoRepository.findFirstByTipoEventoAndNivelAndGerenciaIsNull(tipoEvento, nivel)
                        .orElseGet(ConfiguracionSemaforo::new)
                : configuracionSemaforoRepository.findFirstByTipoEventoAndNivelAndGerenciaId(
                        tipoEvento, nivel, gerencia.getId()).orElseGet(ConfiguracionSemaforo::new);
        configuracion.setTipoEvento(tipoEvento);
        configuracion.setNivel(nivel);
        configuracion.setGerencia(nivel == NivelConfiguracionAlerta.GERENCIA ? gerencia : null);
        configuracion.setRojoHasta(rojoHasta);
        configuracion.setAmarilloHasta(amarilloHasta);
        return crearRespuesta(configuracionSemaforoRepository.save(configuracion));
    }

    public void aplicarATerminos(List<Termino> terminos) {
        terminos.forEach(termino -> {
            ConfiguracionSemaforoResponse configuracion = obtener(TipoEventoAlerta.TERMINO, termino.getGerenciaReferencia());
            termino.setSemaforoRojoHasta(configuracion.rojoHasta());
            termino.setSemaforoAmarilloHasta(configuracion.amarilloHasta());
        });
    }

    private ConfiguracionSemaforoResponse valoresPredeterminados(TipoEventoAlerta tipoEvento) {
        return tipoEvento == TipoEventoAlerta.TERMINO
                ? new ConfiguracionSemaforoResponse(tipoEvento, NivelConfiguracionAlerta.SISTEMA, null, 3, 7)
                : new ConfiguracionSemaforoResponse(tipoEvento, NivelConfiguracionAlerta.SISTEMA, null, 1, 3);
    }

    private ConfiguracionSemaforoResponse crearRespuesta(ConfiguracionSemaforo configuracion) {
        return new ConfiguracionSemaforoResponse(
                configuracion.getTipoEvento(),
                configuracion.getNivel(),
                configuracion.getGerencia() != null ? configuracion.getGerencia().getId() : null,
                configuracion.getRojoHasta(),
                configuracion.getAmarilloHasta());
    }
}
