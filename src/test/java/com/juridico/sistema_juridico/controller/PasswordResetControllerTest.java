package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;
import com.juridico.sistema_juridico.dto.request.auth.PasswordResetRequest;
import com.juridico.sistema_juridico.service.PasswordPolicyService;
import com.juridico.sistema_juridico.service.PasswordResetService;
import com.juridico.sistema_juridico.service.UsernameRecoveryService;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private UsernameRecoveryService usernameRecoveryService;

    private PasswordResetController controller;

    @BeforeEach
    void setUp() {
        controller = new PasswordResetController(
                passwordResetService,
                new PasswordPolicyService(),
                usernameRecoveryService,
                new MicrosoftAuthProperties());
        ReflectionTestUtils.setField(controller, "emailMaintenance", false);
    }

    @Test
    void passwordResetWebReportsDeliveryFailureInsteadOfSuccess() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setIdentifier("usuario");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(passwordResetService.requestPasswordReset("usuario", servletRequest))
                .thenReturn(PasswordResetService.ResetRequestResult.EMAIL_ERROR);

        String view = controller.solicitarResetWeb(request, servletRequest, redirect);

        assertEquals("redirect:/auth/solicitar-reset", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
        assertEquals(null, redirect.getFlashAttributes().get("success"));
    }

    @Test
    void usernameRecoveryReportsDeliveryFailureInsteadOfSuccess() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(usernameRecoveryService.requestUsernameReminder("persona@finabien.gob.mx", servletRequest))
                .thenReturn(UsernameRecoveryService.UsernameRecoveryResult.EMAIL_ERROR);

        String view = controller.recordarUsuario("persona@finabien.gob.mx", servletRequest, redirect);

        assertEquals("redirect:/auth/recordar-usuario", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
        assertEquals(null, redirect.getFlashAttributes().get("success"));
    }

    @Test
    void passwordResetApiUsesServiceUnavailableWhenDeliveryFails() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setIdentifier("usuario");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        when(passwordResetService.requestPasswordReset("usuario", servletRequest))
                .thenReturn(PasswordResetService.ResetRequestResult.EMAIL_ERROR);

        var response = controller.solicitarResetApi(request, servletRequest);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("EMAIL_UNAVAILABLE", response.getBody().getStatus());
    }

    @Test
    void activationEndsBeforeMicrosoftLinkingAndShowsCompletionScreen() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Usuario usuario = Usuario.builder().id(27).email("persona@finabien.gob.mx").build();
        when(passwordResetService.findUserForValidToken("token-activacion")).thenReturn(Optional.of(usuario));
        when(passwordResetService.confirmReset(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request)))
                .thenReturn(PasswordResetService.ConfirmResetResult.SUCCESS);

        String view = controller.confirmarResetWeb(
                "token-activacion", "Segura123!", "Segura123!", true, request, redirect);

        assertEquals("redirect:/auth/activacion-completada", view);
        assertEquals(27, request.getSession().getAttribute(PasswordResetController.ACTIVATED_USER_ID));
    }

    @Test
    void activationValidationFailureReturnsToActivationScreen() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(passwordResetService.findUserForValidToken("token-activacion")).thenReturn(Optional.empty());
        when(passwordResetService.confirmReset(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request)))
                .thenReturn(PasswordResetService.ConfirmResetResult.INVALID_TOKEN);

        String view = controller.confirmarResetWeb(
                "token-activacion", "Segura123!", "Segura123!", true, request, redirect);

        assertEquals("redirect:/auth/activar?token=token-activacion", view);
        assertNotNull(redirect.getFlashAttributes().get("error"));
    }
}
