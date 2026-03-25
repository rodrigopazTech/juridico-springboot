package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.CatalogoMigracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CatalogoMigracionRepository extends JpaRepository<CatalogoMigracion, Long> {
    
    // Para traer los catálogos vinculados a un lote
    List<CatalogoMigracion> findByLoteId(Long loteId);

    // Para revisar si un catálogo ya se guardó en este lote y no duplicarlo en la Etapa 1
    boolean existsByLoteIdAndTipoCatalogoAndValorOriginal(Long loteId, String tipoCatalogo, String valorOriginal);
}