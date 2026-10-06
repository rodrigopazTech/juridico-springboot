package com.juridico.sistema_juridico.service.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.juridico.sistema_juridico.Entity.configuracion.CalendarioVersion;
import com.juridico.sistema_juridico.Entity.configuracion.SuspensionPlazo;
import com.juridico.sistema_juridico.repository.configuracion.CalendarioVersionRepository;
import com.juridico.sistema_juridico.repository.configuracion.DiaInhabilRepository;
import com.juridico.sistema_juridico.repository.configuracion.SuspensionPlazoRepository;

class CalendarioInstitucionalServiceTest {

    @Test
    void suspensionPublicaNuevaVersionConTodoElPeriodoCongelado() {
        DiaInhabilRepository dias = mock(DiaInhabilRepository.class);
        SuspensionPlazoRepository suspensiones = mock(SuspensionPlazoRepository.class);
        CalendarioVersionRepository versiones = mock(CalendarioVersionRepository.class);
        when(dias.findAllByOrderByFechaAsc()).thenReturn(List.of());
        when(suspensiones.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(suspensiones.findByActivoTrueOrderByFechaInicioAsc()).thenAnswer(inv -> List.of(
                SuspensionPlazo.builder()
                        .fechaInicio(LocalDate.of(2026, 12, 21))
                        .fechaFin(LocalDate.of(2026, 12, 23)).activo(true).build()));
        when(versiones.findTopByOrderByVersionDesc()).thenReturn(Optional.of(
                CalendarioVersion.builder().version(7).fechasInhabiles("")
                        .motivoCambio("anterior").publicadoPor("SIAJ").build()));
        when(versiones.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CalendarioInstitucionalService servicio = new CalendarioInstitucionalService(dias, suspensiones, versiones);

        SuspensionPlazo nueva = SuspensionPlazo.builder()
                .fechaInicio(LocalDate.of(2026, 12, 21))
                .fechaFin(LocalDate.of(2026, 12, 23))
                .motivo("Suspensión institucional").build();
        servicio.guardarSuspension(nueva, "Jefa Jurídica");

        verify(versiones).save(any(CalendarioVersion.class));
        assertEquals("Jefa Jurídica", nueva.getRegistradoPor());
        assertTrue(servicio.serializar(servicio.deserializar("2026-12-21,2026-12-22,2026-12-23"))
                .contains("2026-12-23"));
    }
}
