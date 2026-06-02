package com.juridico.sistema_juridico.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class RealtimeNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeNotificationService.class);
    private static final String CHANNEL_NAME = "canal_notificaciones_realtime";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final ExecutorService listenerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "postgres-event-listener");
        thread.setDaemon(true);
        return thread;
    });

    private Connection dedicatedConnection;
    private volatile boolean running = true;

    @PostConstruct
    public void startListener() {
        log.info("Iniciando servicio de escucha en tiempo real para PostgreSQL...");
        listenerExecutor.submit(this::listenLoop);
    }

    private void listenLoop() {
        int reconnectAttempts = 0;

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                establishDedicatedConnection();
                reconnectAttempts = 0; // Resetear intentos al conectar con éxito

                try (Statement stmt = dedicatedConnection.createStatement()) {
                    log.info("Ejecutando comando LISTEN en el canal: {}", CHANNEL_NAME);
                    stmt.execute("LISTEN " + CHANNEL_NAME + ";");
                }

                PGConnection pgConn = dedicatedConnection.unwrap(PGConnection.class);

                while (running && !Thread.currentThread().isInterrupted()) {
                    // Consulta de control periódica para forzar la lectura del socket y 
                    // recibir las notificaciones asíncronas pendientes del servidor.
                    try (Statement dummyStmt = dedicatedConnection.createStatement()) {
                        dummyStmt.execute("SELECT 1;");
                    }

                    PGNotification[] notifications = pgConn.getNotifications();

                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            String jsonPayload = notification.getParameter();
                            log.info("🔔 Evento recibido de Postgres [Canal: {}]: {}", 
                                    notification.getName(), jsonPayload);
                            
                            // Propagar el evento a la capa de red (WebSockets STOMP)
                            dispatchToNetworkLayer(jsonPayload);
                        }
                    }

                    // Evitar consumo excesivo de CPU (Polling controlado de 500ms)
                    TimeUnit.MILLISECONDS.sleep(500);
                }

            } catch (SQLException e) {
                log.error("Error en la conexión dedicada de PostgreSQL. Intentando reconectar...", e);
                closeResources();
                handleReconnectionBackoff(++reconnectAttempts);
            } catch (InterruptedException e) {
                log.warn("El hilo de escucha de eventos ha sido interrumpido.");
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Establece una conexión física directa e independiente de HikariCP
     */
    private void establishDedicatedConnection() throws SQLException {
        log.info("Abriendo conexión dedicada con la base de datos para eventos asíncronos en URL: {}", dbUrl);
        this.dedicatedConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Punto de integración con la capa de red (WebSockets STOMP)
     * Notificaciones se envían al canal privado del usuario destinatario.
     * Otros eventos (expedientes) se envían al canal global.
     */
    private void dispatchToNetworkLayer(String jsonPayload) {
        try {
            JsonNode node = objectMapper.readTree(jsonPayload);
            String tabla = node.path("tabla").asText();
            
            if ("notificaciones".equals(tabla)) {
                if (node.has("usuario_id")) {
                    int usuarioId = node.get("usuario_id").asInt();
                    var opt = usuarioRepository.findById(usuarioId);
                    if (opt.isPresent()) {
                        String email = opt.get().getEmail();
                        if (email != null && !email.trim().isEmpty()) {
                            log.info("Despachando notificación privada al usuario '{}' en '/queue/alertas'", email);
                            this.messagingTemplate.convertAndSendToUser(email, "/queue/alertas", jsonPayload);
                            return;
                        }
                    } else {
                        log.warn("No se encontró el usuario con ID {} para enviar la notificación realtime.", usuarioId);
                    }
                }
            } else if ("expedientes".equals(tabla) || "documentos".equals(tabla) || "carpetas".equals(tabla)
                    || "comentarios".equals(tabla) || "actividad_expedientes".equals(tabla)) {
                String expedienteId = null;
                if ("expedientes".equals(tabla) && node.has("id")) {
                    expedienteId = node.get("id").asText();
                } else if (node.has("expediente_id")) {
                    expedienteId = node.get("expediente_id").asText();
                }
                
                if (expedienteId != null && !expedienteId.trim().isEmpty()) {
                    String dest = "/topic/expedientes/" + expedienteId;
                    log.info("Despachando evento de expediente al canal '{}'", dest);
                    this.messagingTemplate.convertAndSend(dest, jsonPayload);
                    
                    // Notificar también al canal general de expedientes
                    if ("expedientes".equals(tabla)) {
                        this.messagingTemplate.convertAndSend("/topic/expedientes", jsonPayload);
                    }
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Error al procesar el payload de notificación para envío segmentado", e);
        }

        log.info("Despachando evento JSON al broker de WebSockets en '/topic/alertas' (global)");
        this.messagingTemplate.convertAndSend("/topic/alertas", jsonPayload);
    }

    private void handleReconnectionBackoff(int attempt) {
        // Backoff exponencial para evitar saturar el servidor (máximo de 30 segundos)
        long sleepTime = Math.min((long) Math.pow(2, attempt) * 1000L, 30000L);
        log.info("Reintento de conexión #{} en {} ms...", attempt, sleepTime);
        try {
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
            log.warn("Espera de reconexión interrumpida.");
            Thread.currentThread().interrupt();
        }
    }

    private void closeResources() {
        if (dedicatedConnection != null) {
            try {
                if (!dedicatedConnection.isClosed()) {
                    dedicatedConnection.close();
                }
            } catch (SQLException e) {
                log.error("Error al cerrar la conexión dedicada de PostgreSQL", e);
            }
        }
    }

    @PreDestroy
    public void stopListener() {
        log.info("Deteniendo el Listener de PostgreSQL de forma segura...");
        this.running = false;
        closeResources();
        listenerExecutor.shutdownNow();
        try {
            if (!listenerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("El pool del Listener no se detuvo en el tiempo esperado.");
            }
        } catch (InterruptedException e) {
            log.error("Interrupción durante el apagado del servicio", e);
            Thread.currentThread().interrupt();
        }
        log.info("Servicio de notificaciones en tiempo real apagado.");
    }
}
