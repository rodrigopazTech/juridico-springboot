package com.juridico.sistema_juridico.controller.api;

import com.juridico.sistema_juridico.Entity.usuario.PushSubscription;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.PushSubscriptionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
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

        Optional<PushSubscription> existing = subscriptionRepository.findByEndpoint(request.getEndpoint());
        if (existing.isPresent()) {
            PushSubscription sub = existing.get();
            sub.setUsuario(usuario);
            sub.setP256dh(request.getKeys().getP256dh());
            sub.setAuth(request.getKeys().getAuth());
            subscriptionRepository.save(sub);
            log.info("Suscripción Web Push actualizada para el usuario: {}", email);
        } else {
            PushSubscription sub = PushSubscription.builder()
                    .usuario(usuario)
                    .endpoint(request.getEndpoint())
                    .p256dh(request.getKeys().getP256dh())
                    .auth(request.getKeys().getAuth())
                    .build();
            subscriptionRepository.save(sub);
            log.info("Nueva suscripción Web Push registrada para el usuario: {}", email);
        }

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
        private Keys keys;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
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
