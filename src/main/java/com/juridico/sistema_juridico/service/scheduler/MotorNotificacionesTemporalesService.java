package com.juridico.sistema_juridico.service.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.NivelAtencionTemporal;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.response.procesal.AtencionTemporal;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.service.procesal.EvaluadorAtencionTemporalService;

import lombok.RequiredArgsConstructor;

/**
 * Único programador de alertas temporales. Consume el mismo evaluador usado por
 * las pantallas; los programadores heredados permanecen sin agenda para evitar
 * dos fuentes de verdad.
 */
@Service
@RequiredArgsConstructor
public class MotorNotificacionesTemporalesService {
    private final TerminoRepository terminoRepository;
    private final AudienciaRepository audienciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final CatPrioridadRepository prioridadRepository;
    private final EvaluadorAtencionTemporalService evaluador;

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void generarAlertasTemporales() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();
        terminoRepository.findAll().stream().filter(this::generaRecordatorio).forEach(t -> notificar(t, hoy));
        audienciaRepository.findAll().stream().filter(this::generaRecordatorio).forEach(a -> notificar(a, ahora));
    }

    boolean generaRecordatorio(Termino termino) {
        return termino != null && termino.getFechaVencimiento() != null
                && termino.getEstatusTermino() != EstatusTermino.PRESENTADO
                && termino.getEstatusTermino() != EstatusTermino.CONCLUIDO;
    }

    boolean generaRecordatorio(Audiencia audiencia) {
        return audiencia != null && audiencia.getFechaAudiencia() != null
                && audiencia.getEstatusAudiencia() == EstatusAudiencia.PENDIENTE;
    }

    private void notificar(Termino termino, LocalDate hoy) {
        AtencionTemporal atencion = evaluador.evaluarTermino(termino, hoy);
        if (atencion.nivel() != NivelAtencionTemporal.AMARILLO
                && atencion.nivel() != NivelAtencionTemporal.ROJO
                && atencion.nivel() != NivelAtencionTemporal.VENCIDO) return;
        String titulo = tituloTermino(atencion, hoy) + ": " + termino.getNumeroReferencia();
        String mensaje = termino.getAsunto() + ". " + atencion.detalle() + " " + atencion.explicacion();
        CatPrioridad prioridad = prioridad(atencion.nivel());
        for (Usuario usuario : destinatarios(termino)) {
            notificacionService.crearNotificacion(usuario, null, NotificacionEvento.TERMINO_RECORDATORIO,
                    titulo, mensaje, prioridad, termino.getId().toString(), true);
        }
    }

    private void notificar(Audiencia audiencia, LocalDateTime ahora) {
        AtencionTemporal atencion = evaluador.evaluarAudiencia(audiencia, ahora);
        if (atencion.nivel() != NivelAtencionTemporal.AMARILLO
                && atencion.nivel() != NivelAtencionTemporal.ROJO
                && atencion.nivel() != NivelAtencionTemporal.PASADA_SIN_RESULTADO) return;
        String titulo = tituloAudiencia(atencion, ahora.toLocalDate());
        String lugar = Boolean.TRUE.equals(audiencia.getEsVirtual()) ? "Virtual" : audiencia.getSalaLugar();
        String mensaje = atencion.detalle() + " Hora: " + audiencia.getHoraAudiencia() + ". Lugar: "
                + (lugar == null || lugar.isBlank() ? "Por confirmar" : lugar) + ".";
        for (Usuario usuario : destinatarios(audiencia)) {
            notificacionService.crearNotificacion(usuario, null, NotificacionEvento.AUDIENCIA_RECORDATORIO,
                    titulo, mensaje, prioridad(atencion.nivel()), audiencia.getId().toString(), true);
        }
    }

    private String tituloTermino(AtencionTemporal atencion, LocalDate hoy) {
        if (atencion.nivel() == NivelAtencionTemporal.AMARILLO) return "Término entra en atención";
        if (atencion.nivel() == NivelAtencionTemporal.ROJO && Long.valueOf(0).equals(atencion.cantidadRestante())) {
            return "Término vence hoy";
        }
        if (atencion.nivel() == NivelAtencionTemporal.ROJO) return "Término entra en crítico";
        return "Término vencido · " + hoy;
    }

    private String tituloAudiencia(AtencionTemporal atencion, LocalDate hoy) {
        if (atencion.nivel() == NivelAtencionTemporal.AMARILLO) return "Audiencia próxima";
        if (atencion.nivel() == NivelAtencionTemporal.ROJO) return "Audiencia inminente";
        return "Audiencia pasada sin resultado · " + hoy;
    }

    private CatPrioridad prioridad(NivelAtencionTemporal nivel) {
        return prioridadRepository.findByNombreIgnoreCase(nivel == NivelAtencionTemporal.AMARILLO ? "MEDIA" : "ALTA")
                .orElse(null);
    }

    private Set<Usuario> destinatarios(Termino termino) {
        Set<Usuario> usuarios = new HashSet<>();
        if (termino.getAbogadoResponsable() != null) usuarios.add(termino.getAbogadoResponsable());
        Gerencia gerencia = termino.getGerenciaReferencia();
        Materia materia = termino.getMateriaReferencia();
        if (gerencia == null) return usuarios;
        if (termino.getEstatusTermino() == EstatusTermino.REVISION
                || termino.getEstatusTermino() == EstatusTermino.GERENCIA
                || termino.getEstatusTermino() == EstatusTermino.LIBERADO) {
            usuarioRepository.findByRolAndGerenciasIdAndActivoTrue(RolUsuario.JEFE_DEPTO, gerencia.getId()).stream()
                    .filter(jefe -> materia == null || jefe.tieneMateria(materia.getId())).forEach(usuarios::add);
        }
        if (termino.getEstatusTermino() == EstatusTermino.GERENCIA
                || termino.getEstatusTermino() == EstatusTermino.LIBERADO) {
            usuarios.addAll(usuarioRepository.findByRolAndGerenciasIdAndActivoTrue(RolUsuario.GERENTE, gerencia.getId()));
        }
        if (termino.getEstatusTermino() == EstatusTermino.DIRECCION) {
            usuarios.addAll(usuarioRepository.findByRolInAndActivoTrue(
                    Arrays.asList(RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR)));
        }
        return usuarios;
    }

    private Set<Usuario> destinatarios(Audiencia audiencia) {
        Set<Usuario> usuarios = new HashSet<>();
        if (audiencia.getAbogadoComparece() != null) usuarios.add(audiencia.getAbogadoComparece());
        if (audiencia.getExpediente() != null && audiencia.getExpediente().getAbogadoResponsable() != null) {
            usuarios.add(audiencia.getExpediente().getAbogadoResponsable());
        }
        if (audiencia.getAsuntoIndependiente() != null && audiencia.getAsuntoIndependiente().getResponsable() != null) {
            usuarios.add(audiencia.getAsuntoIndependiente().getResponsable());
        }
        Gerencia gerencia = audiencia.getGerenciaReferencia();
        Materia materia = audiencia.getMateriaReferencia();
        if (gerencia != null) {
            usuarioRepository.findByRolAndGerenciasIdAndActivoTrue(RolUsuario.JEFE_DEPTO, gerencia.getId()).stream()
                    .filter(jefe -> materia == null || jefe.tieneMateria(materia.getId())).forEach(usuarios::add);
            usuarios.addAll(usuarioRepository.findByRolAndGerenciasIdAndActivoTrue(RolUsuario.GERENTE, gerencia.getId()));
        }
        return usuarios;
    }
}
