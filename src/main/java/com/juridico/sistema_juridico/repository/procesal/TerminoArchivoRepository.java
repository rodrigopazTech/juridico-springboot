package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.enums.TipoDocumentoTermino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerminoArchivoRepository extends JpaRepository<TerminoArchivo, Long> {

    List<TerminoArchivo> findByTerminoIdOrderByCreatedAtDesc(Integer terminoId);

    Optional<TerminoArchivo> findFirstByTerminoIdAndTipoDocumentoOrderByCreatedAtDesc(Integer terminoId,
                                                                                       TipoDocumentoTermino tipoDocumento);
}
