// back/src/main/java/com/crypto/service/CryptoMonitoringService.java
package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.event.CryptoUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ✅ VERSÃO FINAL - SCHEDULER SEGURO
 *
 * GARANTIAS:
 * - Apenas 1 execução por vez (lock)
 * - Intervalo de 30 minutos FIXO
 * - Fallback automático se API falhar
 * - Sem requests duplicados
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoMonitoringService {

    private final CryptoService cryptoService;
    private final ApplicationEventPublisher eventPublisher;
    private final WebSocketService webSocketService;

    private final Lock schedulerLock = new ReentrantLock();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private LocalDateTime lastSuccessfulRun = null;

    private static final long SCHEDULER_INTERVAL_MS = 1800000; // 30 minutos

    /**
     * ✅ SCHEDULER ÚNICO - 30 MINUTOS
     *
     * MUDANÇAS:
     * - Lock para prevenir concorrência
     * - Flag isRunning para skip durante execução
     * - Timeout de 5 minutos para adquirir lock
     */
    @Scheduled(fixedRate = SCHEDULER_INTERVAL_MS, initialDelay = 60000)
    public void scheduledUpdate() {
        // ✅ 1. Skip se já está rodando
        if (isRunning.get()) {
            log.warn("⚠️ Scheduler já em execução, pulando ciclo");
            return;
        }

        boolean lockAcquired = false;
        try {
            // ✅ 2. Tentar adquirir lock (timeout 5 min)
            lockAcquired = schedulerLock.tryLock(5, java.util.concurrent.TimeUnit.MINUTES);

            if (!lockAcquired) {
                log.error("❌ Timeout ao aguardar lock do scheduler");
                return;
            }

            // ✅ 3. Marcar como em execução
            isRunning.set(true);

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("⏰ SCHEDULER: Iniciando atualização periódica");
            log.info("   Última execução: {}",
                    lastSuccessfulRun != null ? lastSuccessfulRun : "Primeira vez");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // ✅ 4. Executar atualização
            updateAndProcessAlerts();

            // ✅ 5. Registrar sucesso
            lastSuccessfulRun = LocalDateTime.now();
            log.info("✅ Scheduler concluído às {}", lastSuccessfulRun);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Scheduler interrompido: {}", e.getMessage());

        } catch (Exception e) {
            log.error("❌ Erro no scheduler: {}", e.getMessage(), e);

        } finally {
            // ✅ 6. Sempre liberar recursos
            isRunning.set(false);
            if (lockAcquired) {
                schedulerLock.unlock();
            }
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    /**
     * ✅ ATUALIZAÇÃO GLOBAL
     *
     * Usa getCurrentPrices() que:
     * - Verifica cache primeiro (TTL 30min)
     * - Se cache expirado, enfileira request
     * - Se request falhar, usa banco
     */
    public void updateAndProcessAlerts() {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento...");

            // ✅ CRÍTICO: Este método USA CACHE + FILA
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            if (currentCryptos.isEmpty()) {
                log.error("❌ NENHUM DADO DISPONÍVEL (cache + banco + API vazios)");
                log.error("   Sistema sem dados para processar!");
                return;
            }

            log.info("📊 Obtidos {} criptomoedas", currentCryptos.size());

            // Publicar evento (alertas)
            publishCryptoUpdateEvent(
                    currentCryptos,
                    null,
                    CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE
            );

            // Broadcast via WebSocket
            webSocketService.broadcastPrices(currentCryptos);

            log.info("✅ Ciclo concluído");

        } catch (Exception e) {
            log.error("❌ Erro no ciclo: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ ATUALIZAÇÃO POR USUÁRIO
     *
     * Usa os mesmos dados do cache global
     */
    public void updateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🔄 Processando alertas para: {}", userEmail);

            // ✅ USA CACHE - sem request extra
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            if (currentCryptos.isEmpty()) {
                log.error("❌ Sem dados para: {}", userEmail);
                return;
            }

            // Publicar evento
            publishCryptoUpdateEvent(
                    currentCryptos,
                    userEmail,
                    CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE
            );

            log.info("✅ Alertas processados para: {}", userEmail);

        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {}: {}",
                    userEmail, e.getMessage());
        }
    }

    /**
     * ⚠️ FORCE UPDATE - ADMIN APENAS
     *
     * ATENÇÃO: Consome rate limit!
     * Use apenas em emergências.
     */
    public void forceUpdateAndProcessAlerts() {
        if (isRunning.get()) {
            throw new IllegalStateException(
                    "Scheduler em execução. Aguarde o ciclo terminar."
            );
        }

        boolean lockAcquired = false;
        try {
            lockAcquired = schedulerLock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS);

            if (!lockAcquired) {
                throw new IllegalStateException("Timeout ao aguardar lock");
            }

            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("⚠️ FORCE UPDATE SOLICITADO!");
            log.warn("   Consumindo rate limit do CoinGecko...");
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Limpar cache para forçar nova request
            cryptoService.clearCache();

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            publishCryptoUpdateEvent(
                    currentCryptos,
                    null,
                    CryptoUpdateEvent.UpdateType.MANUAL_UPDATE
            );

            webSocketService.broadcastPrices(currentCryptos);

            log.warn("✅ Force update concluído: {} moedas (RATE LIMIT CONSUMIDO!)",
                    currentCryptos.size());
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Force update interrompido", e);

        } catch (Exception e) {
            log.error("❌ Erro no force update: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no force update", e);

        } finally {
            if (lockAcquired) {
                schedulerLock.unlock();
            }
        }
    }

    /**
     * ✅ Publicar evento (desacoplado)
     */
    private void publishCryptoUpdateEvent(
            List<CryptoCurrency> cryptos,
            String userEmail,
            CryptoUpdateEvent.UpdateType type) {

        try {
            CryptoUpdateEvent event = userEmail == null
                    ? new CryptoUpdateEvent(this, cryptos, type)
                    : new CryptoUpdateEvent(this, cryptos, userEmail, type);

            eventPublisher.publishEvent(event);

            log.debug("📤 Evento publicado: {} cryptos, tipo: {}",
                    cryptos.size(), type);

        } catch (Exception e) {
            log.error("❌ Erro ao publicar evento: {}", e.getMessage());
        }
    }

    /**
     * ✅ Estatísticas
     */
    public MonitoringStats getMonitoringStats() {
        try {
            List<CryptoCurrency> savedCryptos = cryptoService.getAllSavedCryptos();

            return MonitoringStats.builder()
                    .totalCryptocurrencies(savedCryptos.size())
                    .isSchedulerRunning(isRunning.get())
                    .lastSuccessfulRun(lastSuccessfulRun)
                    .lastUpdate(savedCryptos.isEmpty() ? null :
                            savedCryptos.get(0).getLastUpdated())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas: {}", e.getMessage());
            return MonitoringStats.builder()
                    .totalCryptocurrencies(0)
                    .isSchedulerRunning(false)
                    .build();
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class MonitoringStats {
        private int totalCryptocurrencies;
        private boolean isSchedulerRunning;
        private LocalDateTime lastSuccessfulRun;
        private LocalDateTime lastUpdate;
    }
}