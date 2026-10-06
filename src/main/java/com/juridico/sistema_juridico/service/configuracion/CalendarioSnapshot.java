package com.juridico.sistema_juridico.service.configuracion;

import java.time.LocalDate;
import java.util.Set;

public record CalendarioSnapshot(Integer version, Set<LocalDate> fechasInhabiles) {
}
