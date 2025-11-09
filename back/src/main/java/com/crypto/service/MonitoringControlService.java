package com.crypto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ✅ Atualizado - Adicionado UserActivityTracker e registro de atividade
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringControlService {

    private final CryptoMonitoringService cryptoMonitoringService;
    private final UserActivityTracker activityTracker; // ✅ NOVO: Rastreador de atividade

    private final Map<String, ScheduledFuture<?>> activeMonitors = new ConcurrentHashMap<>();
    private final Map<String, Lock> userLocks = new ConcurrentHashMap<>();
    private final Map<String, MonitoringMetadata> monitoringMetadata = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler = createTaskScheduler();

    /**
     * ✅ CORRIGIDO - Auto-stop e registro de atividade
     */
    public boolean startMonitoring(String username, String userEmail) {
        if (username == null || username.trim().isEmpty()) {
            log.error("❌ Username não pode ser nulo ou vazio");
            return false;
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("❌ Email não pode ser nulo ou vazio");
            return false;
        }

        Lock userLock = userLocks.computeIfAbsent(username, k -> new ReentrantLock());
        userLock.lock();

        try {
            // ✅ Parar monitoramento anterior, se existir
            if (isMonitoringActiveInternal(username)) {
                log.info("♻️ Monitoramento já ativo para {}, reiniciando...", username);
                stopMonitoring(username);
            }

            log.info("🚀 Iniciando monitoramento para: {} (email: {})", username, userEmail);

            ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(
                    () -> runMonitoringCycle(username, userEmail),
                    Duration.ofMinutes(5)
            );

            ScheduledFuture<?> existing = activeMonitors.putIfAbsent(username, scheduledTask);

            if (existing != null) {
                scheduledTask.cancel(false);
                log.warn("⚠️ Scheduler já existia para {}, cancelando duplicado", username);
                return false;
            }

            monitoringMetadata.put(username, new MonitoringMetadata(userEmail, Instant.now()));

            log.info("✅ Monitoramento INICIADO para: {} (email: {})", username, userEmail);

            // ✅ NOVO: registrar atividade do usuário
            activityTracker.recordActivity(username);

            runFirstCheckAsync(username, userEmail);
            return true;

        } catch (Exception e) {
            log.error("❌ Erro ao iniciar monitoramento para {}: {}", username, e.getMessage(), e);
            return false;
        } finally {
            userLock.unlock();
        }
    }

    public boolean stopMonitoring(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.error("❌ Username não pode ser nulo ou vazio");
            return false;
        }

        Lock userLock = userLocks.computeIfAbsent(username, k -> new ReentrantLock());
        userLock.lock();

        try {
            ScheduledFuture<?> scheduledTask = activeMonitors.get(username);

            if (scheduledTask == null) {
                log.warn("⚠️ Nenhum monitoramento ativo para: {}", username);
                return false;
            }

            if (scheduledTask.isCancelled() || scheduledTask.isDone()) {
                log.warn("⚠️ Scheduler já estava cancelado/finalizado para: {}", username);
                activeMonitors.remove(username);
                monitoringMetadata.remove(username);
                return false;
            }

            boolean cancelled = scheduledTask.cancel(false);

            if (cancelled) {
                activeMonitors.remove(username);
                monitoringMetadata.remove(username);
                log.info("🛑 Monitoramento PARADO para: {}", username);
                return true;
            } else {
                log.warn("⚠️ Falha ao cancelar scheduler para: {}", username);
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Erro ao parar monitoramento para {}: {}", username, e.getMessage(), e);
            return false;
        } finally {
            userLock.unlock();
        }
    }

    public boolean isMonitoringActive(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return isMonitoringActiveInternal(username);
    }

    private boolean isMonitoringActiveInternal(String username) {
        ScheduledFuture<?> task = activeMonitors.get(username);

        if (task == null) {
            return false;
        }

        boolean isActive = !task.isCancelled() && !task.isDone();

        if (!isActive) {
            activeMonitors.remove(username);
            monitoringMetadata.remove(username);
        }

        return isActive;
    }

    public Map<String, Object> getMonitoringStatus(String username) {
        boolean isActive = isMonitoringActive(username);
        MonitoringMetadata metadata = monitoringMetadata.get(username);

        return Map.of(
                "username", username,
                "active", isActive,
                "email", metadata != null ? metadata.getEmail() : "N/A",
                "startedAt", metadata != null ? metadata.getStartedAt().toEpochMilli() : 0,
                "totalActiveMonitors", activeMonitors.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    public Map<String, Object> getGlobalStatus() {
        return Map.of(
                "totalActiveMonitors", activeMonitors.size(),
                "activeUsers", activeMonitors.keySet(),
                "timestamp", System.currentTimeMillis(),
                "systemHealthy", true
        );
    }

    private void runMonitoringCycle(String username, String userEmail) {
        try {
            log.debug("🔄 Executando ciclo de monitoramento para: {}", username);

            if (!isMonitoringActiveInternal(username)) {
                log.warn("⚠️ Monitoramento não está mais ativo para: {}", username);
                return;
            }

            cryptoMonitoringService.updateAndProcessAlertsForUser(userEmail);
            log.debug("✅ Ciclo concluído para: {}", username);

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento para {}: {}", username, e.getMessage(), e);
        }
    }

    private void runFirstCheckAsync(String username, String userEmail) {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                runMonitoringCycle(username, userEmail);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ Primeira verificação interrompida para: {}", username);
            }
        }, "FirstCheck-" + username).start();
    }

    public void stopAllMonitoring() {
        log.info("🛑 Parando todos os monitoramentos ativos ({} usuários)...", activeMonitors.size());

        activeMonitors.keySet().stream()
                .toList()
                .forEach(this::stopMonitoring);

        log.info("✅ Todos os monitoramentos parados");
    }

    @PreDestroy
    public void shutdown() {
        log.info("🔌 Encerrando MonitoringControlService...");
        stopAllMonitoring();

        if (taskScheduler instanceof ThreadPoolTaskScheduler scheduler) {
            scheduler.shutdown();
        }

        log.info("✅ MonitoringControlService encerrado");
    }

    private TaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("crypto-monitor-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.initialize();
        return scheduler;
    }

    private static class MonitoringMetadata {
        private final String email;
        private final Instant startedAt;

        public MonitoringMetadata(String email, Instant startedAt) {
            this.email = email;
            this.startedAt = startedAt;
        }

        public String getEmail() {
            return email;
        }

        public Instant getStartedAt() {
            return startedAt;
        }
    }
}
