package com.juridico.sistema_juridico.repository.Expediente;

import com.juridico.sistema_juridico.Entity.expediente.BitacoraDato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BitacoraDatoRepository extends JpaRepository<BitacoraDato, Long> {
    List<BitacoraDato> findByExpedienteIdOrderByFechaDesc(UUID expedienteId);
}
