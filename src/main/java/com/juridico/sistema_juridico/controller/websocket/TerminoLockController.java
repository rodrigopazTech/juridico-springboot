package com.juridico.sistema_juridico.controller.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class TerminoLockController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Mapa en memoria para almacenar los bloqueos: terminoId -> Map<email, nombreCompleto>
    private final Map<Integer, Map<String, String>> activeLocks = new ConcurrentHashMap<>();

    // Registro de bloqueos asociados a la sesión del WebSocket: sessionId -> SessionLockInfo
    private static class SessionLockInfo {
        final Integer terminoId;
        final String email;

        SessionLockInfo(Integer terminoId, String email) {
            this.terminoId = terminoId;
            this.email = email;
        }
    }
    private final Map<String, SessionLockInfo> sessionToLockMap = new ConcurrentHashMap<>();

    @MessageMapping("/terminos/lock")
    public void lockTermino(Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        if (payload == null || !payload.containsKey("terminoId")) {
            return;
        }

        try {
            Integer terminoId = Integer.valueOf(payload.get("terminoId").toString());
            String email = (principal != null) ? principal.getName() : "anonimo";
            String nombreCompleto = payload.containsKey("nombreCompleto") ? payload.get("nombreCompleto").toString() : email;
            String sessionId = headerAccessor.getSessionId();

            // Agregar el usuario a la lista de editores de este término
            activeLocks.computeIfAbsent(terminoId, k -> new ConcurrentHashMap<>()).put(email, nombreCompleto);

            if (sessionId != null) {
                sessionToLockMap.put(sessionId, new SessionLockInfo(terminoId, email));
            }

            broadcastLocks();
        } catch (Exception e) {
            // Log or ignore errors gracefully
        }
    }

    @MessageMapping("/terminos/unlock")
    public void unlockTermino(Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        if (payload == null || !payload.containsKey("terminoId")) {
            return;
        }

        try {
            Integer terminoId = Integer.valueOf(payload.get("terminoId").toString());
            String email = (principal != null) ? principal.getName() : "anonimo";
            String sessionId = headerAccessor.getSessionId();

            Map<String, String> users = activeLocks.get(terminoId);
            if (users != null) {
                users.remove(email);
                if (users.isEmpty()) {
                    activeLocks.remove(terminoId);
                }
            }

            if (sessionId != null) {
                sessionToLockMap.remove(sessionId);
            }

            broadcastLocks();
        } catch (Exception e) {
            // Log or ignore errors gracefully
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            SessionLockInfo info = sessionToLockMap.remove(sessionId);
            if (info != null) {
                Map<String, String> users = activeLocks.get(info.terminoId);
                if (users != null) {
                    users.remove(info.email);
                    if (users.isEmpty()) {
                        activeLocks.remove(info.terminoId);
                    }
                }
                broadcastLocks();
            }
        }
    }

    private void broadcastLocks() {
        messagingTemplate.convertAndSend("/topic/terminos/locks", activeLocks);
    }
}
