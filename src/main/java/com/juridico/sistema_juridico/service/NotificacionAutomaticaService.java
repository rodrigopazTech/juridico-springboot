package com.juridico.sistema_juridico.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.service.configuracion.ConfiguracionAlertaService;

@Service
@EnableScheduling
public class NotificacionAutomaticaService {

    @Autowired
    private TerminoRepository terminoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private CatPrioridadRepository catPrioridadRepository;

    @Autowired
    private ConfiguracionAlertaService configuracionAlertaService;

    private final List<EstatusTermino> ESTATUS_FINALES = Arrays.asList(EstatusTermino.CONCLUIDO);

    // Reemplazado por MotorNotificacionesTemporalesService.
    @Transactional
    public void verificarVencimientosProximos() {
        LocalDate hoy = LocalDate.now();

        for (Integer diasAnticipacion : configuracionAlertaService.obtenerDiasCandidatos(TipoEventoAlerta.TERMINO)) {
            verificarYNotificar(hoy.plusDays(diasAnticipacion), diasAnticipacion);
        }
    }

    private void verificarYNotificar(LocalDate fechaObjetivo, int diasAnticipacion) {
        CatPrioridad prioridad = catPrioridadRepository
                .findByNombreIgnoreCase(diasAnticipacion >= 3 ? "MEDIA" : "ALTA")
                .orElse(null);
        List<Termino> terminos = terminoRepository.findByFechaVencimientoAndEstatusTerminoNotIn(
                fechaObjetivo, ESTATUS_FINALES);

        for (Termino termino : terminos) {
            String expedienteNum = termino.getNumeroReferencia();
            String mensaje = String.format(
                    "El término '%s' %s vence el %s. Actualmente está en la etapa: %s",
                    termino.getAsunto(),
                    termino.getExpediente() != null ? "del expediente " + expedienteNum : "(sin expediente)",
                    termino.getFechaVencimiento(),
                    termino.getEstatusTermino());

            Set<Usuario> destinatarios = obtenerDestinatarios(termino);
            String tituloNotif = crearTitulo(diasAnticipacion) + ": " + expedienteNum;

            for (Usuario usuario : destinatarios) {
                if (!configuracionAlertaService.debeNotificar(
                        TipoEventoAlerta.TERMINO, diasAnticipacion, usuario, "TERMINO", termino.getId().toString())) {
                    continue;
                }
                notificacionService.crearNotificacion(
                        usuario,
                        null,
                        NotificacionEvento.TERMINO_RECORDATORIO,
                        tituloNotif,
                        mensaje,

                        prioridad,
                        termino.getId().toString(),
                        true);
            }
        }
    }

    private Set<Usuario> obtenerDestinatarios(Termino termino) {
        Set<Usuario> destinatarios = new HashSet<>();
        Gerencia gerencia = termino.getExpediente() != null
                ? termino.getExpediente().getGerencia()
                : termino.getContexto() != null ? termino.getContexto().getGerencia() : null;

        if (termino.getAbogadoResponsable() != null) {
            destinatarios.add(termino.getAbogadoResponsable());
        }

        if (gerencia != null) {
            switch (termino.getEstatusTermino()) {
                case REVISION:
                    destinatarios.addAll(usuarioRepository.findByRolAndGerenciasIdAndActivoTrue(RolUsuario.JEFE_DEPTO, gerencia.getId()));
                    break;
                case GERENCIA:
                    destinatarios.addAll(usuarioRepository.findByRolAndGerenciasIdAndActivoTrue(RolUsuario.GERENTE, gerencia.getId()));
                    break;
                case DIRECCION:
                    destinatarios.addAll(usuarioRepository
                            .findByRolInAndActivoTrue(Arrays.asList(RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR)));
                    break;
                case LIBERADO:
                case PRESENTADO:
                    destinatarios.addAll(usuarioRepository.findByRolAndGerenciasIdAndActivoTrue(RolUsuario.JEFE_DEPTO, gerencia.getId()));
                    destinatarios.addAll(usuarioRepository.findByRolAndGerenciasIdAndActivoTrue(RolUsuario.GERENTE, gerencia.getId()));
                    destinatarios.addAll(usuarioRepository
                            .findByRolInAndActivoTrue(Arrays.asList(RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR)));
                    break;
            }
        } else if (termino.getEstatusTermino() == EstatusTermino.DIRECCION
                || termino.getEstatusTermino() == EstatusTermino.LIBERADO
                || termino.getEstatusTermino() == EstatusTermino.PRESENTADO) {
            destinatarios.addAll(usuarioRepository
                    .findByRolInAndActivoTrue(Arrays.asList(RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR)));
        }

        return destinatarios;
    }

    private String crearTitulo(int diasAnticipacion) {
        if (diasAnticipacion == 0) {
            return "¡VENCE HOY! URGENTE";
        }
        if (diasAnticipacion == 1) {
            return "¡Vence Mañana!";
        }
        return "Advertencia: vence en " + diasAnticipacion + " días";
    }
}
