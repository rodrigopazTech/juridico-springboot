package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.expediente.ActividadExpediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActividadExpedienteRepository extends JpaRepository<ActividadExpediente, Long> {
    List<ActividadExpediente> findByExpedienteIdOrderByFechaRegistroDesc(UUID expedienteId);
    
    List<ActividadExpediente> findTop15ByUsuarioIdOrderByFechaRegistroDesc(Integer usuarioId);
    List<ActividadExpediente> findTop100ByUsuarioIdOrderByFechaRegistroDesc(Integer usuarioId);
    
    List<ActividadExpediente> findTop15ByUsuarioIdOrExpedienteAbogadoResponsableIdOrderByFechaRegistroDesc(Integer usuarioId, Integer responsableId);

}