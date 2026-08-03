package com.juridico.sistema_juridico.listener;

import com.juridico.sistema_juridico.repository.Usuarios.PushSubscriptionRepository;
import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(SessionListener.class);
    private static final Set<String> ACTIVE_SESSION_IDS = ConcurrentHashMap.newKeySet();

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @Autowired
    private AuditoriaTecnicaService auditoriaTecnicaService;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        ACTIVE_SESSION_IDS.add(sessionId);
        log.debug("Sesion HTTP creada: {}", sessionId);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        ACTIVE_SESSION_IDS.remove(sessionId);
        log.info("Sesion HTTP destruida: {}", sessionId);
        try {
            subscriptionRepository.deleteBySessionId(sessionId);
            log.info("Suscripciones Web Push asociadas a la sesion {} eliminadas correctamente", sessionId);
        } catch (Exception e) {
            log.error("Error al eliminar las suscripciones de la sesion destruida: {}", sessionId, e);
        }
        try {
            auditoriaTecnicaService.cerrarSesion(sessionId, "SESSION_DESTROYED");
        } catch (Exception e) {
            log.error("Error al cerrar la sesion de auditoria {}: {}", sessionId, e.getMessage());
        }
    }

    public static boolean isSessionActive(String sessionId) {
        return sessionId != null && ACTIVE_SESSION_IDS.contains(sessionId);
    }
}
