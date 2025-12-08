package com.juridico.sistema_juridico.repository;

import com.juridico.sistema_juridico.model.Termino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TerminoRepository extends JpaRepository<Termino, Long> {
    // Método para traer solo los términos que no han sido eliminados lógicamente
    List<Termino> findByActivoTrue();
}