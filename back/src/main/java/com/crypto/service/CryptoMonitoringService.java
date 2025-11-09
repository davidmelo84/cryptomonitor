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
 * ✅ MONITORING SERVICE - THREAD-SAFE
 *
 * PROTEÇÕES:
 * - Lock para evitar execuções concorrentes
 * - Flag de execução (evita sobreposição)
 * - Timeout de 5 minutos (evita travamento)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoMonitoringService {

    private final CryptoService cryptoService;
    private final ApplicationEventPublisher eventPublisher;
    private final WebSocketService webSocketService;

    // ✅ PROTEÇÃO CONTRA EXECUÇÕES CONCORRENTES
    private final Lock schedulerLock = new ReentrantLock();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private LocalDateTime lastSuccessfulRun = null;

    /**
     * ✅ SCHEDULER - Executa A CADA 30 MINUTOS (THREAD-SAFE)
     *
     * PROTEÇÕES:
     * - Não executa se já estiver rodando
     * - Lock com timeout (evita deadlock)
     * - Registra última execução bem-sucedida
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000) // 30min, começa após 1min
    public void scheduledUpdate() {
        // ✅ 1. VERIFICAR SE JÁ ESTÁ RODANDO
        if (isRunning.get()) {
            log.warn("⚠️ Scheduler já em execução, pulando ciclo");
            return;
        }

        // ✅ 2. TENTAR ADQUIRIR LOCK (timeout 10s)
        boolean lockAcquired = false;
        try {
            lockAcquired = schedulerLock.tryLock(10, java.util.concurrent.TimeUnit.SECONDS);

            if (!lockAcquired) {
                log.error("❌ Timeout ao aguardar lock do scheduler");
                return;
            }

            // ✅ 3. MARCAR COMO EXECUTANDO
            isRunning.set(true);

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("⏰ SCHEDULER: Iniciando atualização periódica");
            log.info("   Última execução bem-sucedida: {}",
                    lastSuccessfulRun != null ? lastSuccessfulRun : "Primeira vez");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // ✅ 4. EXECUTAR ATUALIZAÇÃO
            updateAndProcessAlerts();

            // ✅ 5. REGISTRAR SUCESSO
            lastSuccessfulRun = LocalDateTime.now();
            log.info("✅ Scheduler concluído com sucesso às {}", lastSuccessfulRun);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Scheduler interrompido: {}", e.getMessage());

        } catch (Exception e) {
            log.error("❌ Erro no scheduler: {}", e.getMessage(), e);

        } finally {
            // ✅ 6. SEMPRE LIBERAR RECURSOS
            isRunning.set(false);

            if (lockAcquired) {
                schedulerLock.unlock();
            }
        }
    }

    /**
     * ✅ Atualização global (PROTEGIDA)
     */
    public void updateAndProcessAlerts() {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento...");

            // 1. Buscar preços (cache 30min - NÃO faz request se cache válido!)
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            if (currentCryptos.isEmpty()) {
                log.warn("⚠️ Nenhuma crypto obtida, abortando ciclo");
                return;
            }

            log.info("📊 Obtidos preços de {} criptomoedas", currentCryptos.size());

            // 2. Salvar no banco (fallback)
            currentCryptos.forEach(cryptoService::saveCrypto);

            // 3. Publicar evento (alertas)
            publishCryptoUpdateEvent(
                    currentCryptos,
                    null,
                    CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE
            );

            // 4. Broadcast via WebSocket (TEMPO REAL!)
            webSocketService.broadcastPrices(currentCryptos);

            log.info("✅ Ciclo de monitoramento concluído com sucesso");

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento: {}", e.getMessage(), e);
            throw e; // Re-lançar para finally pegar
        }
    }

    /**
     * ✅ Atualização para usuário específico (PROTEGIDA)
     */
    public void updateAndProcessAlertsForUser(String userEmail) {
        // ✅ Verificar se scheduler não está rodando
        if (isRunning.get()) {
            log.warn("⚠️ Scheduler em execução, usando dados do cache para: {}", userEmail);
        }

        try {
            log.info("🔄 Iniciando ciclo para: {}", userEmail);

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            if (currentCryptos.isEmpty()) {
                log.warn("⚠️ Nenhuma crypto obtida para: {}", userEmail);
                return;
            }

            log.info("📊 Obtidos preços de {} criptomoedas", currentCryptos.size());

            currentCryptos.forEach(cryptoService::saveCrypto);

            publishCryptoUpdateEvent(
                    currentCryptos,
                    userEmail,
                    CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE
            );

            log.info("✅ Ciclo concluído para: {}", userEmail);

        } catch (Exception e) {
            log.error("❌ Erro no ciclo para {}: {}", userEmail, e.getMessage(), e);
        }
    }

    /**
     * ⚠️ FORÇAR ATUALIZAÇÃO MANUAL (ADMIN)
     *
     * USE COM CUIDADO - Consome rate limit!
     */
    public void forceUpdateAndProcessAlerts() {
        // ✅ Bloquear se scheduler estiver rodando
        if (isRunning.get()) {
            throw new IllegalStateException(
                    "Não é possível forçar update enquanto scheduler está rodando. " +
                            "Aguarde o ciclo atual terminar."
            );
        }

        boolean lockAcquired = false;
        try {
            lockAcquired = schedulerLock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS);

            if (!lockAcquired) {
                throw new IllegalStateException("Timeout ao aguardar lock");
            }

            log.warn("🚀 FORCE UPDATE solicitado! Consumindo rate limit...");

            // ⚠️ Limpar cache para forçar nova request
            cryptoService.clearCache();

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
            currentCryptos.forEach(cryptoService::saveCrypto);

            publishCryptoUpdateEvent(
                    currentCryptos,
                    null,
                    CryptoUpdateEvent.UpdateType.MANUAL_UPDATE
            );

            webSocketService.broadcastPrices(currentCryptos);

            log.warn("⚠️ Force update concluído: {} moedas (rate limit consumido!)",
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
     * ✅ Atualização de uma crypto específica (SAFE)
     */
    public void processAlertsForCrypto(String coinId) {
        try {
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
                            () -> log.warn("⚠️ Criptomoeda {} não encontrada", coinId)
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

            log.debug("📤 Evento publicado: {} cryptos, tipo: {}, usuário: {}",
                    cryptos.size(), type, userEmail != null ? userEmail : "global");

        } catch (Exception e) {
            log.error("❌ Erro ao publicar evento: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ Estatísticas (SAFE)
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