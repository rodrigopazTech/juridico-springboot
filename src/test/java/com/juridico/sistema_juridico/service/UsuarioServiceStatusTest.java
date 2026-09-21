package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.exception.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceStatusTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SessionRevocationService sessionRevocationService;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Mock
    private AudienciaRepository audienciaRepository;

    @Mock
    private TerminoRepository terminoRepository;

    @InjectMocks
    private UsuarioService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void suspendingUserRevokesAllSessions() {
        Usuario actor = Usuario.builder().id(1).email("admin@finabien.gob.mx").rol(RolUsuario.ADMIN).build();
        Usuario target = Usuario.builder().id(2).email("user@finabien.gob.mx").rol(RolUsuario.ABOGADO).activo(true).build();
        authenticate(actor.getEmail());
        when(usuarioRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(usuarioRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(usuarioRepository.save(target)).thenReturn(target);

        Usuario result = service.cambiarEstadoUsuario(target.getId(), false);

        assertFalse(result.getActivo());
        verify(sessionRevocationService).revokeAll(target, "USUARIO_SUSPENDIDO");
    }

    @Test
    void reactivatingUserDoesNotRevokeSessionsAgain() {
        Usuario actor = Usuario.builder().id(1).email("admin@finabien.gob.mx").rol(RolUsuario.ADMIN).build();
        Usuario target = Usuario.builder().id(2).email("user@finabien.gob.mx").rol(RolUsuario.ABOGADO).activo(false).build();
        authenticate(actor.getEmail());
        when(usuarioRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(usuarioRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(usuarioRepository.save(target)).thenReturn(target);

        Usuario result = service.cambiarEstadoUsuario(target.getId(), true);

        assertTrue(result.getActivo());
        verify(sessionRevocationService, never()).revokeAll(target, "USUARIO_SUSPENDIDO");
    }

    @Test
    void deletionReportsExpedientesAsTheExactBlockingRelationship() {
        Usuario actor = Usuario.builder().id(1).email("admin@finabien.gob.mx").rol(RolUsuario.ADMIN).build();
        Usuario target = Usuario.builder().id(2).email("user@finabien.gob.mx").rol(RolUsuario.ABOGADO).build();
        authenticate(actor.getEmail());
        when(usuarioRepository.existsById(target.getId())).thenReturn(true);
        when(usuarioRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(usuarioRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(expedienteRepository.countByAbogadoResponsableId(target.getId())).thenReturn(2L);

        ConflictException error = assertThrows(ConflictException.class,
                () -> service.eliminarUsuario(target.getId()));

        assertEquals("No se puede eliminar el usuario porque tiene expedientes asociados.", error.getMessage());
        verify(usuarioRepository, never()).deleteById(target.getId());
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "unused"));
    }
}
