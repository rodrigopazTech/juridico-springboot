package com.juridico.sistema_juridico.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.request.recordatorios.RecordatorioRequest;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;

@ExtendWith(MockitoExtension.class)
class TareaServiceTest {

    @Mock
    private RecordatorioRepository recordatorioRepository;

    @Mock
    private CatPrioridadRepository prioridadRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private TareaService tareaService;

    private Usuario usuario;
    private CatPrioridad prioridadAlta;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(7).email("usuario@siaj.test").build();
        prioridadAlta = CatPrioridad.builder().id(1).nombre("ALTA").activo(true).build();
    }

    @Test
    void crearPersisteProgramacionValida() {
        RecordatorioRequest request = requestFuturo(LocalDate.now().plusDays(1), LocalTime.of(11, 50));
        when(prioridadRepository.findById(1)).thenReturn(Optional.of(prioridadAlta));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Recordatorio creado = tareaService.crear(request, usuario);

        assertEquals(usuario, creado.getUsuario());
        assertEquals(LocalTime.of(11, 50), creado.getHoraRecordatorio());
        assertEquals(0, creado.getRecordatorioRevision());
        assertEquals(prioridadAlta, creado.getPrioridad());
    }

    @Test
    void actualizarFechaIncrementaRevisionYArchivaAvisoAnterior() {
        Recordatorio existente = tareaExistente();
        Notificacion anterior = Notificacion.builder()
                .id(20)
                .usuario(usuario)
                .evento(NotificacionEvento.TAREA_RECORDATORIO)
                .entidadId("10")
                .leida(false)
                .build();
        RecordatorioRequest request = requestFuturo(LocalDate.now().plusDays(3), LocalTime.of(12, 30));

        when(recordatorioRepository.findByIdAndUsuario(10, usuario)).thenReturn(Optional.of(existente));
        when(prioridadRepository.findById(1)).thenReturn(Optional.of(prioridadAlta));
        when(notificacionRepository.findByUsuarioAndEventoAndLeidaFalseAndEntidadId(
                usuario, NotificacionEvento.TAREA_RECORDATORIO, "10")).thenReturn(List.of(anterior));
        when(notificacionRepository.findByUsuarioAndEventoAndLeidaFalseAndEntidadIdStartingWith(
                usuario, NotificacionEvento.TAREA_RECORDATORIO, "10:")).thenReturn(List.of());
        when(recordatorioRepository.save(any(Recordatorio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Recordatorio actualizado = tareaService.actualizar(10, request, usuario);

        assertEquals(1, actualizado.getRecordatorioRevision());
        assertTrue(anterior.getLeida());
        verify(notificacionRepository).saveAll(List.of(anterior));
    }

    @Test
    void actualizarSoloPrioridadNoReprogramaAviso() {
        Recordatorio existente = tareaExistente();
        RecordatorioRequest request = requestFuturo(existente.getFechaRecordatorio(), existente.getHoraRecordatorio());
        when(recordatorioRepository.findByIdAndUsuario(10, usuario)).thenReturn(Optional.of(existente));
        when(prioridadRepository.findById(1)).thenReturn(Optional.of(prioridadAlta));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Recordatorio actualizado = tareaService.actualizar(10, request, usuario);

        assertEquals(0, actualizado.getRecordatorioRevision());
        verify(notificacionRepository, never()).saveAll(any());
    }

    @Test
    void rechazaFechaYHoraPasadas() {
        RecordatorioRequest request = requestFuturo(LocalDate.now().minusDays(1), LocalTime.NOON);
        assertThrows(BadRequestException.class, () -> tareaService.crear(request, usuario));
    }

    @Test
    void noPermiteEditarTareaAjenaOInexistente() {
        RecordatorioRequest request = requestFuturo(LocalDate.now().plusDays(1), LocalTime.NOON);
        when(recordatorioRepository.findByIdAndUsuario(99, usuario)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tareaService.actualizar(99, request, usuario));
    }

    private RecordatorioRequest requestFuturo(LocalDate fecha, LocalTime hora) {
        RecordatorioRequest request = new RecordatorioRequest();
        request.setTitulo("Presentar Jurídico");
        request.setDetalles("Entregar documentación");
        request.setFechaRecordatorio(fecha);
        request.setHoraRecordatorio(hora);
        request.setPrioridadId(1);
        return request;
    }

    private Recordatorio tareaExistente() {
        return Recordatorio.builder()
                .id(10)
                .usuario(usuario)
                .titulo("Tarea original")
                .fechaRecordatorio(LocalDate.now().plusDays(2))
                .horaRecordatorio(LocalTime.of(11, 50))
                .prioridad(prioridadAlta)
                .completado(false)
                .recordatorioRevision(0)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
