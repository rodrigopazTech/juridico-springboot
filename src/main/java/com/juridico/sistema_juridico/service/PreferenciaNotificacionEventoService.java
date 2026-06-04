package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.enums.NotificacionCategoria;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.usuario.PreferenciaNotificacion;
import com.juridico.sistema_juridico.Entity.usuario.PreferenciaNotificacionEvento;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.response.notificaciones.GrupoPreferenciaNotificacionResponse;
import com.juridico.sistema_juridico.dto.response.notificaciones.PreferenciaNotificacionEventoResponse;
import com.juridico.sistema_juridico.repository.Usuarios.PreferenciaNotificacionEventoRepository;
import com.juridico.sistema_juridico.repository.Usuarios.PreferenciaNotificacionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PreferenciaNotificacionEventoService {

    @Autowired
    private PreferenciaNotificacionEventoRepository preferenciaEventoRepository;

    @Autowired
    private PreferenciaNotificacionRepository preferenciaRepository;

    public boolean permiteRecibir(Usuario usuario, NotificacionEvento evento) {
        if (usuario == null || evento == null) {
            return false;
        }

        asegurarPreferenciasBase(usuario);

        if (!evento.isConfigurable()) {
            return true;
        }

        return preferenciaEventoRepository.findByUsuarioIdAndEvento(usuario.getId(), evento)
                .map(PreferenciaNotificacionEvento::isHabilitada)
                .orElseGet(() -> evento.respetaPreferenciasActuales(obtenerPreferenciasLegado(usuario)));
    }

    public List<GrupoPreferenciaNotificacionResponse> construirVista(Usuario usuario) {
        asegurarPreferenciasBase(usuario);

        Map<NotificacionEvento, Boolean> mapaHabilitadas = resolverEstadoEventos(usuario);
        Map<NotificacionCategoria, List<PreferenciaNotificacionEventoResponse>> agrupadas = new LinkedHashMap<>();

        for (NotificacionEvento evento : NotificacionEvento.values()) {
            agrupadas.computeIfAbsent(evento.getCategoria(), key -> new ArrayList<>())
                    .add(new PreferenciaNotificacionEventoResponse(
                            evento,
                            evento.getNombreVisible(),
                            evento.getDescripcionVisible(),
                            mapaHabilitadas.getOrDefault(evento, true),
                            evento.isConfigurable()));
        }

        return agrupadas.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((left, right) -> Integer.compare(left.getOrden(), right.getOrden())))
                .map(entry -> new GrupoPreferenciaNotificacionResponse(
                        entry.getKey().getNombreVisible(),
                        entry.getValue()))
                .toList();
    }

    @Transactional
    public void guardarPreferencias(Usuario usuario, List<NotificacionEvento> eventosHabilitados) {
        asegurarPreferenciasBase(usuario);

        Set<NotificacionEvento> eventosSeleccionados = eventosHabilitados == null
                ? Set.of()
                : eventosHabilitados.stream().collect(Collectors.toSet());

        Map<NotificacionEvento, PreferenciaNotificacionEvento> preferenciasPorEvento = preferenciaEventoRepository
                .findByUsuarioId(usuario.getId())
                .stream()
                .collect(Collectors.toMap(
                        PreferenciaNotificacionEvento::getEvento,
                        pref -> pref,
                        (left, right) -> left,
                        () -> new EnumMap<>(NotificacionEvento.class)));

        for (NotificacionEvento evento : NotificacionEvento.values()) {
            if (!evento.isConfigurable()) {
                continue;
            }

            boolean habilitada = eventosSeleccionados.contains(evento);
            PreferenciaNotificacionEvento preferencia = preferenciasPorEvento.get(evento);

            if (preferencia == null) {
                preferencia = PreferenciaNotificacionEvento.builder()
                        .usuario(usuario)
                        .evento(evento)
                        .habilitada(habilitada)
                        .build();
            } else {
                preferencia.setHabilitada(habilitada);
            }

            preferenciaEventoRepository.save(preferencia);
        }

        sincronizarPreferenciasLegado(usuario, eventosSeleccionados);
    }

    @Transactional
    public void asegurarPreferenciasBase(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return;
        }

        Map<NotificacionEvento, PreferenciaNotificacionEvento> preferenciasPorEvento = preferenciaEventoRepository
                .findByUsuarioId(usuario.getId())
                .stream()
                .collect(Collectors.toMap(
                        PreferenciaNotificacionEvento::getEvento,
                        pref -> pref,
                        (left, right) -> left,
                        () -> new EnumMap<>(NotificacionEvento.class)));

        PreferenciaNotificacion legado = obtenerPreferenciasLegado(usuario);

        for (NotificacionEvento evento : NotificacionEvento.values()) {
            if (!evento.isConfigurable() || preferenciasPorEvento.containsKey(evento)) {
                continue;
            }

            preferenciaEventoRepository.save(PreferenciaNotificacionEvento.builder()
                    .usuario(usuario)
                    .evento(evento)
                    .habilitada(evento.respetaPreferenciasActuales(legado))
                    .build());
        }
    }

    private Map<NotificacionEvento, Boolean> resolverEstadoEventos(Usuario usuario) {
        Map<NotificacionEvento, Boolean> resultado = new EnumMap<>(NotificacionEvento.class);
        Map<NotificacionEvento, PreferenciaNotificacionEvento> preferenciasPorEvento = preferenciaEventoRepository
                .findByUsuarioId(usuario.getId())
                .stream()
                .collect(Collectors.toMap(
                        PreferenciaNotificacionEvento::getEvento,
                        pref -> pref,
                        (left, right) -> left,
                        () -> new EnumMap<>(NotificacionEvento.class)));

        PreferenciaNotificacion legado = obtenerPreferenciasLegado(usuario);

        for (NotificacionEvento evento : NotificacionEvento.values()) {
            if (!evento.isConfigurable()) {
                resultado.put(evento, true);
                continue;
            }

            PreferenciaNotificacionEvento preferenciaEvento = preferenciasPorEvento.get(evento);
            if (preferenciaEvento != null) {
                resultado.put(evento, preferenciaEvento.isHabilitada());
            } else {
                resultado.put(evento, evento.respetaPreferenciasActuales(legado));
            }
        }

        return resultado;
    }

    private PreferenciaNotificacion obtenerPreferenciasLegado(Usuario usuario) {
        return preferenciaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> PreferenciaNotificacion.builder().usuario(usuario).build());
    }

    private void sincronizarPreferenciasLegado(Usuario usuario, Set<NotificacionEvento> eventosSeleccionados) {
        PreferenciaNotificacion preferenciasLegado = obtenerPreferenciasLegado(usuario);

        preferenciasLegado.setNotificarNuevaAudiencia(
                eventosSeleccionados.contains(NotificacionEvento.AUDIENCIA_NUEVA)
                        || eventosSeleccionados.contains(NotificacionEvento.AUDIENCIA_COMPARECENCIA_ASIGNADA)
                        || eventosSeleccionados.contains(NotificacionEvento.AUDIENCIA_RECORDATORIO)
                        || eventosSeleccionados.contains(NotificacionEvento.AUDIENCIA_ACTA_DISPONIBLE)
                        || eventosSeleccionados.contains(NotificacionEvento.AUDIENCIA_VALIDADA_CONCLUIDA));
        preferenciasLegado.setNotificarTerminoVencer(
                eventosSeleccionados.contains(NotificacionEvento.TERMINO_RECORDATORIO));
        preferenciasLegado.setNotificarNuevoTermino(
                eventosSeleccionados.contains(NotificacionEvento.TERMINO_ASIGNADO)
                        || eventosSeleccionados.contains(NotificacionEvento.TERMINO_NUEVO));
        preferenciasLegado.setNotificarReasignacion(
                eventosSeleccionados.contains(NotificacionEvento.EXPEDIENTE_REASIGNADO));

        preferenciaRepository.save(preferenciasLegado);
    }
}
