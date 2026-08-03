package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.PasswordResetToken;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.request.auth.PasswordResetConfirmRequest;
import com.juridico.sistema_juridico.repository.Usuarios.PasswordResetTokenRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordResetRateLimitService rateLimitService;

    @Mock
    private PasswordResetMailer emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditoriaTecnicaService auditoriaTecnicaService;

    @Spy
    private PasswordPolicyService passwordPolicyService = new PasswordPolicyService();

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PasswordResetService service;

    @Test
    void requestPasswordResetReturnsGenericForUnknownIdentifier() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isAllowed("nadie", "127.0.0.1")).thenReturn(true);
        when(usuarioRepository.findByLoginIdentifier("nadie")).thenReturn(Optional.empty());

        PasswordResetService.ResetRequestResult result =
                service.requestPasswordReset(" Nadie ", request);

        assertEquals(PasswordResetService.ResetRequestResult.GENERIC, result);
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendResetEmail(anyString(), anyString(), any(Integer.class));
    }

    @Test
    void confirmResetUpdatesPasswordAndConsumesToken() {
        Usuario usuario = Usuario.builder()
                .id(7)
                .email("usuario@finabien.gob.mx")
                .password("old")
                .forcePasswordChange(true)
                .build();
        PasswordResetToken token = PasswordResetToken.builder()
                .id(11L)
                .token("abc")
                .usuario(usuario)
                .email(usuario.getEmail())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(10))
                .usado(false)
                .activo(true)
                .build();
        PasswordResetConfirmRequest resetRequest = new PasswordResetConfirmRequest();
        resetRequest.setToken("abc");
        resetRequest.setNuevaContrasena("Nueva2026!");
        resetRequest.setConfirmacionContrasena("Nueva2026!");

        when(tokenRepository.findFirstByTokenAndActivoTrue("abc")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("Nueva2026!")).thenReturn("encoded");

        PasswordResetService.ConfirmResetResult result = service.confirmReset(resetRequest, request);

        assertEquals(PasswordResetService.ConfirmResetResult.SUCCESS, result);
        assertEquals("encoded", usuario.getPassword());
        assertFalse(usuario.getForcePasswordChange());
        assertTrue(token.getUsado());
        assertFalse(token.getActivo());
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).save(token);
        verify(auditoriaTecnicaService).registrarEventoSeguridad(eq(usuario), eq(usuario.getEmail()), eq(request),
                eq("PASSWORD_RESET_CONFIRMADO"), eq("EXITOSO"), eq(200), anyString(), eq("USUARIO"), eq("7"));
    }
}
