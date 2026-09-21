package com.juridico.sistema_juridico.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.ActividadUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Usuarios.ActividadUsuarioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceAssignmentTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ActividadUsuarioRepository actividadUsuarioRepository;
    @Mock private GerenciaRepository gerenciaRepository;
    @Mock private MateriaRepository materiaRepository;
    @Mock private UsuarioIdentityService usuarioIdentityService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UsuarioService service;

    private Usuario actor;
    private Gerencia gerencia;
    private Materia materia;

    @BeforeEach
    void setUp() {
        actor = Usuario.builder().id(1).email("admin@finabien.gob.mx").rol(RolUsuario.ADMIN).build();
        gerencia = Gerencia.builder().id(10).nombre("Gerencia Jurídica").activo(true).build();
        materia = Materia.builder().id(20).nombre("Laboral").gerencia(gerencia).activo(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor.getEmail(), "unused"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsOperationalUserWithoutManagement() {
        Usuario nuevo = nuevoAbogado();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.guardarUsuario(nuevo, List.of(), List.of()));

        assertEquals("Selecciona el área del usuario.", error.getMessage());
    }

    @Test
    void rejectsOperationalUserWithoutMatter() {
        Usuario nuevo = nuevoAbogado();
        stubActorAndIdentity();
        when(gerenciaRepository.findAllById(List.of(10))).thenReturn(List.of(gerencia));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.guardarUsuario(nuevo, List.of(10), List.of()));

        assertEquals("Selecciona al menos una materia para el usuario.", error.getMessage());
    }

    @Test
    void rejectsOperationalUserWithMultipleManagements() {
        Usuario nuevo = nuevoAbogado();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.guardarUsuario(nuevo, List.of(10, 11), List.of(20)));

        assertEquals("Selecciona el área del usuario.", error.getMessage());
    }

    @Test
    void savesConsistentAssignmentAndRegistersHistory() {
        Usuario nuevo = nuevoAbogado();
        stubActorAndIdentity();
        when(gerenciaRepository.findAllById(List.of(10))).thenReturn(List.of(gerencia));
        when(materiaRepository.findAllById(List.of(20))).thenReturn(List.of(materia));
        when(passwordEncoder.encode(any(String.class))).thenReturn("hash");
        when(usuarioRepository.saveAndFlush(nuevo)).thenReturn(nuevo);

        Usuario guardado = service.guardarUsuario(nuevo, List.of(10), List.of(20));

        assertEquals(Set.of(gerencia), guardado.getGerencias());
        assertEquals(Set.of(materia), guardado.getMaterias());
        verify(actividadUsuarioRepository).save(any(ActividadUsuario.class));
    }

    private void stubActorAndIdentity() {
        when(usuarioRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(usuarioIdentityService.generateUsernameFromName(any(Usuario.class))).thenReturn("persona.prueba");
        when(usuarioRepository.findByUsernameIgnoreCase("persona.prueba")).thenReturn(Optional.empty());
    }

    private Usuario nuevoAbogado() {
        return Usuario.builder()
                .nombreCompleto("Persona Prueba")
                .email("persona.prueba@finabien.gob.mx")
                .username("persona.prueba")
                .numeroEmpleado("E0100")
                .rol(RolUsuario.ABOGADO)
                .activo(true)
                .build();
    }
}
