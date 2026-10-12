package com.juridico.sistema_juridico.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Scheduler dedicado para pulsos de latido (heartbeats) STOMP
        ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
        heartbeatScheduler.initialize();

        // Habilita un broker simple en memoria en /topic y /queue con Heartbeats
        // [20000, 20000] -> Envia heartbeat cada 20s y espera recibir heartbeat cada 20s
        // Esto mantiene vivas las conexiones a traves de proxies inversos (Nginx) y firewalls sin caidas
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(heartbeatScheduler)
                .setHeartbeatValue(new long[]{20000, 20000});
        
        // Prefijo para enviar mensajes desde los clientes al backend
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint seguro para conexión con SockJS
        registry.addEndpoint("/ws-realtime")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                StompCommand command = accessor.getCommand();
                if (command != null) {
                    Principal principal = accessor.getUser();
                    String username = (principal != null) ? principal.getName() : "ANONIMO";
                    
                    if (StompCommand.CONNECT.equals(command)) {
                        log.info("🔌 WebSocket CONNECT - Usuario: {}", username);
                    } else if (StompCommand.SUBSCRIBE.equals(command)) {
                        log.info("📥 WebSocket SUBSCRIBE - Canal: {} - Usuario: {}", 
                                accessor.getDestination(), username);
                    }
                }
                return message;
            }
        });
    }
}
