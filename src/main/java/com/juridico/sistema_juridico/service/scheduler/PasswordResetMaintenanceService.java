package com.juridico.sistema_juridico.service.scheduler;

import com.juridico.sistema_juridico.repository.Usuarios.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetMaintenanceService {

    private final PasswordResetTokenRepository tokenRepository;

    @Scheduled(cron = "${app.password-reset.cleanup.cron:0 0/30 * * * *}")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = tokenRepository.deleteExpiredUnusedTokens();
        if (deleted > 0) {
            log.info("Tokens de recuperacion expirados eliminados: {}", deleted);
        }
    }
}
