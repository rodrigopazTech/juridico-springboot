package com.juridico.sistema_juridico.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private WebPushService webPushService;

    @Autowired
    private PreferenciaNotificacionEventoService preferenciaEventoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.juridico.sistema_juridico.repository.procesal.TerminoRepository terminoRepository;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.juridico.sistema_juridico.repository.procesal.AudienciaRepository audienciaRepository;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository expedienteRepository;

    public void crearNotificacion(
            Usuario usuario,
            NotificacionEvento evento,
            String titulo,
            String mensaje,
            CatPrioridad prioridad,
            String entidadId) {
        crearNotificacion(usuario, null, evento, titulo, mensaje, prioridad, entidadId, false);
    }

    public void crearNotificacion(
            Usuario usuario,
            Usuario actor,
            NotificacionEvento evento,
            String titulo,
            String mensaje,
            CatPrioridad prioridad,
            String entidadId,
            boolean silenciarSuperiores) {
        if (usuario == null || evento == null) {
            return;
        }

        if (!preferenciaEventoService.permiteRecibir(usuario, evento)) {
            return;
        }

        Notificacion notif = Notificacion.builder()
                .usuario(usuario)
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(evento.getTipoVisual())
                .evento(evento)
                .prioridad(prioridad)
                .entidadTipo(evento.getEntidadTipo())
                .entidadId(entidadId)
                .notificarEn(LocalDateTime.now())
                .leida(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificacionRepository.save(notif);

        webPushService.enviarNotificacion(usuario, titulo, mensaje, "/notificaciones/leer/" + notif.getId());

        if (!silenciarSuperiores) {
            notificarASuperiores(usuario, actor, evento, prioridad, entidadId);
        }
    }

    private void notificarASuperiores(
            Usuario subordinado,
            Usuario actor,
            NotificacionEvento evento,
            CatPrioridad prioridad,
            String entidadId) {
        if (subordinado.getGerencia() == null || subordinado.getRol() == null) {
            return;
        }

        List<RolUsuario> rolesSuperiores = new ArrayList<>();
        RolUsuario rolDestinatario = subordinado.getRol();

        if (rolDestinatario == RolUsuario.ABOGADO) {
            rolesSuperiores.addAll(
                    List.of(RolUsuario.JEFE_DEPTO, RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        } else if (rolDestinatario == RolUsuario.JEFE_DEPTO) {
            rolesSuperiores.addAll(List.of(RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        } else if (rolDestinatario == RolUsuario.GERENTE) {
            rolesSuperiores.addAll(List.of(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        }

        if (rolesSuperiores.isEmpty()) {
            return;
        }

        List<Usuario> jefes = new ArrayList<>();
        jefes.addAll(usuarioRepository.findByGerenciaIdAndRolInAndActivoTrue(
                subordinado.getGerencia().getId(), rolesSuperiores));
        jefes = jefes.stream().distinct().toList();

        String etiquetaAutor = (actor != null) ? actor.getRol().toString() : "SISTEMA";
        String tituloJefe = "[" + etiquetaAutor + "] Asignacion a " + rolDestinatario;

        String numExpediente = resolverNumeroExpediente(evento, entidadId);
        String mensajeJefe;
        if (actor != null && !actor.getId().equals(subordinado.getId())) {
            mensajeJefe = String.format("El %s %s ha asignado un %s al %s %s en el expediente: %s",
                    actor.getRol(),
                    actor.getNombreCompleto(),
                    evento.getEtiquetaActividad(),
                    rolDestinatario,
                    subordinado.getNombreCompleto(),
                    numExpediente);
        } else {
            String nombreAutor = (actor != null) ? actor.getNombreCompleto() : "El sistema";
            mensajeJefe = String.format("%s ha generado actividad de %s para %s en el expediente: %s",
                    nombreAutor,
                    evento.getEtiquetaActividad(),
                    subordinado.getNombreCompleto(),
                    numExpediente);
        }

        for (Usuario jefe : jefes) {
            if (jefe.getId().equals(subordinado.getId()) || (actor != null && jefe.getId().equals(actor.getId()))) {
                continue;
            }

            if (!preferenciaEventoService.permiteRecibir(jefe, evento)) {
                continue;
            }

            Notificacion notifJefe = Notificacion.builder()
                    .usuario(jefe)
                    .titulo(tituloJefe)
                    .mensaje(mensajeJefe)
                    .tipo(evento.getTipoVisual())
                    .evento(evento)
                    .prioridad(prioridad)
                    .entidadTipo(evento.getEntidadTipo())
                    .entidadId(entidadId)
                    .notificarEn(LocalDateTime.now())
                    .leida(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificacionRepository.save(notifJefe);
            webPushService.enviarNotificacion(jefe, tituloJefe, mensajeJefe, "/notificaciones/leer/" + notifJefe.getId());
        }
    }

    private String resolverNumeroExpediente(NotificacionEvento evento, String entidadId) {
        if (entidadId == null || entidadId.trim().isEmpty()) {
            return "S/N";
        }

        try {
            if ("TERMINO".equals(evento.getEntidadTipo())) {
                Termino termino = terminoRepository.findById(Integer.valueOf(entidadId)).orElse(null);
                return termino != null ? termino.getNumeroReferencia() : "S/N";
            }

            if ("AUDIENCIA".equals(evento.getEntidadTipo())) {
                Audiencia audiencia = audienciaRepository.findById(Integer.valueOf(entidadId)).orElse(null);
                if (audiencia != null && audiencia.getExpediente() != null) {
                    return audiencia.getExpediente().getNumero();
                }
                return "S/N";
            }

            if ("EXPEDIENTE".equals(evento.getEntidadTipo())) {
                Expediente expediente = expedienteRepository.findById(UUID.fromString(entidadId)).orElse(null);
                return expediente != null ? expediente.getNumero() : "S/N";
            }
        } catch (Exception ignored) {
            return entidadId;
        }

        return entidadId;
    }

    public Page<Notificacion> listarMisNotificaciones(Usuario usuario, Pageable pageable) {
        return notificacionRepository.findByUsuarioOrderByCreatedAtDesc(usuario, pageable);
    }

    public long contarNoLeidas(Usuario usuario) {
        return notificacionRepository.countByUsuarioAndLeidaFalse(usuario);
    }
}
