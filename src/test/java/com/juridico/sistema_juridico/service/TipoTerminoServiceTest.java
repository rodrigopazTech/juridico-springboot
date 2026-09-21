package com.juridico.sistema_juridico.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.repository.Catalogo.TipoTerminoRepository;
import com.juridico.sistema_juridico.service.catalogo.TipoTerminoService;

@ExtendWith(MockitoExtension.class)
class TipoTerminoServiceTest {

    @Mock
    private TipoTerminoRepository tipoTerminoRepository;

    @InjectMocks
    private TipoTerminoService tipoTerminoService;

    private TipoTermino tipoPrueba;

    @BeforeEach
    void setUp() {
        tipoPrueba = TipoTermino.builder()
                .id(1)
                .nombre("Contestación de Demanda")
                .diasPredeterminados(15)
                .tipoDias("HABILES")
                .semaforoRojoDias(3)
                .semaforoAmarilloDias(7)
                .activo(true)
                .build();
    }

    @Test
    void testListarActivos() {
        when(tipoTerminoRepository.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(tipoPrueba));
        List<TipoTermino> resultado = tipoTerminoService.listarActivos();
        assertEquals(1, resultado.size());
        assertEquals("Contestación de Demanda", resultado.get(0).getNombre());
    }

    @Test
    void testCrearExitoso() {
        when(tipoTerminoRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(tipoTerminoRepository.save(any(TipoTermino.class))).thenReturn(tipoPrueba);

        TipoTermino creado = tipoTerminoService.crear(tipoPrueba);
        assertNotNull(creado);
        assertEquals(15, creado.getDiasPredeterminados());
    }

    @Test
    void testCrearDuplicadoLanzaExcepcion() {
        when(tipoTerminoRepository.findByNombreIgnoreCase(anyString())).thenReturn(Optional.of(tipoPrueba));
        assertThrows(BadRequestException.class, () -> tipoTerminoService.crear(tipoPrueba));
    }
}
