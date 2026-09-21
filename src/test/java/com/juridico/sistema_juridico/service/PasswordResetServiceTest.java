package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.PasswordResetToken;
import com.juridico.sistema_juridico.Entity.usuario.ManualRecoveryCode;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.request.auth.PasswordResetConfirmRequest;
import com.juridico.sistema_juridico.repository.Usuarios.PasswordResetTokenRepository;
import com.juridico.sistema_juridico.repository.Usuarios.ManualRecoveryCodeRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
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
    private ManualRecoveryCodeRepository manualRecoveryCodeRepository;

    @Mock
    private PasswordResetRateLimitService rateLimitService;

    @Mock
    private PasswordResetMailer emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditoriaTecnicaService auditoriaTecnicaService;

    @Mock
    private SessionRevocationService sessionRevocationService;

    @Spy
    private PasswordPolicyService passwordPolicyService = new PasswordPolicyService();

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PasswordResetService service;

    @org.junit.jupiter.api.BeforeEach
    void configureService() {
        ReflectionTestUtils.setField(service, "expirationMinutes", 30);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
    }

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
        verify(sessionRevocationService).revokeAll(usuario, "PASSWORD_RESET");
        verify(auditoriaTecnicaService).registrarEventoSeguridad(eq(usuario), eq(usuario.getEmail()), eq(request),
                eq("PASSWORD_RESET_CONFIRMADO"), eq("EXITOSO"), eq(200), anyString(), eq("USUARIO"), eq("7"));
    }

    @Test
    void confirmManualRecoveryUpdatesPasswordConsumesCodeAndRevokesSessions() {
        Usuario usuario = Usuario.builder()
                .id(8)
                .username("usuario.manual")
                .email("usuario.manual@finabien.gob.mx")
                .password("old")
                .activo(true)
                .forcePasswordChange(true)
                .build();
        ManualRecoveryCode recoveryCode = ManualRecoveryCode.builder()
                .id(12L)
                .usuario(usuario)
                .codigoHash("code-hash")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(10))
                .usado(false)
                .activo(true)
                .build();

        when(usuarioRepository.findByLoginIdentifier("usuario.manual")).thenReturn(Optional.of(usuario));
        when(manualRecoveryCodeRepository
                .findFirstByUsuarioIdAndActivoTrueAndUsadoFalseOrderByFechaCreacionDesc(8))
                .thenReturn(Optional.of(recoveryCode));
        when(passwordEncoder.matches("ABCD234567", "code-hash")).thenReturn(true);
        when(passwordEncoder.encode("Nueva2026!")).thenReturn("encoded");

        PasswordResetService.ManualRecoveryResult result = service.confirmManualRecovery(
                " Usuario.Manual ", " ABCD234567 ", "Nueva2026!", "Nueva2026!", request);

        assertEquals(PasswordResetService.ManualRecoveryResult.SUCCESS, result);
        assertEquals("encoded", usuario.getPassword());
        assertFalse(usuario.getForcePasswordChange());
        assertTrue(recoveryCode.getUsado());
        assertFalse(recoveryCode.getActivo());
        verify(usuarioRepository).save(usuario);
        verify(manualRecoveryCodeRepository).save(recoveryCode);
        verify(tokenRepository).invalidateActiveTokensByUsuarioId(8);
        verify(sessionRevocationService).revokeAll(usuario, "RECUPERACION_MANUAL");
    }

    @Test
    void confirmManualRecoveryRejectsInactiveUserWithoutReadingCodes() {
        Usuario usuario = Usuario.builder()
                .id(9)
                .username("usuario.inactivo")
                .email("inactivo@finabien.gob.mx")
                .activo(false)
                .build();
        when(usuarioRepository.findByLoginIdentifier("usuario.inactivo")).thenReturn(Optional.of(usuario));

        PasswordResetService.ManualRecoveryResult result = service.confirmManualRecovery(
                "usuario.inactivo", "ABCD234567", "Nueva2026!", "Nueva2026!", request);

        assertEquals(PasswordResetService.ManualRecoveryResult.INACTIVE_USER, result);
        verify(manualRecoveryCodeRepository, never())
                .findFirstByUsuarioIdAndActivoTrueAndUsadoFalseOrderByFechaCreacionDesc(any(Integer.class));
        verify(sessionRevocationService, never()).revokeAll(any(), anyString());
    }

    @Test
    void sendActivationLinkCreatesTokenAndUsesActivationUrl() {
        Usuario usuario = Usuario.builder()
                .id(10)
                .username("usuario.nuevo")
                .email("usuario.nuevo@finabien.gob.mx")
                .activo(true)
                .build();
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendActivationEmail(eq(usuario.getEmail()), eq(usuario.getUsername()),
                org.mockito.ArgumentMatchers.contains("/auth/activar?token="), eq(30)))
                .thenReturn(true);

        boolean sent = service.sendActivationLink(usuario, request);

        assertTrue(sent);
        verify(tokenRepository).invalidateActiveTokensByUsuarioId(10);
        verify(emailService).sendActivationEmail(eq(usuario.getEmail()), eq(usuario.getUsername()),
                org.mockito.ArgumentMatchers.contains("/auth/activar?token="), eq(30));
    }

    @Test
    void sendActivationLinkInvalidatesTokenWhenMailFails() {
        Usuario usuario = Usuario.builder()
                .id(11)
                .username("usuario.sin.correo")
                .email("usuario.sin.correo@finabien.gob.mx")
                .activo(true)
                .build();
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendActivationEmail(eq(usuario.getEmail()), eq(usuario.getUsername()),
                anyString(), eq(30))).thenReturn(false);

        boolean sent = service.sendActivationLink(usuario, request);

        assertFalse(sent);
        org.mockito.ArgumentCaptor<PasswordResetToken> tokenCaptor =
                org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository, org.mockito.Mockito.times(2)).save(tokenCaptor.capture());
        assertFalse(tokenCaptor.getValue().getActivo());
    }
}
