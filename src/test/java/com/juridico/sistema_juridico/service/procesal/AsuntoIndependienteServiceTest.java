package com.juridico.sistema_juridico.service.procesal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.EstadoAsuntoIndependiente;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.AsuntoIndependiente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AsuntoIndependienteRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.service.AuditoriaService;

@ExtendWith(MockitoExtension.class)
class AsuntoIndependienteServiceTest {
    @Mock AsuntoIndependienteRepository asuntoRepository;
    @Mock ExpedienteRepository expedienteRepository;
    @Mock TerminoRepository terminoRepository;
    @Mock AudienciaRepository audienciaRepository;
    @Mock MateriaRepository materiaRepository;
    @Mock GerenciaRepository gerenciaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock AuditoriaService auditoriaService;

    private AsuntoIndependienteService service;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        service = new AsuntoIndependienteService(asuntoRepository, expedienteRepository, terminoRepository,
                audienciaRepository, materiaRepository, gerenciaRepository, usuarioRepository, auditoriaService);
        admin = Usuario.builder().id(1).nombreCompleto("Administrador").rol(RolUsuario.ADMIN).activo(true).build();
    }

    @Test
    void buscaPorMateriaYResponsable() {
        Materia materia = Materia.builder().id(7).nombre("Laboral").build();
        Usuario responsable = Usuario.builder().id(3).nombreCompleto("Ana Jurídica")
                .rol(RolUsuario.ABOGADO).activo(true).build();
        AsuntoIndependiente asunto = AsuntoIndependiente.builder().id(11L).folioSiaj("AI-2026-0011")
                .referencia("Oficio 10").materia(materia).responsable(responsable)
                .estado(EstadoAsuntoIndependiente.ACTIVO).createdAt(LocalDateTime.now()).build();
        when(asuntoRepository.findAll()).thenReturn(List.of(asunto));

        assertEquals(1, service.listarTodos(admin, "laboral").size());
        assertEquals(1, service.listarTodos(admin, "ana jurídica").size());
    }

    @Test
    void jefeSoloVeAsuntosDeSusMaterias() {
        Gerencia gerencia = Gerencia.builder().id(4).nombre("Gerencia Jurídica").build();
        Materia laboral = Materia.builder().id(7).nombre("Laboral").gerencia(gerencia).build();
        Materia fiscal = Materia.builder().id(8).nombre("Fiscal").gerencia(gerencia).build();
        Usuario jefe = Usuario.builder().id(2).nombreCompleto("Jefe Laboral").rol(RolUsuario.JEFE_DEPTO)
                .gerencias(Set.of(gerencia)).materias(Set.of(laboral)).activo(true).build();
        AsuntoIndependiente permitido = AsuntoIndependiente.builder().id(1L).folioSiaj("AI-1")
                .referencia("Laboral").gerencia(gerencia).materia(laboral)
                .estado(EstadoAsuntoIndependiente.ACTIVO).createdAt(LocalDateTime.now()).build();
        AsuntoIndependiente restringido = AsuntoIndependiente.builder().id(2L).folioSiaj("AI-2")
                .referencia("Fiscal").gerencia(gerencia).materia(fiscal)
                .estado(EstadoAsuntoIndependiente.ACTIVO).createdAt(LocalDateTime.now().minusMinutes(1)).build();
        when(asuntoRepository.findAll()).thenReturn(List.of(permitido, restringido));

        List<AsuntoIndependiente> visibles = service.listarTodos(jefe, "");

        assertEquals(List.of(permitido), visibles);
    }

    @Test
    void vincularMueveTodasLasActuacionesYConservaLaIdentidad() {
        long asuntoId = 8L;
        UUID expedienteId = UUID.randomUUID();
        Expediente expediente = new Expediente();
        expediente.setId(expedienteId);
        AsuntoIndependiente asunto = AsuntoIndependiente.builder().id(asuntoId).folioSiaj("AI-2026-0008")
                .referencia("Consulta urgente").estado(EstadoAsuntoIndependiente.ACTIVO).build();
        Termino termino = Termino.builder().asuntoIndependiente(asunto).build();
        Audiencia audiencia = Audiencia.builder().asuntoIndependiente(asunto).build();

        when(asuntoRepository.findById(asuntoId)).thenReturn(Optional.of(asunto));
        when(expedienteRepository.findById(expedienteId)).thenReturn(Optional.of(expediente));
        when(terminoRepository.findByAsuntoIndependienteId(asuntoId)).thenReturn(List.of(termino));
        when(audienciaRepository.findByAsuntoIndependienteId(asuntoId)).thenReturn(List.of(audiencia));
        when(asuntoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.vincularAExpediente(asuntoId, expedienteId, "Se recibió expediente", admin);

        assertEquals(expediente, termino.getExpediente());
        assertNull(termino.getAsuntoIndependiente());
        assertEquals(expediente, audiencia.getExpediente());
        assertNull(audiencia.getAsuntoIndependiente());
        assertEquals(EstadoAsuntoIndependiente.VINCULADO, asunto.getEstado());
        assertEquals("AI-2026-0008", asunto.getFolioSiaj());
        assertEquals("Se recibió expediente", asunto.getMotivoVinculacion());
        verify(terminoRepository).saveAll(List.of(termino));
        verify(audienciaRepository).saveAll(List.of(audiencia));
    }
}
