package com.juridico.sistema_juridico.repository.Usuarios;

import com.juridico.sistema_juridico.Entity.usuario.ActividadUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActividadUsuarioRepository extends JpaRepository<ActividadUsuario, Long> {
    List<ActividadUsuario> findTop15ByUsuarioIdOrderByFechaRegistroDesc(Integer usuarioId);
}
