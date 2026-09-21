package com.juridico.sistema_juridico.repository.Catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;

@Repository
public interface TipoTerminoRepository extends JpaRepository<TipoTermino, Integer> {

    List<TipoTermino> findByActivoTrueOrderByNombreAsc();

    List<TipoTermino> findByMateriaIdAndActivoTrueOrderByNombreAsc(Integer materiaId);

    Optional<TipoTermino> findByNombreIgnoreCase(String nombre);
}
