package com.juridico.sistema_juridico.repository.configuracion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;

@Repository
public interface ConfiguracionAlertaRepository extends JpaRepository<ConfiguracionAlerta, Integer> {

    List<ConfiguracionAlerta> findByTipoEventoAndActivaTrue(TipoEventoAlerta tipoEvento);

    @Query("""
            SELECT DISTINCT c.diasAnticipacion
            FROM ConfiguracionAlerta c
            WHERE c.tipoEvento = :tipoEvento AND c.activa = true
            """)
    List<Integer> findDiasActivosByTipoEvento(@Param("tipoEvento") TipoEventoAlerta tipoEvento);
}
