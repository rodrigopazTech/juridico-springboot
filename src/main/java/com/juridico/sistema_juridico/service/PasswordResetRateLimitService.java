package com.juridico.sistema_juridico.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetRateLimitService {

    private final Map<String, Deque<Instant>> userRequests = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> ipRequests = new ConcurrentHashMap<>();

    private final int userMaxRequests;
    private final Duration userWindow;
    private final int ipMaxRequests;
    private final Duration ipWindow;

    public PasswordResetRateLimitService(
            @Value("${app.password-reset.rate-limit.user-max-requests:5}") int userMaxRequests,
            @Value("${app.password-reset.rate-limit.user-window-minutes:60}") int userWindowMinutes,
            @Value("${app.password-reset.rate-limit.ip-max-requests:10}") int ipMaxRequests,
            @Value("${app.password-reset.rate-limit.ip-window-minutes:60}") int ipWindowMinutes) {
        this.userMaxRequests = userMaxRequests;
        this.userWindow = Duration.ofMinutes(userWindowMinutes);
        this.ipMaxRequests = ipMaxRequests;
        this.ipWindow = Duration.ofMinutes(ipWindowMinutes);
    }

    public boolean isAllowed(String email, String ip) {
        Instant now = Instant.now();
        String emailKey = normalize(email);
        String ipKey = ip != null && !ip.isBlank() ? ip.trim() : "unknown";

        boolean userAllowed = isKeyAllowed(userRequests, emailKey, userMaxRequests, userWindow, now);
        boolean ipAllowed = isKeyAllowed(ipRequests, ipKey, ipMaxRequests, ipWindow, now);
        return userAllowed && ipAllowed;
    }

    private boolean isKeyAllowed(Map<String, Deque<Instant>> store, String key, int maxRequests,
            Duration window, Instant now) {
        Deque<Instant> requests = store.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (requests) {
            Instant threshold = now.minus(window);
            while (!requests.isEmpty() && requests.peekFirst().isBefore(threshold)) {
                requests.removeFirst();
            }
            if (requests.size() >= maxRequests) {
                return false;
            }
            requests.addLast(now);
            return true;
        }
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }
}
