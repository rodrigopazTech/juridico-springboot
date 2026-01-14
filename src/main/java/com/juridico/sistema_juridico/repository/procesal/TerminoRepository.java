package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.Termino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID; // Importante

@Repository
public interface TerminoRepository extends JpaRepository<Termino, Integer> {

    // Cambiado: 'estatusTermino' para que coincida con la Entity
    List<Termino> findByFechaVencimientoBeforeAndEstatusTerminoNot(LocalDate fecha, String estatus);

    // Cambiado: UUID para que coincida con el ID de Expediente
    List<Termino> findByExpedienteId(UUID expedienteId);
}