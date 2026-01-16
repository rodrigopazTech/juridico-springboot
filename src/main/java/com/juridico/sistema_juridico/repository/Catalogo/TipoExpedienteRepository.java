package com.juridico.sistema_juridico.repository.Catalogo;

import com.juridico.sistema_juridico.Entity.catalogo.TipoExpediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TipoExpedienteRepository extends JpaRepository<TipoExpediente, Integer> {
    
    List<TipoExpediente> findByActivoTrueOrderByNombreAsc();
}