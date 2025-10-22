package com.crypto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.time.Duration;


@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringControlService {

    private final CryptoMonitoringService cryptoMonitoringService;

    // Armazena os schedulers ativos por usuário
    private final Map<String, ScheduledFuture<?>> activeMonitors = new ConcurrentHashMap<>();

    // Task Scheduler configurável
    private final TaskScheduler taskScheduler = createTaskScheduler();

    /**
     * Inicia o monitoramento para um usuário específico
     */
    public synchronized boolean startMonitoring(String username, String userEmail) {
        // Verifica se já existe monitoramento ativo
        if (activeMonitors.containsKey(username)) {
            log.warn("Monitoramento já ativo para usuário: {}", username);
            return false;
        }

        try {
            // Agenda tarefa periódica (a cada 5 minutos)
            ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(
                    () -> runMonitoringCycle(username, userEmail),
                    Duration.ofMinutes(5) // ✅ Mais claro: 5 minutos
            );

            // Armazena a referência da tarefa
            activeMonitors.put(username, scheduledTask);

            log.info("✅ Monitoramento INICIADO para usuário: {} (email: {})", username, userEmail);

            // Executa primeira verificação imediatamente
            runMonitoringCycle(username, userEmail);

            return true;

        } catch (Exception e) {
            log.error("❌ Erro ao iniciar monitoramento para {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Para o monitoramento de um usuário específico
     */
    public synchronized boolean stopMonitoring(String username) {
        ScheduledFuture<?> scheduledTask = activeMonitors.get(username);

        if (scheduledTask == null) {
            log.warn("Nenhum monitoramento ativo para usuário: {}", username);
            return false;
        }

        try {
            // Cancela a tarefa agendada
            scheduledTask.cancel(false);
            activeMonitors.remove(username);

            log.info("🛑 Monitoramento PARADO para usuário: {}", username);
            return true;

        } catch (Exception e) {
            log.error("❌ Erro ao parar monitoramento para {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se o monitoramento está ativo para um usuário
     */
    public boolean isMonitoringActive(String username) {
        ScheduledFuture<?> task = activeMonitors.get(username);
        return task != null && !task.isCancelled() && !task.isDone();
    }

    /**
     * Retorna status detalhado do monitoramento
     */
    public Map<String, Object> getMonitoringStatus(String username) {
        boolean isActive = isMonitoringActive(username);

        return Map.of(
                "username", username,
                "active", isActive,
                "totalActiveMonitors", activeMonitors.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Executa um ciclo de monitoramento
     */
    private void runMonitoringCycle(String username, String userEmail) {
        try {
            log.info("🔄 Executando ciclo de monitoramento para: {}", username);

            // Delega para o serviço de monitoramento existente
            cryptoMonitoringService.updateAndProcessAlertsForUser(userEmail);

            log.info("✅ Ciclo concluído para: {}", username);

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento para {}: {}", username, e.getMessage());
        }
    }

    /**
     * Para todos os monitoramentos (útil para shutdown da aplicação)
     */
    public void stopAllMonitoring() {
        log.info("🛑 Parando todos os monitoramentos ativos...");
        activeMonitors.keySet().forEach(this::stopMonitoring);
    }

    /**
     * Cria e configura o TaskScheduler
     */
    private TaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // Até 10 monitoramentos simultâneos
        scheduler.setThreadNamePrefix("crypto-monitor-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }
}
