package com.juridico.sistema_juridico.repository.Catalogo;

import com.juridico.sistema_juridico.Entity.catalogo.Estado;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EstadoRepository extends JpaRepository<Estado, Integer> {
    // Para que salgan ordenados alfabéticamente en el select
    @Cacheable("estados")
    List<Estado> findAllByOrderByNombreAsc();
    
    List<Estado> findByActivoTrue();
    List<Estado> findAllByActivoTrueOrderByNombreAsc();

    java.util.Optional<Estado> findByNombreIgnoreCase(String nombre);
    
    List<Estado> findByGerenciaId(Integer gerenciaId);
    List<Estado> findByGerenciaIdAndActivoTrue(Integer gerenciaId);
    
    List<Estado> findByGerenciaIsNull();
    List<Estado> findByGerenciaIsNullAndActivoTrue();
}