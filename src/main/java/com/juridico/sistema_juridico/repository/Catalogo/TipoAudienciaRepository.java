package com.juridico.sistema_juridico.repository.Catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;

@Repository
public interface TipoAudienciaRepository extends JpaRepository<TipoAudiencia, Integer> {
    List<TipoAudiencia> findByActivoTrue();
}