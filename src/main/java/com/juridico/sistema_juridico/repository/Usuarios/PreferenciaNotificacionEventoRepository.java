package com.juridico.sistema_juridico.repository.Usuarios;

import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.usuario.PreferenciaNotificacionEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreferenciaNotificacionEventoRepository extends JpaRepository<PreferenciaNotificacionEvento, Integer> {
    List<PreferenciaNotificacionEvento> findByUsuarioId(Integer usuarioId);

    Optional<PreferenciaNotificacionEvento> findByUsuarioIdAndEvento(Integer usuarioId, NotificacionEvento evento);

    @Modifying
    @Query("DELETE FROM PreferenciaNotificacionEvento p WHERE p.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
