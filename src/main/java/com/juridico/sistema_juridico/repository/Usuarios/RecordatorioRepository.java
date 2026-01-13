package com.juridico.sistema_juridico.repository.Usuarios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;

@Repository
public interface RecordatorioRepository extends JpaRepository<Recordatorio, Integer> {
     List<Recordatorio> findByUsuarioIdAndCompletadoFalse(Integer usuarioId);
}