package com.juridico.sistema_juridico.repository.procesal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.juridico.sistema_juridico.Entity.procesal.TerminoPresentado;
import java.util.List;
import java.util.UUID; 

@Repository
public interface TerminoPresentadoRepository extends JpaRepository<TerminoPresentado, Integer> {
    
    // Este es el método que te faltaba para descargar el acuse
    List<TerminoPresentado> findByTerminoExpedienteId(UUID expedienteId);
}