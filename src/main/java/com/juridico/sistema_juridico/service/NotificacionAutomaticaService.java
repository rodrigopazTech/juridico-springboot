package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // CORRECCIÓN CLAVE: Ahora SOLO "Concluido" apaga la alarma.
    // Si está en Liberado o Presentado y se acaba el tiempo, ¡la alarma sonará!
    private final List<EstatusTermino> ESTATUS_FINALES = Arrays.asList(EstatusTermino.CONCLUIDO);

    // @Scheduled(fixedRate = 60000) // Cambiar al cron normal cuando termines de
    // probar
    @Scheduled(cron = "0 0 8 * * ?") // (Horario real 8 AM)
    @Transactional
    public void verificarVencimientosProximos() {
        LocalDate hoy = LocalDate.now();
        System.out.println("⏰ Ejecutando revisión automática de términos: " + hoy);

        verificarYNotificar(hoy.plusDays(3), "Advertencia de Vencimiento",
                catPrioridadRepository.findByNombreIgnoreCase("MEDIA").orElse(null));
        verificarYNotificar(hoy.plusDays(1), "¡Vence Mañana!",
                catPrioridadRepository.findByNombreIgnoreCase("ALTA").orElse(null));
        verificarYNotificar(hoy, "¡VENCE HOY! URGENTE",
                catPrioridadRepository.findByNombreIgnoreCase("ALTA").orElse(null));
    }

    private void verificarYNotificar(LocalDate fechaObjetivo, String tituloBase, CatPrioridad prioridad) {
        // La consulta trae el expediente y abogado de inmediato (Evita
        // LazyInitializationException)
        List<Termino> terminos = terminoRepository.findByFechaVencimientoAndEstatusTerminoNotIn(fechaObjetivo,
                ESTATUS_FINALES);

        for (Termino t : terminos) {
            String mensaje = String.format(
                    "El término '%s' del expediente %s vence el %s. Actualmente está en la etapa: %s",
                    t.getActuacion(),
                    t.getExpediente().getNumero(),
                    t.getFechaVencimiento(),
                    t.getEstatusTermino());

            Set<Usuario> destinatarios = new HashSet<>();
            Gerencia g = t.getExpediente().getGerencia();

            // 1. El Abogado Responsable SIEMPRE es notificado
            if (t.getAbogadoResponsable() != null) {
                destinatarios.add(t.getAbogadoResponsable());
            }

            // 2. Ruteo Dinámico: ¿A quién más le avisamos según la etapa?
            switch (t.getEstatusTermino()) {
                case REVISION:
                    destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.JEFE_DEPTO, g));
                    break;
                case GERENCIA:
                    destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.GERENTE, g));
                    break;
                case DIRECCION:
                    destinatarios.addAll(usuarioRepository
                            .findByRolInAndActivoTrue(Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION)));
                    break;
                case LIBERADO:
                case PRESENTADO:
                    // ¡ALERTA GENERAL! Si ya se liberó o presentó pero no se ha subido el acuse
                    // (Concluido),
                    // y el tiempo se está acabando, le avisamos a TODA LA CADENA DE MANDO.
                    destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.JEFE_DEPTO, g));
                    destinatarios.addAll(usuarioRepository.findByRolAndGerenciaAndActivoTrue(RolUsuario.GERENTE, g));
                    destinatarios.addAll(usuarioRepository
                            .findByRolInAndActivoTrue(Arrays.asList(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION)));
                    break;
            }

            // 3. Enviar las notificaciones
            String expedienteNum = (t.getExpediente() != null) ? t.getExpediente().getNumero() : "S/N";
            String tituloNotif = tituloBase + ": " + expedienteNum;

            for (Usuario u : destinatarios) {
                notificacionService.crearNotificacion(
                        u,
                        tituloNotif,
                        mensaje,
                        "TERMINO",
                        prioridad,
                        t.getId().toString());
                System.out.println("   -> Notificación enviada a " + u.getEmail() + " (" + u.getRol()
                        + ") sobre término ID: " + t.getId());
            }
        }
    }
}