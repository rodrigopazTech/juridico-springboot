package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import com.juridico.sistema_juridico.exception.ConflictException;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.CatalogosSyncService;
import com.juridico.sistema_juridico.service.PasswordResetService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class UsuariosControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private CatalogosSyncService catalogosSyncService;

    @Test
    void guardarUsuarioShowsValidationMessageInsteadOfServerError() {
        UsuariosController controller = new UsuariosController();
        ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
        Usuario usuario = Usuario.builder().nombreCompleto("Sin área").email("").build();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(usuarioService.guardarUsuario(any(Usuario.class), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("Selecciona el área del usuario."));

        String view = controller.guardarUsuario(
                usuario, null, null, new MockHttpServletRequest(), redirect);

        assertEquals("redirect:/usuarios", view);
        assertEquals("Selecciona el área del usuario.", redirect.getFlashAttributes().get("mensaje"));
        assertEquals("error", redirect.getFlashAttributes().get("tipo"));
    }

    @Test
    void guardarUsuarioWithoutEmailDoesNotTryToSendActivation() {
        UsuariosController controller = new UsuariosController();
        ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
        ReflectionTestUtils.setField(controller, "syncService", catalogosSyncService);
        ReflectionTestUtils.setField(controller, "passwordResetService", passwordResetService);
        Usuario usuario = Usuario.builder().nombreCompleto("Persona sin correo")
                .numeroEmpleado("E0200").build();
        Usuario guardado = Usuario.builder().id(20).nombreCompleto("Persona sin correo")
                .username("persona.sin.correo").numeroEmpleado("E0200").email(null).build();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(usuarioService.guardarUsuario(any(Usuario.class), isNull(), isNull())).thenReturn(guardado);

        String view = controller.guardarUsuario(
                usuario, null, null, new MockHttpServletRequest(), redirect);

        assertEquals("redirect:/usuarios", view);
        assertEquals("success", redirect.getFlashAttributes().get("tipo"));
        org.junit.jupiter.api.Assertions.assertTrue(
                redirect.getFlashAttributes().get("mensaje").toString().contains("persona.sin.correo"));
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void eliminarUsuarioShowsSpecificRelationshipReason() {
        UsuariosController controller = new UsuariosController();
        ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        org.mockito.Mockito.doThrow(new ConflictException(
                "No se puede eliminar el usuario porque tiene expedientes asociados."))
                .when(usuarioService).eliminarUsuario(9);

        String view = controller.eliminarUsuario(9, redirect);

        assertEquals("redirect:/usuarios", view);
        assertEquals("No se puede eliminar el usuario porque tiene expedientes asociados.",
                redirect.getFlashAttributes().get("mensaje"));
        assertEquals("error", redirect.getFlashAttributes().get("tipo"));
    }

    @Test
    void inactiveUserCannotReceiveManualRecoveryCode() {
        UsuariosController controller = new UsuariosController();
        ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
        ReflectionTestUtils.setField(controller, "usuarioRepository", usuarioRepository);
        Usuario actor = Usuario.builder().id(1).email("admin@finabien.gob.mx")
                .rol(com.juridico.sistema_juridico.Entity.enums.RolUsuario.ADMIN).build();
        Usuario inactive = Usuario.builder().id(9).email("persona@finabien.gob.mx")
                .rol(com.juridico.sistema_juridico.Entity.enums.RolUsuario.ABOGADO).activo(false).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor.getEmail(), "unused"));
        when(usuarioRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(usuarioService.buscarPorId(9)).thenReturn(inactive);

        var response = controller.generarCodigoRecuperacionManual(
                9, "Prueba de recuperación", new MockHttpServletRequest());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        org.junit.jupiter.api.Assertions.assertTrue(response.getBody().toString().contains("inactivo"));
        SecurityContextHolder.clearContext();
    }

    @Test
    void inactiveUserCannotReceivePasswordResetEmail() {
        UsuariosController controller = new UsuariosController();
        ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
        ReflectionTestUtils.setField(controller, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(controller, "passwordResetService", passwordResetService);
        Usuario actor = Usuario.builder().id(1).email("admin@finabien.gob.mx")
                .rol(com.juridico.sistema_juridico.Entity.enums.RolUsuario.ADMIN).build();
        Usuario inactive = Usuario.builder().id(9).email("persona@finabien.gob.mx")
                .rol(com.juridico.sistema_juridico.Entity.enums.RolUsuario.ABOGADO).activo(false).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor.getEmail(), "unused"));
        when(usuarioRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(usuarioService.buscarPorId(9)).thenReturn(inactive);

        var response = controller.enviarRestablecimiento(9, new MockHttpServletRequest());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        org.junit.jupiter.api.Assertions.assertTrue(response.getBody().toString().contains("inactivo"));
        org.mockito.Mockito.verifyNoInteractions(passwordResetService);
        SecurityContextHolder.clearContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchResultExplainsMatchedFieldAndRelevance() {
        UsuariosController controller = new UsuariosController();
        Usuario usuario = Usuario.builder()
                .id(8)
                .nombreCompleto("María López")
                .email("maria.lopez@finabien.gob.mx")
                .username("maria.lopez")
                .numeroEmpleado("E0100")
                .rol(com.juridico.sistema_juridico.Entity.enums.RolUsuario.ABOGADO)
                .build();

        Map<Integer, Map<String, String>> coincidencias = ReflectionTestUtils.invokeMethod(
                controller, "construirCoincidenciasBusqueda", List.of(usuario), "maria.lopez@");

        assertEquals("Correo", coincidencias.get(8).get("campo"));
        assertEquals("Alta", coincidencias.get(8).get("relevancia"));
        assertEquals("maria.lopez@finabien.gob.mx", coincidencias.get(8).get("texto"));
    }
}
