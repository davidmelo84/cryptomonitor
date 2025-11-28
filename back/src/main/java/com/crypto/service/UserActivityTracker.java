package com.crypto.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class UserActivityTracker {

    private final Map<String, UserActivity> activeUsers = new ConcurrentHashMap<>();
    private final MonitoringControlService monitoringService;

    private static final long INACTIVITY_THRESHOLD_MS = 60 * 60 * 1000; // 15 minutos
    private static final long HEARTBEAT_TIMEOUT_MS = 3 * 60 * 1000; // 3 minutos

    public UserActivityTracker(@Lazy MonitoringControlService monitoringService) {
        this.monitoringService = monitoringService;
        log.info("✅ UserActivityTracker inicializado");
        log.info("   Threshold de inatividade: {} minutos",
                INACTIVITY_THRESHOLD_MS / 60000);
    }


    public void recordActivity(String username) {
        UserActivity activity = activeUsers.computeIfAbsent(
                username,
                k -> new UserActivity(username)
        );

        activity.updateActivity();

        log.debug("👤 Atividade registrada: {}", username);
    }


    public void receiveHeartbeat(String username) {
        recordActivity(username);

        UserActivity activity = activeUsers.get(username);
        if (activity != null) {
            activity.heartbeatReceived();
        }

        log.debug("💓 Heartbeat recebido: {}", username);
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutos
    public void checkInactiveUsers() {
        log.debug("🔍 Verificando usuários inativos...");

        Instant now = Instant.now();
        int inactiveCount = 0;
        int stoppedCount = 0;

        for (Map.Entry<String, UserActivity> entry : activeUsers.entrySet()) {
            String username = entry.getKey();
            UserActivity activity = entry.getValue();

            if (activity.isInactive(INACTIVITY_THRESHOLD_MS)) {
                inactiveCount++;

                if (activity.hasActiveMonitoring() &&
                        monitoringService.isMonitoringActive(username)) {

                    log.warn("⏸️ Parando monitoramento de usuário inativo: {} " +
                                    "(última atividade: {})",
                            username, activity.getLastActivityDuration());

                    try {
                        monitoringService.stopMonitoring(username);
                        activity.monitoringStopped();
                        stoppedCount++;

                    } catch (Exception e) {
                        log.error("❌ Erro ao parar monitoramento de {}: {}",
                                username, e.getMessage());
                    }
                }

                if (activity.isInactive(60 * 60 * 1000)) {
                    log.info("🗑️ Removendo usuário muito inativo: {}", username);
                    activeUsers.remove(username);
                }
            }

            if (activity.heartbeatTimeout(HEARTBEAT_TIMEOUT_MS)) {
                log.warn("⚠️ Heartbeat timeout para: {} (última: {})",
                        username, activity.getLastHeartbeatDuration());


            }
        }

        if (inactiveCount > 0 || stoppedCount > 0) {
            log.info("📊 Verificação concluída: {} inativos, {} monitoramentos parados",
                    inactiveCount, stoppedCount);
        }
    }


    public boolean isUserActive(String username) {
        UserActivity activity = activeUsers.get(username);
        return activity != null && !activity.isInactive(INACTIVITY_THRESHOLD_MS);
    }

    public void recordLogout(String username) {
        log.info("👋 Logout registrado: {}", username);

        if (monitoringService.isMonitoringActive(username)) {
            try {
                monitoringService.stopMonitoring(username);
            } catch (Exception e) {
                log.error("❌ Erro ao parar monitoramento no logout: {}", e.getMessage());
            }
        }

        activeUsers.remove(username);
    }


    public Map<String, Object> getStats() {
        int totalUsers = activeUsers.size();
        int activeNow = (int) activeUsers.values().stream()
                .filter(a -> !a.isInactive(INACTIVITY_THRESHOLD_MS))
                .count();

        int withMonitoring = (int) activeUsers.values().stream()
                .filter(UserActivity::hasActiveMonitoring)
                .count();

        return Map.of(
                "totalTracked", totalUsers,
                "activeNow", activeNow,
                "withMonitoring", withMonitoring,
                "inactiveThresholdMinutes", INACTIVITY_THRESHOLD_MS / 60000,
                "heartbeatTimeoutMinutes", HEARTBEAT_TIMEOUT_MS / 60000
        );
    }


    private static class UserActivity {
        private final String username;
        private volatile Instant lastActivity;
        private volatile Instant lastHeartbeat;
        private volatile boolean activeMonitoring;

        UserActivity(String username) {
            this.username = username;
            this.lastActivity = Instant.now();
            this.lastHeartbeat = Instant.now();
            this.activeMonitoring = false;
        }

        void updateActivity() {
            this.lastActivity = Instant.now();
        }

        void heartbeatReceived() {
            this.lastHeartbeat = Instant.now();
        }

        void monitoringStarted() {
            this.activeMonitoring = true;
        }

        void monitoringStopped() {
            this.activeMonitoring = false;
        }

        boolean hasActiveMonitoring() {
            return activeMonitoring;
        }

        boolean isInactive(long thresholdMs) {
            return Duration.between(lastActivity, Instant.now()).toMillis() > thresholdMs;
        }

        boolean heartbeatTimeout(long timeoutMs) {
            return Duration.between(lastHeartbeat, Instant.now()).toMillis() > timeoutMs;
        }

        String getLastActivityDuration() {
            long minutes = Duration.between(lastActivity, Instant.now()).toMinutes();
            return minutes + " minutos atrás";
        }

        String getLastHeartbeatDuration() {
            long minutes = Duration.between(lastHeartbeat, Instant.now()).toMinutes();
            return minutes + " minutos atrás";
        }
    }
}