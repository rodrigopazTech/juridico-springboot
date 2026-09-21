package com.juridico.sistema_juridico.security;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Authentication authentication;

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(provider, "jwtSecret",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(provider, "jwtExpirationInMs", 60_000L);
        ReflectionTestUtils.setField(provider, "refreshExpirationInMs", 120_000L);
    }

    @Test
    void accessTokenCarriesCurrentSecurityVersion() {
        Usuario usuario = Usuario.builder().email("usuario@finabien.gob.mx").securityVersion(4).build();
        when(authentication.getName()).thenReturn(usuario.getEmail());
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        String token = provider.generateToken(authentication);

        assertTrue(provider.validateToken(token));
        assertEquals(usuario.getEmail(), provider.getUsernameFromJWT(token));
        assertEquals(4, provider.getSecurityVersionFromJWT(token));
    }

    @Test
    void refreshTokenCarriesCurrentSecurityVersion() {
        Usuario usuario = Usuario.builder().email("usuario@finabien.gob.mx").securityVersion(7).build();
        when(authentication.getName()).thenReturn(usuario.getEmail());
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        String token = provider.generateRefreshToken(authentication);

        assertTrue(provider.validateToken(token));
        assertEquals(7, provider.getSecurityVersionFromJWT(token));
    }
}
