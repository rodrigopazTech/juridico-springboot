package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionRevocationServiceTest {

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private AuditoriaTecnicaService auditoriaTecnicaService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SessionInformation sessionInformation;

    @InjectMocks
    private SessionRevocationService service;

    @Test
    void revokeAllIncrementsSecurityVersionAndExpiresRegisteredSessions() {
        Usuario usuario = Usuario.builder()
                .id(8)
                .email("persona@finabien.gob.mx")
                .securityVersion(3)
                .build();
        UserDetails principal = User.withUsername("PERSONA@finabien.gob.mx")
                .password("unused")
                .authorities("ABOGADO")
                .build();

        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
        when(sessionRegistry.getAllSessions(principal, false)).thenReturn(List.of(sessionInformation));

        service.revokeAll(usuario, "PASSWORD_RESET");

        assertEquals(4, usuario.getSecurityVersion());
        verify(usuarioRepository).save(usuario);
        verify(sessionInformation).expireNow();
        verify(auditoriaTecnicaService).cerrarSesionesActivasUsuario(usuario, "PASSWORD_RESET");
    }
}
