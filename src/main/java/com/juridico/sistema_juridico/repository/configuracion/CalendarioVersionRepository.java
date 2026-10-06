package com.juridico.sistema_juridico.repository.configuracion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.juridico.sistema_juridico.Entity.configuracion.CalendarioVersion;

public interface CalendarioVersionRepository extends JpaRepository<CalendarioVersion, Integer> {
    Optional<CalendarioVersion> findTopByOrderByVersionDesc();
}
