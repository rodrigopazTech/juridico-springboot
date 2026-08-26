package com.juridico.sistema_juridico.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juridico.sistema_juridico.Entity.usuario.PushSubscription;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.listener.SessionListener;
import com.juridico.sistema_juridico.repository.Usuarios.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    @Value("${app.webpush.public-key}")
    private String publicKey;

    @Value("${app.webpush.private-key}")
    private String privateKey;

    @Value("${app.webpush.subject}")
    private String subject;

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private PushService pushService;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(publicKey) || !StringUtils.hasText(privateKey) || !StringUtils.hasText(subject)) {
            log.warn("Servicio Web Push deshabilitado: faltan claves VAPID o subject.");
            return;
        }

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            pushService = new PushService(publicKey, privateKey, subject);
            log.info("Servicio Web Push inicializado correctamente.");
        } catch (GeneralSecurityException e) {
            log.error("Error al inicializar PushService con claves VAPID", e);
        }
    }

    @Async
    public void enviarNotificacion(Usuario usuario, String titulo, String mensaje, String url) {
        if (pushService == null) {
            log.debug("Notificacion Web Push omitida: servicio no inicializado.");
            return;
        }

        List<PushSubscription> subscriptions = subscriptionRepository.findByUsuarioId(usuario.getId());
        if (subscriptions.isEmpty()) {
            return;
        }

        log.info("Enviando push a {} dispositivos del usuario {}", subscriptions.size(), usuario.getEmail());

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("title", titulo);
        payloadMap.put("body", mensaje);
        payloadMap.put("url", url);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (IOException e) {
            log.error("Error al serializar el payload de la notificación push", e);
            return;
        }

        for (PushSubscription sub : subscriptions) {
            if (!SessionListener.isSessionActive(sub.getSessionId())) {
                log.info("Eliminando suscripcion Web Push sin sesion activa. Usuario: {}, endpoint: {}",
                        usuario.getEmail(), sub.getEndpoint());
                subscriptionRepository.delete(sub);
                continue;
            }

            try {
                Subscription clientSub = new Subscription(
                        sub.getEndpoint(),
                        new Subscription.Keys(sub.getP256dh(), sub.getAuth())
                );

                Notification notification = new Notification(clientSub, payload);
                var response = pushService.send(notification);
                
                int statusCode = response.getStatusLine().getStatusCode();
                log.debug("Push enviado a {}. Estatus: {}", sub.getEndpoint(), statusCode);

            } catch (Exception e) {
                log.error("Fallo al enviar notificación push a endpoint: {}", sub.getEndpoint(), e);
                if (e.getMessage() != null && (e.getMessage().contains("410") || e.getMessage().contains("404"))) {
                    log.info("Eliminando suscripción push inválida: {}", sub.getEndpoint());
                    subscriptionRepository.delete(sub);
                }
            }
        }
    }
}
