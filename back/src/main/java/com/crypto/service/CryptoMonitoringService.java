// back/src/main/java/com/crypto/service/CryptoMonitoringService.java
package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.event.CryptoUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ✅ MONITORING SERVICE - COM SCHEDULER OTIMIZADO
 *
 * ESTRATÉGIA:
 * - Scheduler executa A CADA 30 MINUTOS (reduz requests)
 * - Cache mantém dados por 30 minutos
 * - WebSocket broadcast para frontend (tempo real)
 * - Total: ~2 requests/hora ao CoinGecko
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoMonitoringService {

    private final CryptoService cryptoService;
    private final ApplicationEventPublisher eventPublisher;
    private final WebSocketService webSocketService;

    /**
     * ✅ SCHEDULER - Executa A CADA 30 MINUTOS
     *
     * Antes: A cada 5min = 288 requests/dia
     * Agora: A cada 30min = 48 requests/dia
     * Redução: 83%
     */
    @Scheduled(fixedDelay = 1800000) // 30 minutos = 1800000ms
    public void scheduledUpdate() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("⏰ SCHEDULER: Iniciando atualização periódica");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        updateAndProcessAlerts();
    }

    /**
     * ✅ Atualização global (com broadcast WebSocket)
     */
    public void updateAndProcessAlerts() {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento...");

            // 1. Buscar preços (cache 30min)
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
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
        }
    }

    /**
     * ✅ Atualização para usuário específico
     */
    public void updateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🔄 Iniciando ciclo para: {}", userEmail);

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
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
     * ✅ Forçar atualização manual
     */
    public void forceUpdateAndProcessAlerts() {
        try {
            log.info("🚀 Forçando atualização manual...");

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

            log.info("✅ Atualização manual concluída: {} moedas", currentCryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro na atualização manual: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na atualização manual", e);
        }
    }

    /**
     * ✅ Atualização de uma crypto específica
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

                                webSocketService.broadcastPrices(List.of(savedCrypto));
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
     * ✅ Estatísticas
     */
    public MonitoringStats getMonitoringStats() {
        try {
            List<CryptoCurrency> savedCryptos = cryptoService.getAllSavedCryptos();

            return MonitoringStats.builder()
                    .totalCryptocurrencies(savedCryptos.size())
                    .totalActiveAlerts(0)
                    .lastUpdate(savedCryptos.isEmpty() ? null :
                            savedCryptos.get(0).getLastUpdated())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas: {}", e.getMessage());
            return MonitoringStats.builder()
                    .totalCryptocurrencies(0)
                    .totalActiveAlerts(0)
                    .build();
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class MonitoringStats {
        private int totalCryptocurrencies;
        private long totalActiveAlerts;
        private java.time.LocalDateTime lastUpdate;
    }
}