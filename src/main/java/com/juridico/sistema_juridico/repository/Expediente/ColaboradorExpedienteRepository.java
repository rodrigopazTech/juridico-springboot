package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.expediente.ColaboradorExpediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ColaboradorExpedienteRepository extends JpaRepository<ColaboradorExpediente, Integer> {

    // 1. Ver colaboradores activos de un expediente
    List<ColaboradorExpediente> findByExpedienteIdAndFechaExpiracionAfter(UUID expedienteId, LocalDateTime ahora);

    // 2. Verificar si el usuario tiene permiso temporal vigente
    boolean existsByExpedienteIdAndUsuarioIdAndFechaExpiracionAfter(
            UUID expedienteId, 
            Integer usuarioId, 
            LocalDateTime ahora
    );

    // 3. Buscar permiso específico para editarlo (El que usas en AudienciasController)
    List<ColaboradorExpediente> findByExpedienteIdAndUsuarioId(UUID expedienteId, Integer usuarioId);
}