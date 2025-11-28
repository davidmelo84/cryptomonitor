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

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringControlService {

    private final CryptoMonitoringService cryptoMonitoringService;
    private final UserActivityTracker activityTracker;

    private final Map<String, ScheduledFuture<?>> activeMonitors = new ConcurrentHashMap<>();
    private final Map<String, Lock> userLocks = new ConcurrentHashMap<>();
    private final Map<String, MonitoringMetadata> monitoringMetadata = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler = createTaskScheduler();


    public boolean startMonitoring(String username, String userEmail) {
        if (username == null || username.isBlank()) return false;
        if (userEmail == null || userEmail.isBlank()) return false;

        Lock lock = userLocks.computeIfAbsent(username, k -> new ReentrantLock());
        lock.lock();

        try {
            return startMonitoring_INTERNAL(username, userEmail);
        } finally {
            lock.unlock();
        }
    }

    public boolean stopMonitoring(String username) {
        if (username == null || username.isBlank()) return false;

        Lock lock = userLocks.computeIfAbsent(username, k -> new ReentrantLock());
        lock.lock();

        try {
            return stopMonitoring_INTERNAL(username);
        } finally {
            lock.unlock();
        }
    }


    private boolean startMonitoring_INTERNAL(String username, String userEmail) {

        if (isMonitoringActiveInternal(username)) {
            stopMonitoring_INTERNAL(username);
        }

        log.info("🚀 Iniciando monitoramento para {}...", username);

        ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(
                () -> runMonitoringCycle(username, userEmail),
                Duration.ofMinutes(5)
        );

        activeMonitors.put(username, scheduledTask);
        monitoringMetadata.put(username, new MonitoringMetadata(userEmail, Instant.now()));

        activityTracker.recordActivity(username);

        runFirstCheckAsync(username, userEmail);

        return true;
    }

    private boolean stopMonitoring_INTERNAL(String username) {
        ScheduledFuture<?> scheduledTask = activeMonitors.get(username);

        if (scheduledTask == null) {
            log.warn("⚠️ Nenhum monitoramento ativo para {}", username);
            return false;
        }

        boolean cancelled = scheduledTask.cancel(false);

        activeMonitors.remove(username);
        monitoringMetadata.remove(username);

        log.info("🛑 Monitoramento PARADO para {}", username);

        return cancelled;
    }


    public boolean isMonitoringActive(String username) {
        if (username == null || username.isBlank()) return false;
        return isMonitoringActiveInternal(username);
    }

    private boolean isMonitoringActiveInternal(String username) {
        ScheduledFuture<?> task = activeMonitors.get(username);

        if (task == null) return false;

        boolean active = !task.isCancelled() && !task.isDone();

        if (!active) {
            activeMonitors.remove(username);
            monitoringMetadata.remove(username);
        }

        return active;
    }

    public Map<String, Object> getMonitoringStatus(String username) {
        MonitoringMetadata meta = monitoringMetadata.get(username);

        return Map.of(
                "username", username,
                "active", isMonitoringActive(username),
                "email", meta != null ? meta.getEmail() : "N/A",
                "startedAt", meta != null ? meta.getStartedAt().toEpochMilli() : 0,
                "totalActiveMonitors", activeMonitors.size()
        );
    }

    public Map<String, Object> getGlobalStatus() {
        return Map.of(
                "totalActiveMonitors", activeMonitors.size(),
                "activeUsers", activeMonitors.keySet(),
                "systemHealthy", true
        );
    }


    private void runMonitoringCycle(String username, String email) {
        try {
            if (!isMonitoringActiveInternal(username)) return;

            cryptoMonitoringService.updateAndProcessAlertsForUser(email);

        } catch (Exception e) {
            log.error("Erro no ciclo para {}: {}", username, e.getMessage(), e);
        }
    }

    private void runFirstCheckAsync(String username, String email) {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                runMonitoringCycle(username, email);
            } catch (Exception ignored) {}
        }, "FirstCheck-" + username).start();
    }


    public void stopAllMonitoring() {
        activeMonitors.keySet().forEach(this::stopMonitoring);
    }

    @PreDestroy
    public void shutdown() {
        stopAllMonitoring();

        if (taskScheduler instanceof ThreadPoolTaskScheduler scheduler) {
            scheduler.shutdown();
        }
    }

    private TaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("crypto-monitor-");
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

        public String getEmail() { return email; }
        public Instant getStartedAt() { return startedAt; }
    }
}
