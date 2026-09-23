package com.juridico.sistema_juridico.repository.configuracion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;

public interface PerfilRecordatorioAudienciaRepository extends JpaRepository<PerfilRecordatorioAudiencia, Integer> {
    List<PerfilRecordatorioAudiencia> findAllByOrderByNombreAscVersionDesc();
    List<PerfilRecordatorioAudiencia> findByEstadoOrderByNombreAsc(EstadoPerfil estado);
    Optional<PerfilRecordatorioAudiencia> findFirstByCodigoOrderByVersionDesc(String codigo);
    boolean existsByCodigoAndVersion(String codigo, Integer version);
}
