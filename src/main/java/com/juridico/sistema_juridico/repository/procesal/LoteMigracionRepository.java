package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.LoteMigracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LoteMigracionRepository extends JpaRepository<LoteMigracion, Long> {
    
    // Busca si hay un lote de trabajo a medias (Para recuperar el progreso)
    Optional<LoteMigracion> findFirstByEstatusOrderByFechaCreacionDesc(String estatus);
}