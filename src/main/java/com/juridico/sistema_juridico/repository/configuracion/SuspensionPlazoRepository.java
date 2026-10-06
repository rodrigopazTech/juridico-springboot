package com.juridico.sistema_juridico.repository.configuracion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.juridico.sistema_juridico.Entity.configuracion.SuspensionPlazo;

public interface SuspensionPlazoRepository extends JpaRepository<SuspensionPlazo, Integer> {
    List<SuspensionPlazo> findAllByOrderByFechaInicioDesc();
    List<SuspensionPlazo> findByActivoTrueOrderByFechaInicioAsc();
}
