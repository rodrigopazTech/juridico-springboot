package com.juridico.sistema_juridico.repository.General;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.general.Comentario;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Integer> {
    List<Comentario> findByEntidadTipoAndEntidadIdOrderByCreatedAtDesc(String tipo, String id);
}