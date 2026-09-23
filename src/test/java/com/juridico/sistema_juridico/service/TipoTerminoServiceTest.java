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
import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.repository.Catalogo.TipoTerminoRepository;
import com.juridico.sistema_juridico.repository.configuracion.PerfilAlertaTerminoRepository;
import com.juridico.sistema_juridico.service.catalogo.TipoTerminoService;

@ExtendWith(MockitoExtension.class)
class TipoTerminoServiceTest {

    @Mock
    private TipoTerminoRepository tipoTerminoRepository;

    @Mock
    private PerfilAlertaTerminoRepository perfilAlertaTerminoRepository;

    @InjectMocks
    private TipoTerminoService tipoTerminoService;

    private TipoTermino tipoPrueba;

    @BeforeEach
    void setUp() {
        PerfilAlertaTermino perfil = PerfilAlertaTermino.builder()
                .id(1).codigo("PLAZO_MEDIO").nombre("Plazo medio").descripcion("Perfil de prueba")
                .modo(ModoPerfilAlerta.DIAS_RESTANTES).amarilloDias(5).rojoDias(2)
                .estado(EstadoPerfil.PUBLICADO).version(1).build();
        tipoPrueba = TipoTermino.builder()
                .id(1)
                .codigo("CONTESTACION")
                .nombre("Contestación de Demanda")
                .descripcion("Contestación dentro del plazo legal")
                .modoPlazo(ModoPlazoTermino.CALCULADO)
                .diasPredeterminados(15)
                .tipoDias("HABILES")
                .perfilAlerta(perfil)
                .activo(true)
                .build();
        lenient().when(perfilAlertaTerminoRepository.findById(1)).thenReturn(Optional.of(perfil));
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
