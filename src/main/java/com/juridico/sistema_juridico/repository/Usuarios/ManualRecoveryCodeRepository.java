package com.juridico.sistema_juridico.repository.Usuarios;

import com.juridico.sistema_juridico.Entity.usuario.ManualRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManualRecoveryCodeRepository extends JpaRepository<ManualRecoveryCode, Long> {

    Optional<ManualRecoveryCode> findFirstByUsuarioIdAndActivoTrueAndUsadoFalseOrderByFechaCreacionDesc(Integer usuarioId);

    @Modifying
    @Query("UPDATE ManualRecoveryCode c SET c.activo = false WHERE c.usuario.id = :usuarioId AND c.activo = true")
    int invalidateActiveCodesByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
