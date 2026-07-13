package com.juridico.sistema_juridico.controller.api;

import com.juridico.sistema_juridico.Entity.usuario.PushSubscription;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.PushSubscriptionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webpush")
public class WebPushController {

    private static final Logger log = LoggerFactory.getLogger(WebPushController.class);

    @Value("${app.webpush.public-key}")
    private String publicKey;

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WebPushService webPushService;

    // =========================================================================
    // CODIGO DE PRUEBA: Endpoint temporal para probar el envio de notificaciones push
    // =========================================================================
    @GetMapping("/test-send")
    public ResponseEntity<?> testSend() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Usuario no encontrado"));
        }
        
        webPushService.enviarNotificacion(
            usuario, 
            "Notificación de Prueba", 
            "Esta es una notificación push de prueba enviada a las " + java.time.LocalTime.now(), 
            "/dashboard"
        );
        
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Push de prueba enviado a " + email));
    }
    // =========================================================================

    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }

    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<?> subscribe(@RequestBody SubscriptionRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }

        if (request.getEndpoint() == null || request.getKeys() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Datos de suscripción inválidos"));
        }

        // Buscar si ya existe una suscripción para este usuario en este dispositivo (deviceId)
        Optional<PushSubscription> existingByDevice = Optional.empty();
        if (request.getDeviceId() != null && !request.getDeviceId().trim().isEmpty()) {
            existingByDevice = subscriptionRepository.findByUsuarioIdAndDeviceId(usuario.getId(), request.getDeviceId());
        }

        // Buscar si existe una suscripción por el endpoint
        Optional<PushSubscription> existingByEndpoint = subscriptionRepository.findByEndpoint(request.getEndpoint());

        PushSubscription subToSave;

        if (existingByDevice.isPresent()) {
            subToSave = existingByDevice.get();
            subToSave.setEndpoint(request.getEndpoint());
            subToSave.setP256dh(request.getKeys().getP256dh());
            subToSave.setAuth(request.getKeys().getAuth());
            log.info("Actualizando suscripción Web Push por Device ID para el usuario: {}", email);
        } else if (existingByEndpoint.isPresent()) {
            subToSave = existingByEndpoint.get();
            subToSave.setUsuario(usuario);
            subToSave.setDeviceId(request.getDeviceId());
            subToSave.setP256dh(request.getKeys().getP256dh());
            subToSave.setAuth(request.getKeys().getAuth());
            log.info("Reutilizando suscripción Web Push por Endpoint para el usuario: {}", email);
        } else {
            subToSave = PushSubscription.builder()
                    .usuario(usuario)
                    .endpoint(request.getEndpoint())
                    .p256dh(request.getKeys().getP256dh())
                    .auth(request.getKeys().getAuth())
                    .deviceId(request.getDeviceId())
                    .build();
            log.info("Nueva suscripción Web Push registrada para el usuario: {}", email);
        }

        subscriptionRepository.save(subToSave);

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Endpoint requerido"));
        }

        subscriptionRepository.findByEndpoint(endpoint).ifPresent(sub -> {
            subscriptionRepository.delete(sub);
            log.info("Suscripción Web Push eliminada para endpoint: {}", endpoint);
        });

        return ResponseEntity.ok(Map.of("success", true));
    }

    public static class SubscriptionRequest {
        private String endpoint;
        private String deviceId;
        private Keys keys;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public Keys getKeys() { return keys; }
        public void setKeys(Keys keys) { this.keys = keys; }

        public static class Keys {
            private String p256dh;
            private String auth;

            public String getP256dh() { return p256dh; }
            public void setP256dh(String p256dh) { this.p256dh = p256dh; }
            public String getAuth() { return auth; }
            public void setAuth(String auth) { this.auth = auth; }
        }
    }
}
