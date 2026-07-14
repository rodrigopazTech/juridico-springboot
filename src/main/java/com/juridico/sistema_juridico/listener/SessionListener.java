package com.juridico.sistema_juridico.listener;

import com.juridico.sistema_juridico.repository.Usuarios.PushSubscriptionRepository;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SessionListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(SessionListener.class);

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.debug("Sesión HTTP creada: {}", se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        log.info("Sesión HTTP destruida: {}", sessionId);
        try {
            subscriptionRepository.deleteBySessionId(sessionId);
            log.info("Suscripciones Web Push asociadas a la sesión {} eliminadas correctamente", sessionId);
        } catch (Exception e) {
            log.error("Error al eliminar las suscripciones de la sesión destruida: {}", sessionId, e);
        }
    }
}
