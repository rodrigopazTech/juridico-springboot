package com.juridico.sistema_juridico.repository.Usuarios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.usuario.EventoCalendario;

@Repository
public interface EventoCalendarioRepository extends JpaRepository<EventoCalendario, Integer> {
    List<EventoCalendario> findByUsuarioId(Integer usuarioId);
}