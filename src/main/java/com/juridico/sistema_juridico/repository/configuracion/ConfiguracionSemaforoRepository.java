package com.juridico.sistema_juridico.repository.configuracion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionSemaforo;
import com.juridico.sistema_juridico.Entity.enums.NivelConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;

@Repository
public interface ConfiguracionSemaforoRepository extends JpaRepository<ConfiguracionSemaforo, Integer> {

    Optional<ConfiguracionSemaforo> findFirstByTipoEventoAndNivelAndGerenciaId(
            TipoEventoAlerta tipoEvento, NivelConfiguracionAlerta nivel, Integer gerenciaId);

    Optional<ConfiguracionSemaforo> findFirstByTipoEventoAndNivelAndGerenciaIsNull(
            TipoEventoAlerta tipoEvento, NivelConfiguracionAlerta nivel);

    List<ConfiguracionSemaforo> findByTipoEventoOrderByNivelAsc(TipoEventoAlerta tipoEvento);
}
