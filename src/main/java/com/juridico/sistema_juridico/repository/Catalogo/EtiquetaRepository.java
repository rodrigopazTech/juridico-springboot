package com.juridico.sistema_juridico.repository.Catalogo;

import com.juridico.sistema_juridico.Entity.catalogo.Etiqueta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EtiquetaRepository extends JpaRepository<Etiqueta, Integer> {
    Optional<Etiqueta> findByNombreIgnoreCase(String nombre);
}
