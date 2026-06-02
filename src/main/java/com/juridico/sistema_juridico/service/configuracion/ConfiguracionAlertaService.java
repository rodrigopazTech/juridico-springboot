package com.juridico.sistema_juridico.service.configuracion;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.NivelConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.response.configuracion.ConfiguracionAlertaResponse;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.repository.configuracion.ConfiguracionAlertaRepository;

@Service
public class ConfiguracionAlertaService {

    private static final Set<Integer> DIAS_PREDETERMINADOS = Set.of(3, 1, 0);

    private final ConfiguracionAlertaRepository configuracionAlertaRepository;

    public ConfiguracionAlertaService(ConfiguracionAlertaRepository configuracionAlertaRepository) {
        this.configuracionAlertaRepository = configuracionAlertaRepository;
    }

    public Set<Integer> obtenerDiasCandidatos(TipoEventoAlerta tipoEvento) {
        Set<Integer> dias = new LinkedHashSet<>(DIAS_PREDETERMINADOS);
        dias.addAll(configuracionAlertaRepository.findDiasActivosByTipoEvento(tipoEvento));
        return ordenar(dias);
    }

    public boolean debeNotificar(TipoEventoAlerta tipoEvento, int diasAnticipacion, Usuario usuario,
            String asuntoTipo, String asuntoId) {
        return obtenerDiasAplicables(tipoEvento, usuario, asuntoTipo, asuntoId).contains(diasAnticipacion);
    }

    public Set<Integer> obtenerDiasAplicables(TipoEventoAlerta tipoEvento, Usuario usuario,
            String asuntoTipo, String asuntoId) {
        List<ConfiguracionAlerta> reglas = configuracionAlertaRepository.findByTipoEventoAndActivaTrue(tipoEvento);
        Set<Integer> resultado = new LinkedHashSet<>();

        List<ConfiguracionAlerta> obligatorias = reglas.stream()
                .filter(regla -> Boolean.TRUE.equals(regla.getObligatoria()))
                .filter(regla -> aplica(regla, usuario, asuntoTipo, asuntoId))
                .toList();
        obligatorias.forEach(regla -> resultado.add(regla.getDiasAnticipacion()));

        NivelConfiguracionAlerta nivelEfectivo = reglas.stream()
                .filter(regla -> !Boolean.TRUE.equals(regla.getObligatoria()))
                .filter(regla -> aplica(regla, usuario, asuntoTipo, asuntoId))
                .map(ConfiguracionAlerta::getNivel)
                .max(Comparator.comparingInt(this::prioridad))
                .orElse(null);

        if (nivelEfectivo == null) {
            resultado.addAll(DIAS_PREDETERMINADOS);
        } else {
            reglas.stream()
                    .filter(regla -> !Boolean.TRUE.equals(regla.getObligatoria()))
                    .filter(regla -> regla.getNivel() == nivelEfectivo)
                    .filter(regla -> aplica(regla, usuario, asuntoTipo, asuntoId))
                    .forEach(regla -> resultado.add(regla.getDiasAnticipacion()));
        }

        return ordenar(resultado);
    }

    public List<ConfiguracionAlertaResponse> obtenerConfiguracion(
            TipoEventoAlerta tipoEvento, NivelConfiguracionAlerta nivel, Gerencia gerencia,
            Usuario usuario, String asuntoTipo, String asuntoId) {
        return configuracionAlertaRepository.findByTipoEventoAndActivaTrue(tipoEvento).stream()
                .filter(regla -> mismoAlcance(regla, nivel, gerencia, usuario, asuntoTipo, asuntoId))
                .collect(Collectors.groupingBy(regla -> Boolean.TRUE.equals(regla.getObligatoria())))
                .entrySet().stream()
                .map(entry -> crearRespuesta(tipoEvento, nivel, gerencia, usuario, asuntoTipo, asuntoId,
                        ordenar(entry.getValue().stream()
                                .map(ConfiguracionAlerta::getDiasAnticipacion)
                                .collect(Collectors.toSet())),
                        entry.getKey()))
                .sorted(Comparator.comparing(ConfiguracionAlertaResponse::obligatoria).reversed())
                .toList();
    }

    @Transactional
    public ConfiguracionAlertaResponse reemplazarConfiguracion(
            TipoEventoAlerta tipoEvento, NivelConfiguracionAlerta nivel, Gerencia gerencia,
            Usuario usuario, String asuntoTipo, String asuntoId, Set<Integer> diasAnticipacion,
            boolean obligatoria) {
        validarDias(diasAnticipacion);
        List<ConfiguracionAlerta> existentes = configuracionAlertaRepository.findByTipoEventoAndActivaTrue(tipoEvento)
                .stream()
                .filter(regla -> Boolean.TRUE.equals(regla.getObligatoria()) == obligatoria)
                .filter(regla -> mismoAlcance(regla, nivel, gerencia, usuario, asuntoTipo, asuntoId))
                .toList();
        configuracionAlertaRepository.deleteAll(existentes);

        List<ConfiguracionAlerta> nuevas = diasAnticipacion.stream()
                .map(dia -> ConfiguracionAlerta.builder()
                        .tipoEvento(tipoEvento)
                        .nivel(nivel)
                        .gerencia(gerencia)
                        .usuario(usuario)
                        .asuntoTipo(asuntoTipo)
                        .asuntoId(asuntoId)
                        .diasAnticipacion(dia)
                        .activa(true)
                        .obligatoria(obligatoria)
                        .build())
                .toList();
        configuracionAlertaRepository.saveAll(nuevas);
        return crearRespuesta(tipoEvento, nivel, gerencia, usuario, asuntoTipo, asuntoId,
                ordenar(diasAnticipacion), obligatoria);
    }

    private boolean aplica(ConfiguracionAlerta regla, Usuario usuario, String asuntoTipo, String asuntoId) {
        return switch (regla.getNivel()) {
            case SISTEMA -> true;
            case GERENCIA -> usuario != null && usuario.getGerencia() != null && regla.getGerencia() != null
                    && regla.getGerencia().getId().equals(usuario.getGerencia().getId());
            case USUARIO -> usuario != null && regla.getUsuario() != null
                    && regla.getUsuario().getId().equals(usuario.getId());
            case ASUNTO -> regla.getAsuntoTipo() != null && regla.getAsuntoId() != null
                    && regla.getAsuntoTipo().equalsIgnoreCase(asuntoTipo)
                    && regla.getAsuntoId().equals(asuntoId);
        };
    }

    private int prioridad(NivelConfiguracionAlerta nivel) {
        return switch (nivel) {
            case SISTEMA -> 0;
            case GERENCIA -> 1;
            case USUARIO -> 2;
            case ASUNTO -> 3;
        };
    }

    private boolean mismoAlcance(ConfiguracionAlerta regla, NivelConfiguracionAlerta nivel, Gerencia gerencia,
            Usuario usuario, String asuntoTipo, String asuntoId) {
        if (regla.getNivel() != nivel) {
            return false;
        }
        return switch (nivel) {
            case SISTEMA -> regla.getGerencia() == null && regla.getUsuario() == null && regla.getAsuntoId() == null;
            case GERENCIA -> gerencia != null && regla.getGerencia() != null
                    && gerencia.getId().equals(regla.getGerencia().getId());
            case USUARIO -> usuario != null && regla.getUsuario() != null
                    && usuario.getId().equals(regla.getUsuario().getId());
            case ASUNTO -> regla.getAsuntoTipo() != null && regla.getAsuntoId() != null
                    && regla.getAsuntoTipo().equalsIgnoreCase(asuntoTipo) && regla.getAsuntoId().equals(asuntoId);
        };
    }

    private void validarDias(Set<Integer> diasAnticipacion) {
        if (diasAnticipacion == null) {
            throw new BadRequestException("Los días de anticipación son obligatorios.");
        }
        if (diasAnticipacion.stream().anyMatch(dia -> dia == null || dia < 0 || dia > 365)) {
            throw new BadRequestException("Los días de anticipación deben estar entre 0 y 365.");
        }
    }

    private ConfiguracionAlertaResponse crearRespuesta(TipoEventoAlerta tipoEvento, NivelConfiguracionAlerta nivel,
            Gerencia gerencia, Usuario usuario, String asuntoTipo, String asuntoId, Set<Integer> dias,
            boolean obligatoria) {
        return new ConfiguracionAlertaResponse(
                tipoEvento,
                nivel,
                gerencia != null ? gerencia.getId() : null,
                usuario != null ? usuario.getId() : null,
                asuntoTipo,
                asuntoId,
                dias,
                obligatoria);
    }

    private Set<Integer> ordenar(Set<Integer> dias) {
        return dias.stream()
                .filter(dia -> dia != null && dia >= 0)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
