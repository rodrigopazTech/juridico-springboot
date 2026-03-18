package com.juridico.sistema_juridico.repository.Catalogo;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatPrioridadRepository extends JpaRepository<CatPrioridad, Integer> {
    Optional<CatPrioridad> findByNombreIgnoreCase(String nombre);
}
