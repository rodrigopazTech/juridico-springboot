package com.juridico.sistema_juridico.repository.Expediente;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.expediente.ActividadExpediente;

@Repository
public interface ActividadExpedienteRepository extends JpaRepository<ActividadExpediente, Integer> {
    List<ActividadExpediente> findByExpedienteIdOrderByFechaRegistroDesc(java.util.UUID expedienteId);
}