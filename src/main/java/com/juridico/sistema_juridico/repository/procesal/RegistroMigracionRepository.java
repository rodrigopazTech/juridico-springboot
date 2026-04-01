package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.RegistroMigracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RegistroMigracionRepository extends JpaRepository<RegistroMigracion, Long> {
    
    // Trae los registros de un lote que NO estén en la papelera (RECHAZADO)
    List<RegistroMigracion> findByLoteIdAndEstatusNot(Long loteId, String estatusExcluido);
    
    long countByLoteId(Long loteId);

    // Trae los registros de un lote filtrados por tipo (TERMINO o AUDIENCIA)
    List<RegistroMigracion> findByLoteIdAndTipoDatoAndEstatusNot(Long loteId, String tipoDato, String estatusExcluido);

    // Para nuestra futura Papelera (Trae solo los rechazados)
    List<RegistroMigracion> findByEstatusOrderByCreatedAtDesc(String estatus);
    
    // Agrupa los registros por número de expediente (Útil para la Etapa 2)
    List<RegistroMigracion> findByLoteIdAndNumeroExpedienteExtraidoAndEstatusNot(Long loteId, String numeroExpediente, String estatusExcluido);
}