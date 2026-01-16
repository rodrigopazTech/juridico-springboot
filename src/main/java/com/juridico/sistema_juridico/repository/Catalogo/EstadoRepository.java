package com.juridico.sistema_juridico.repository.Catalogo;

import com.juridico.sistema_juridico.Entity.catalogo.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EstadoRepository extends JpaRepository<Estado, Integer> {
    // Para que salgan ordenados alfabéticamente en el select
    List<Estado> findAllByOrderByNombreAsc();
}