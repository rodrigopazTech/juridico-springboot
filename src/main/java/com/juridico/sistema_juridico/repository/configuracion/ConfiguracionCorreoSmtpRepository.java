package com.juridico.sistema_juridico.repository.configuracion;

import com.juridico.sistema_juridico.Entity.configuracion.ConfiguracionCorreoSmtp;
import com.juridico.sistema_juridico.Entity.enums.EstadoConfiguracionCorreo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracionCorreoSmtpRepository extends JpaRepository<ConfiguracionCorreoSmtp, Long> {

    Optional<ConfiguracionCorreoSmtp> findFirstByEstadoOrderByFechaActivacionDesc(
            EstadoConfiguracionCorreo estado);

    List<ConfiguracionCorreoSmtp> findAllByOrderByFechaCreacionDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfiguracionCorreoSmtp c order by c.id")
    List<ConfiguracionCorreoSmtp> findAllForUpdate();
}
