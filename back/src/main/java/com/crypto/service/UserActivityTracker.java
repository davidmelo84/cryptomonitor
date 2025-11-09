package com.crypto.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ RASTREADOR DE ATIVIDADE DO USUÁRIO
 *
 * FUNCIONALIDADES:
 * - Detecta inatividade (padrão: 15 minutos)
 * - Para schedulers automaticamente
 * - Heartbeat via WebSocket
 * - Auto-cleanup de sessões inativas
 *
 * INTEGRAÇÃO:
 * - Frontend envia heartbeat a cada 60 segundos
 * - Backend verifica a cada 5 minutos
 * - Usuários inativos têm monitoramento pausado
 */
@Slf4j
@Service
public class UserActivityTracker {

    private final Map<String, UserActivity> activeUsers = new ConcurrentHashMap<>();
    private final MonitoringControlService monitoringService;

    private static final long INACTIVITY_THRESHOLD_MS = 15 * 60 * 1000; // 15 minutos
    private static final long HEARTBEAT_TIMEOUT_MS = 3 * 60 * 1000; // 3 minutos

    public UserActivityTracker(MonitoringControlService monitoringService) {
        this.monitoringService = monitoringService;
        log.info("✅ UserActivityTracker inicializado");
        log.info("   Threshold de inatividade: {} minutos",
                INACTIVITY_THRESHOLD_MS / 60000);
    }

    /**
     * ✅ REGISTRAR ATIVIDADE DO USUÁRIO
     *
     * Chamado:
     * - Login
     * - Qualquer request autenticado
     * - Heartbeat do WebSocket
     */
    public void recordActivity(String username) {
        UserActivity activity = activeUsers.computeIfAbsent(
                username,
                k -> new UserActivity(username)
        );

        activity.updateActivity();

        log.debug("👤 Atividade registrada: {}", username);
    }

    /**
     * ✅ HEARTBEAT (WebSocket)
     *
     * Frontend deve enviar a cada 60 segundos:
     *
     * stompClient.send("/app/heartbeat", {}, JSON.stringify({
     *   username: currentUser,
     *   timestamp: Date.now()
     * }));
     */
    public void receiveHeartbeat(String username) {
        recordActivity(username);

        UserActivity activity = activeUsers.get(username);
        if (activity != null) {
            activity.heartbeatReceived();
        }

        log.debug("💓 Heartbeat recebido: {}", username);
    }

    /**
     * ✅ VERIFICAR INATIVIDADE
     *
     * Executado a cada 5 minutos
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutos
    public void checkInactiveUsers() {
        log.debug("🔍 Verificando usuários inativos...");

        Instant now = Instant.now();
        int inactiveCount = 0;
        int stoppedCount = 0;

        for (Map.Entry<String, UserActivity> entry : activeUsers.entrySet()) {
            String username = entry.getKey();
            UserActivity activity = entry.getValue();

            // ✅ 1. Verificar se usuário está inativo
            if (activity.isInactive(INACTIVITY_THRESHOLD_MS)) {
                inactiveCount++;

                // ✅ 2. Parar monitoramento se estiver ativo
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

                // ✅ 3. Remover se muito tempo inativo (> 1 hora)
                if (activity.isInactive(60 * 60 * 1000)) {
                    log.info("🗑️ Removendo usuário muito inativo: {}", username);
                    activeUsers.remove(username);
                }
            }

            // ✅ 4. Verificar se heartbeat parou (WebSocket desconectado)
            if (activity.heartbeatTimeout(HEARTBEAT_TIMEOUT_MS)) {
                log.warn("⚠️ Heartbeat timeout para: {} (última: {})",
                        username, activity.getLastHeartbeatDuration());

                // Marcar como possivelmente inativo
                // (pode estar em outra aba, então não para imediatamente)
            }
        }

        if (inactiveCount > 0 || stoppedCount > 0) {
            log.info("📊 Verificação concluída: {} inativos, {} monitoramentos parados",
                    inactiveCount, stoppedCount);
        }
    }

    /**
     * ✅ VERIFICAR SE USUÁRIO ESTÁ ATIVO
     */
    public boolean isUserActive(String username) {
        UserActivity activity = activeUsers.get(username);
        return activity != null && !activity.isInactive(INACTIVITY_THRESHOLD_MS);
    }

    /**
     * ✅ REGISTRAR LOGOUT
     */
    public void recordLogout(String username) {
        log.info("👋 Logout registrado: {}", username);

        // Parar monitoramento
        if (monitoringService.isMonitoringActive(username)) {
            try {
                monitoringService.stopMonitoring(username);
            } catch (Exception e) {
                log.error("❌ Erro ao parar monitoramento no logout: {}", e.getMessage());
            }
        }

        // Remover da lista
        activeUsers.remove(username);
    }

    /**
     * ✅ ESTATÍSTICAS
     */
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

    // =========================================
    // CLASSE AUXILIAR
    // =========================================

    /**
     * Rastreamento de atividade individual
     */
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