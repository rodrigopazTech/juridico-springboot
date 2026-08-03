package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameRecoveryServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetRateLimitService rateLimitService;

    @Mock
    private PasswordResetMailer emailService;

    @Mock
    private AuditoriaTecnicaService auditoriaTecnicaService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UsernameRecoveryService service;

    @Test
    void requestUsernameReminderReturnsGenericForUnknownEmail() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isAllowed("username:nadie@finabien.gob.mx", "127.0.0.1")).thenReturn(true);
        when(usuarioRepository.findByEmail("nadie@finabien.gob.mx")).thenReturn(Optional.empty());

        UsernameRecoveryService.UsernameRecoveryResult result =
                service.requestUsernameReminder(" Nadie@FINABIEN.gob.mx ", request);

        assertEquals(UsernameRecoveryService.UsernameRecoveryResult.GENERIC, result);
        verify(emailService, never()).sendUsernameReminderEmail(anyString(), anyString(), anyString());
    }

    @Test
    void requestUsernameReminderSendsUsernameForRegisteredEmail() {
        Usuario usuario = new Usuario();
        usuario.setId(7);
        usuario.setEmail("rodrigo.paz@finabien.gob.mx");
        usuario.setUsername("rodrigo.paz");
        usuario.setNumeroEmpleado("E0099");

        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isAllowed("username:rodrigo.paz@finabien.gob.mx", "127.0.0.1")).thenReturn(true);
        when(usuarioRepository.findByEmail("rodrigo.paz@finabien.gob.mx")).thenReturn(Optional.of(usuario));
        when(emailService.sendUsernameReminderEmail("rodrigo.paz@finabien.gob.mx", "rodrigo.paz", "E0099"))
                .thenReturn(true);

        UsernameRecoveryService.UsernameRecoveryResult result =
                service.requestUsernameReminder("rodrigo.paz@finabien.gob.mx", request);

        assertEquals(UsernameRecoveryService.UsernameRecoveryResult.SENT, result);
        verify(emailService).sendUsernameReminderEmail("rodrigo.paz@finabien.gob.mx", "rodrigo.paz", "E0099");
    }
}
