package com.juridico.sistema_juridico.repository.Catalogo;

import com.juridico.sistema_juridico.Entity.catalogo.TipoExpediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoExpedienteRepository extends JpaRepository<TipoExpediente, Integer> {
    Optional<TipoExpediente> findByNombreIgnoreCase(String nombre);
    
    java.util.List<TipoExpediente> findByGerenciaIdAndActivoTrue(Integer gerenciaId);
    
    java.util.List<TipoExpediente> findByGerenciaIsNullAndActivoTrue();
}
