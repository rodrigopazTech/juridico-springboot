package com.juridico.sistema_juridico.repository.Expediente;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.expediente.DocumentoExpediente;

@Repository
public interface DocumentoExpedienteRepository extends JpaRepository<DocumentoExpediente, Integer> {
     List<DocumentoExpediente> findByExpedienteId(java.util.UUID expedienteId);
}