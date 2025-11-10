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
 * ✅ CORRIGIDO - RATE LIMIT RESPEITADO
 *
 * MUDANÇAS:
 * - Scheduler ÚNICO a cada 30 minutos
 * - Cache SEMPRE respeitado
 * - Sem requisições duplicadas
 * - Fallback para banco quando rate limit
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
     * ✅ SCHEDULER ÚNICO - Executa A CADA 30 MINUTOS
     *
     * GARANTE:
     * - Máximo 2 requisições/hora ao CoinGecko
     * - Cache de 30 min SEMPRE respeitado
     * - Sem execuções concorrentes
     */
    @Scheduled(fixedRate = SCHEDULER_INTERVAL_MS, initialDelay = 60000)
    public void scheduledUpdate() {
        // ✅ Prevenir execuções concorrentes
        if (isRunning.get()) {
            log.warn("⚠️ Scheduler já em execução, pulando ciclo");
            return;
        }

        boolean lockAcquired = false;
        try {
            lockAcquired = schedulerLock.tryLock(10, java.util.concurrent.TimeUnit.SECONDS);

            if (!lockAcquired) {
                log.error("❌ Timeout ao aguardar lock do scheduler");
                return;
            }

            isRunning.set(true);

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("⏰ SCHEDULER: Iniciando atualização periódica");
            log.info("   Última execução: {}",
                    lastSuccessfulRun != null ? lastSuccessfulRun : "Primeira vez");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            updateAndProcessAlerts();

            lastSuccessfulRun = LocalDateTime.now();
            log.info("✅ Scheduler concluído às {}", lastSuccessfulRun);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Scheduler interrompido: {}", e.getMessage());

        } catch (Exception e) {
            log.error("❌ Erro no scheduler: {}", e.getMessage(), e);

        } finally {
            isRunning.set(false);
            if (lockAcquired) {
                schedulerLock.unlock();
            }
        }
    }

    /**
     * ✅ ATUALIZAÇÃO GLOBAL (USA CACHE!)
     *
     * NÃO FAZ REQUEST SE:
     * - Cache ainda válido (< 30 min)
     * - Dados já foram buscados neste ciclo
     */
    public void updateAndProcessAlerts() {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento...");

            // ✅ CRÍTICO: getCurrentPrices() USA CACHE!
            // Se cache válido: 0 requests ao CoinGecko
            // Se cache expirado: 1 request ao CoinGecko
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            if (currentCryptos.isEmpty()) {
                log.warn("⚠️ Nenhuma crypto obtida, usando fallback do banco");
                currentCryptos = cryptoService.getAllSavedCryptos();

                if (currentCryptos.isEmpty()) {
                    log.error("❌ Sem dados disponíveis (cache + banco vazios)");
                    return;
                }
            }

            log.info("📊 Obtidos preços de {} criptomoedas", currentCryptos.size());

            // Salvar no banco (fallback futuro)
            currentCryptos.forEach(cryptoService::saveCrypto);

            // Publicar evento (alertas)
            publishCryptoUpdateEvent(
                    currentCryptos,
                    null,
                    CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE
            );

            // Broadcast via WebSocket
            webSocketService.broadcastPrices(currentCryptos);

            log.info("✅ Ciclo de monitoramento concluído com sucesso");

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ ATUALIZAÇÃO POR USUÁRIO (USA CACHE!)
     *
     * NUNCA faz request extra - apenas usa dados do cache
     */
    public void updateAndProcessAlertsForUser(String userEmail) {
        if (isRunning.get()) {
            log.debug("⏸️ Scheduler em execução, usando cache para: {}", userEmail);
        }

        try {
            log.info("🔄 Processando alertas para: {}", userEmail);

            // ✅ USA CACHE - sem request extra
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            if (currentCryptos.isEmpty()) {
                log.warn("⚠️ Cache vazio, usando banco para: {}", userEmail);
                currentCryptos = cryptoService.getAllSavedCryptos();
            }

            if (currentCryptos.isEmpty()) {
                log.error("❌ Sem dados disponíveis para: {}", userEmail);
                return;
            }

            // Salvar no banco
            currentCryptos.forEach(cryptoService::saveCrypto);

            // Publicar evento
            publishCryptoUpdateEvent(
                    currentCryptos,
                    userEmail,
                    CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE
            );

            log.info("✅ Alertas processados para: {}", userEmail);

        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {}: {}",
                    userEmail, e.getMessage(), e);
        }
    }

    /**
     * ⚠️ FORCE UPDATE - ADMIN APENAS
     *
     * ATENÇÃO: Consome rate limit!
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

            log.warn("⚠️ FORCE UPDATE solicitado! Consumindo rate limit...");

            // Limpar cache para forçar nova request
            cryptoService.clearCache();

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
            currentCryptos.forEach(cryptoService::saveCrypto);

            publishCryptoUpdateEvent(
                    currentCryptos,
                    null,
                    CryptoUpdateEvent.UpdateType.MANUAL_UPDATE
            );

            webSocketService.broadcastPrices(currentCryptos);

            log.warn("✅ Force update concluído: {} moedas (rate limit consumido!)",
                    currentCryptos.size());

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
     * ✅ ATUALIZAÇÃO DE UMA CRYPTO (USA CACHE!)
     */
    public void processAlertsForCrypto(String coinId) {
        try {
            // ✅ USA CACHE - sem request extra
            cryptoService.getCryptoByCoinId(coinId)
                    .ifPresentOrElse(
                            crypto -> {
                                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);

                                publishCryptoUpdateEvent(
                                        List.of(savedCrypto),
                                        null,
                                        CryptoUpdateEvent.UpdateType.SINGLE_CRYPTO
                                );

                                webSocketService.sendCryptoUpdate(savedCrypto);
                                log.info("✅ Alertas processados para {}", coinId);
                            },
                            () -> log.warn("⚠️ Criptomoeda {} não encontrada no cache", coinId)
                    );
        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {}: {}", coinId, e.getMessage());
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
            log.error("❌ Erro ao publicar evento: {}", e.getMessage(), e);
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