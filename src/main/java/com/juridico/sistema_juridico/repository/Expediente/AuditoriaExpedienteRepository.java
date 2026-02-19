package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.expediente.AuditoriaExpediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditoriaExpedienteRepository extends JpaRepository<AuditoriaExpediente, UUID> {
    List<AuditoriaExpediente> findByExpedienteIdOrderByCreatedAtDesc(UUID expedienteId);
}
