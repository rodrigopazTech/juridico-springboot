package com.juridico.sistema_juridico.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {
    @Mock NotificacionRepository notificacionRepository;
    @Mock WebPushService webPushService;
    @Mock PreferenciaNotificacionEventoService preferenciaEventoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock TerminoRepository terminoRepository;
    @Mock AudienciaRepository audienciaRepository;
    @Mock ExpedienteRepository expedienteRepository;
    @InjectMocks NotificacionService service;

    @Test
    void mismaAlertaNoSeEntregaDosVeces() {
        Usuario usuario = Usuario.builder().id(9).nombreCompleto("Usuario").build();
        when(notificacionRepository.existsByUsuarioAndEventoAndEntidadIdAndTitulo(
                usuario, NotificacionEvento.TERMINO_RECORDATORIO, "15", "Término entra en crítico"))
                .thenReturn(true);

        service.crearNotificacion(usuario, null, NotificacionEvento.TERMINO_RECORDATORIO,
                "Término entra en crítico", "Mensaje", null, "15", true);

        verify(notificacionRepository, never()).save(any(Notificacion.class));
        verify(webPushService, never()).enviarNotificacion(any(), any(), any(), any());
    }
}
