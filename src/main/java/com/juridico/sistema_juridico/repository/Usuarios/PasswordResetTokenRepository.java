package com.juridico.sistema_juridico.repository.Usuarios;

import com.juridico.sistema_juridico.Entity.usuario.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findFirstByTokenAndActivoTrue(String token);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.activo = false WHERE t.usuario.id = :usuarioId AND t.usado = false AND t.activo = true")
    int invalidateActiveTokensByUsuarioId(@Param("usuarioId") Integer usuarioId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.usado = false AND t.fechaExpiracion < CURRENT_TIMESTAMP")
    int deleteExpiredUnusedTokens();
}
