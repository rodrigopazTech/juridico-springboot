package com.juridico.sistema_juridico.service.scheduler;

import com.juridico.sistema_juridico.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler que purga automáticamente los documentos que llevan más de 3 meses
 * en la papelera temporal. Se ejecuta todos los días a las 02:00 AM.
 *
 * El proceso:
 *  1. Busca documentos con eliminado=true y fechaEliminado < (hoy - 3 meses).
 *  2. Elimina el archivo físico del filesystem.
 *  3. Elimina el registro de la base de datos.
 *
 * Este comportamiento es equivalente al DELETE existente, solo que diferido
 * en el tiempo.
 */
@Component
public class DocumentoCleanupScheduler {

    @Autowired
    private DocumentoService documentoService;

    /**
     * Purga diaria a las 02:00 AM.
     * Cron: segundo minuto hora día mes día-semana
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void purgarPapeleraTemporal() {
        try {
            int eliminados = documentoService.purgarDocumentosVencidos();
            if (eliminados > 0) {
                System.out.println("[Papelera] Purga automática completada: " + eliminados +
                        " documento(s) con más de 3 meses en papelera eliminados permanentemente.");
            }
        } catch (Exception e) {
            System.err.println("[Papelera] Error durante la purga automática: " + e.getMessage());
        }
    }
}
