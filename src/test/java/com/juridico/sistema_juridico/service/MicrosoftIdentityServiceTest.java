package com.juridico.sistema_juridico.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftUserProfile;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MicrosoftIdentityServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private MicrosoftIdentityService service;

    @BeforeEach
    void setUp() {
        service = new MicrosoftIdentityService(usuarioRepository);
    }

    @Test
    void findsExactlyOneActiveSiajAccountByVerifiedMicrosoftEmail() {
        Usuario usuario = Usuario.builder().id(4).email("persona@finabien.gob.mx").activo(true).build();
        MicrosoftUserProfile profile = profile("object-1", "persona@finabien.gob.mx");
        when(usuarioRepository.findByEmailIgnoreCase("persona@finabien.gob.mx"))
                .thenReturn(Optional.of(usuario));

        Optional<Usuario> result = service.buscarCuentaActivaPorCorreoMicrosoft(profile);

        assertEquals(usuario, result.orElseThrow());
    }

    @Test
    void doesNotOfferInactiveAccountForAutomaticLinking() {
        Usuario usuario = Usuario.builder().id(4).email("persona@finabien.gob.mx").activo(false).build();
        MicrosoftUserProfile profile = profile("object-1", "persona@finabien.gob.mx");
        when(usuarioRepository.findByEmailIgnoreCase("persona@finabien.gob.mx"))
                .thenReturn(Optional.of(usuario));

        assertTrue(service.buscarCuentaActivaPorCorreoMicrosoft(profile).isEmpty());
    }

    @Test
    void linksAndUnlinksMicrosoftIndependentlyFromActivation() {
        Usuario usuario = Usuario.builder().id(4).email("persona@finabien.gob.mx")
                .activo(true).microsoftLoginEnabled(false).build();
        MicrosoftUserProfile profile = profile("object-1", "persona@finabien.gob.mx");
        when(usuarioRepository.findByMicrosoftObjectId("object-1")).thenReturn(Optional.empty());
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario linked = service.vincularPerfilAUsuario(usuario, profile);
        assertTrue(linked.getMicrosoftLoginEnabled());
        assertEquals("object-1", linked.getMicrosoftObjectId());

        Usuario unlinked = service.desvincularPerfilMicrosoft(linked);
        assertFalse(unlinked.getMicrosoftLoginEnabled());
        assertNull(unlinked.getMicrosoftObjectId());
        verify(usuarioRepository, org.mockito.Mockito.times(2)).save(usuario);
    }

    private MicrosoftUserProfile profile(String objectId, String email) {
        return new MicrosoftUserProfile(objectId, email, List.of(), "Persona", null, email, null, null);
    }
}
