package com.juridico.sistema_juridico.service.scheduler;

import com.juridico.sistema_juridico.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DocumentoPapeleraScheduler {

    @Autowired
    private DocumentoService documentoService;

    /** Se ejecuta todos los dias a las 02:30 AM para limpiar papelera vencida. */
    @Scheduled(cron = "0 30 2 * * ?")
    public void purgarDocumentosVencidos() {
        documentoService.purgarDocumentosVencidos();
    }
}
