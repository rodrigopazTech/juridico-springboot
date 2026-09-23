package com.juridico.sistema_juridico.repository.configuracion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;

public interface PerfilAlertaTerminoRepository extends JpaRepository<PerfilAlertaTermino, Integer> {
    List<PerfilAlertaTermino> findAllByOrderByNombreAscVersionDesc();
    List<PerfilAlertaTermino> findByEstadoOrderByNombreAsc(EstadoPerfil estado);
    List<PerfilAlertaTermino> findByEstadoAndModoOrderByNombreAsc(EstadoPerfil estado, ModoPerfilAlerta modo);
    Optional<PerfilAlertaTermino> findFirstByCodigoOrderByVersionDesc(String codigo);
    boolean existsByCodigoAndVersion(String codigo, Integer version);
}
