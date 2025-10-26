// back/src/main/java/com/crypto/service/MonitoringControlService.java
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
 * ✅ REFATORADO - THREAD-SAFE COM LOCKS
 *
 * CORREÇÕES:
 * - Lock por usuário para evitar race conditions
 * -putIfAbsent() atômico ao invés de containsKey() + put()
 * - Shutdown graceful com @PreDestroy
 * - Validação de estado antes de operações
 * - Logs detalhados para debugging
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringControlService {

    private final CryptoMonitoringService cryptoMonitoringService;

    // ✅ MANTIDO: Armazena schedulers ativos
    private final Map<String, ScheduledFuture<?>> activeMonitors = new ConcurrentHashMap<>();

    // ✅ NOVO: Locks por usuário para operações atômicas
    private final Map<String, Lock> userLocks = new ConcurrentHashMap<>();

    // ✅ NOVO: Metadata dos monitoramentos (timestamp, email)
    private final Map<String, MonitoringMetadata> monitoringMetadata = new ConcurrentHashMap<>();

    // ✅ MANTIDO: Task scheduler configurável
    private final TaskScheduler taskScheduler = createTaskScheduler();

    /**
     * ✅ REFATORADO - Thread-safe com lock por usuário
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

        // ✅ NOVO: Obter lock específico do usuário (thread-safe)
        Lock userLock = userLocks.computeIfAbsent(username, k -> new ReentrantLock());

        userLock.lock();
        try {
            // ✅ NOVO: Verificação atômica dentro do lock
            if (isMonitoringActiveInternal(username)) {
                log.warn("⚠️ Monitoramento já ativo para usuário: {}", username);
                return false;
            }

            log.info("🚀 Iniciando monitoramento para: {} (email: {})", username, userEmail);

            // ✅ MELHORADO: Criar e agendar tarefa
            ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(
                    () -> runMonitoringCycle(username, userEmail),
                    Duration.ofMinutes(5)
            );

            // ✅ NOVO: Usar putIfAbsent para garantir atomicidade
            ScheduledFuture<?> existing = activeMonitors.putIfAbsent(username, scheduledTask);

            if (existing != null) {
                // ✅ NOVO: Se já existia, cancelar a nova tarefa
                scheduledTask.cancel(false);
                log.warn("⚠️ Scheduler já existia para {}, cancelando duplicado", username);
                return false;
            }

            // ✅ NOVO: Armazenar metadata
            monitoringMetadata.put(username, new MonitoringMetadata(userEmail, Instant.now()));

            log.info("✅ Monitoramento INICIADO para: {} (email: {})", username, userEmail);

            // ✅ NOVO: Executar primeira verificação imediatamente (em thread separada)
            runFirstCheckAsync(username, userEmail);

            return true;

        } catch (Exception e) {
            log.error("❌ Erro ao iniciar monitoramento para {}: {}", username, e.getMessage(), e);
            return false;

        } finally {
            userLock.unlock();
        }
    }

    /**
     * ✅ REFATORADO - Thread-safe com lock por usuário
     */
    public boolean stopMonitoring(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.error("❌ Username não pode ser nulo ou vazio");
            return false;
        }

        // ✅ NOVO: Obter lock específico do usuário
        Lock userLock = userLocks.computeIfAbsent(username, k -> new ReentrantLock());

        userLock.lock();
        try {
            ScheduledFuture<?> scheduledTask = activeMonitors.get(username);

            if (scheduledTask == null) {
                log.warn("⚠️ Nenhum monitoramento ativo para: {}", username);
                return false;
            }

            // ✅ MELHORADO: Verificar se já está cancelado
            if (scheduledTask.isCancelled() || scheduledTask.isDone()) {
                log.warn("⚠️ Scheduler já estava cancelado/finalizado para: {}", username);
                activeMonitors.remove(username);
                monitoringMetadata.remove(username);
                return false;
            }

            // ✅ MELHORADO: Cancelar tarefa com flag false (não interromper se rodando)
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

    /**
     * ✅ REFATORADO - Verificação thread-safe
     */
    public boolean isMonitoringActive(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return isMonitoringActiveInternal(username);
    }

    /**
     * ✅ NOVO - Verificação interna thread-safe
     */
    private boolean isMonitoringActiveInternal(String username) {
        ScheduledFuture<?> task = activeMonitors.get(username);

        if (task == null) {
            return false;
        }

        // ✅ MELHORADO: Verificar se está realmente ativo
        boolean isActive = !task.isCancelled() && !task.isDone();

        // ✅ NOVO: Limpar se estiver inativo
        if (!isActive) {
            activeMonitors.remove(username);
            monitoringMetadata.remove(username);
        }

        return isActive;
    }

    /**
     * ✅ MELHORADO - Status detalhado com metadata
     */
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

    /**
     * ✅ NOVO - Status global do sistema
     */
    public Map<String, Object> getGlobalStatus() {
        return Map.of(
                "totalActiveMonitors", activeMonitors.size(),
                "activeUsers", activeMonitors.keySet(),
                "timestamp", System.currentTimeMillis(),
                "systemHealthy", true
        );
    }

    /**
     * ✅ REFATORADO - Execução com try-catch individual
     */
    private void runMonitoringCycle(String username, String userEmail) {
        try {
            log.debug("🔄 Executando ciclo de monitoramento para: {}", username);

            // ✅ MELHORADO: Verificar se ainda está ativo antes de executar
            if (!isMonitoringActiveInternal(username)) {
                log.warn("⚠️ Monitoramento não está mais ativo para: {}", username);
                return;
            }

            // Delega para o serviço de monitoramento
            cryptoMonitoringService.updateAndProcessAlertsForUser(userEmail);

            log.debug("✅ Ciclo concluído para: {}", username);

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento para {}: {}", username, e.getMessage(), e);
            // ✅ NOVO: Não cancelar automaticamente em caso de erro
            // Deixar continuar tentando nos próximos ciclos
        }
    }

    /**
     * ✅ NOVO - Primeira verificação assíncrona
     */
    private void runFirstCheckAsync(String username, String userEmail) {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Delay de 2s para não sobrecarregar
                runMonitoringCycle(username, userEmail);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ Primeira verificação interrompida para: {}", username);
            }
        }, "FirstCheck-" + username).start();
    }

    /**
     * ✅ REFATORADO - Parar todos com locks
     */
    public void stopAllMonitoring() {
        log.info("🛑 Parando todos os monitoramentos ativos ({} usuários)...", activeMonitors.size());

        // ✅ NOVO: Copiar keys para evitar ConcurrentModificationException
        activeMonitors.keySet().stream()
                .toList() // Snapshot das keys
                .forEach(this::stopMonitoring);

        log.info("✅ Todos os monitoramentos parados");
    }

    /**
     * ✅ NOVO - Shutdown graceful
     */
    @PreDestroy
    public void shutdown() {
        log.info("🔌 Encerrando MonitoringControlService...");

        stopAllMonitoring();

        if (taskScheduler instanceof ThreadPoolTaskScheduler) {
            ((ThreadPoolTaskScheduler) taskScheduler).shutdown();
        }

        log.info("✅ MonitoringControlService encerrado");
    }

    /**
     * ✅ MANTIDO - Criação do TaskScheduler
     */
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

    /**
     * ✅ NOVO - Classe interna para metadata
     */
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