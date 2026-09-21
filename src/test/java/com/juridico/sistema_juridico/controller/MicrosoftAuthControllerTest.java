package com.juridico.sistema_juridico.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftTokenResponse;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftUserProfile;
import com.juridico.sistema_juridico.exception.UnauthorizedException;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.security.CustomUserDetailsService;
import com.juridico.sistema_juridico.security.JwtTokenProvider;
import com.juridico.sistema_juridico.service.AuditoriaService;
import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;
import com.juridico.sistema_juridico.service.MicrosoftGraphClient;
import com.juridico.sistema_juridico.service.MicrosoftIdentityService;

@ExtendWith(MockitoExtension.class)
class MicrosoftAuthControllerTest {

    @Mock private MicrosoftGraphClient graphClient;
    @Mock private MicrosoftIdentityService identityService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuditoriaService auditoriaService;
    @Mock private AuditoriaTecnicaService auditoriaTecnicaService;

    private MicrosoftAuthController controller;

    @BeforeEach
    void setUp() {
        MicrosoftAuthProperties properties = new MicrosoftAuthProperties();
        properties.setEnabled(true);
        properties.setTenantId("tenant");
        properties.setClientId("client");
        properties.setClientSecret("secret");
        properties.setRedirectUri("http://localhost/auth/microsoft/callback");
        controller = new MicrosoftAuthController(properties, graphClient, identityService, usuarioRepository,
                userDetailsService, jwtTokenProvider, auditoriaService, auditoriaTecnicaService);
    }

    @Test
    void unlinkedMicrosoftLoginAsksForExplicitConfirmationWhenActiveAccountExists() {
        MockHttpSession session = new MockHttpSession();
        when(graphClient.buildAuthorizationUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("https://login.microsoft.test/authorize");
        controller.login(session, new RedirectAttributesModelMap());
        String state = (String) session.getAttribute("MICROSOFT_OAUTH_STATE");
        MicrosoftUserProfile profile = profile();
        Usuario candidate = Usuario.builder().id(12).email(profile.mail()).activo(true).build();
        when(graphClient.exchangeCode("code")).thenReturn(token());
        when(graphClient.fetchMe("access")).thenReturn(profile);
        when(identityService.vincularYObtenerUsuarioLocal(profile))
                .thenThrow(new UnauthorizedException("Sin vinculacion"));
        when(identityService.buscarCuentaActivaPorCorreoMicrosoft(profile)).thenReturn(Optional.of(candidate));

        String view = controller.callback("code", state, null, null,
                request(session), new MockHttpServletResponse(), session, new RedirectAttributesModelMap());

        assertEquals("redirect:/auth/microsoft/vincular-confirmacion", view);
        ConcurrentModel model = new ConcurrentModel();
        assertEquals("views/auth/microsoft-vincular-confirmacion",
                controller.mostrarConfirmacion(session, model));
        assertEquals(profile.mail(), model.getAttribute("correoMicrosoft"));
    }

    @Test
    void unlinkedMicrosoftLoginExplainsThatNoActiveSiajAccountMatches() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(graphClient.buildAuthorizationUrl(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("https://login.microsoft.test/authorize");
        controller.login(session, redirect);
        String state = (String) session.getAttribute("MICROSOFT_OAUTH_STATE");
        MicrosoftUserProfile profile = profile();
        when(graphClient.exchangeCode("code")).thenReturn(token());
        when(graphClient.fetchMe("access")).thenReturn(profile);
        when(identityService.vincularYObtenerUsuarioLocal(profile))
                .thenThrow(new UnauthorizedException("Sin vinculacion"));
        when(identityService.buscarCuentaActivaPorCorreoMicrosoft(profile)).thenReturn(Optional.empty());

        String view = controller.callback("code", state, null, null,
                request(session), new MockHttpServletResponse(), session, redirect);

        assertEquals("redirect:/", view);
        assertTrue(redirect.getFlashAttributes().get("microsoftError").toString()
                .contains("no existe una cuenta SIAJ activa asociada"));
    }

    private MockHttpServletRequest request(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private MicrosoftTokenResponse token() {
        return new MicrosoftTokenResponse("Bearer", "User.Read", 3600L, 3600L, "access", null, null);
    }

    private MicrosoftUserProfile profile() {
        return new MicrosoftUserProfile("object-1", "persona@finabien.gob.mx", List.of(),
                "Persona", null, "persona@finabien.gob.mx", null, null);
    }
}
