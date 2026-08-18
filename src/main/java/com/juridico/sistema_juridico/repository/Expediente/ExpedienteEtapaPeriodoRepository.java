package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.expediente.ExpedienteEtapaPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpedienteEtapaPeriodoRepository extends JpaRepository<ExpedienteEtapaPeriodo, Long> {

    Optional<ExpedienteEtapaPeriodo> findByExpedienteIdAndFechaFinIsNull(UUID expedienteId);

    List<ExpedienteEtapaPeriodo> findByExpedienteIdOrderByFechaInicioAsc(UUID expedienteId);

    List<ExpedienteEtapaPeriodo> findByExpedienteIdOrderByFechaInicioDesc(UUID expedienteId);
}
