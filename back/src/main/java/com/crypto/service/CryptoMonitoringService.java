package com.crypto.service;

import com.crypto.model.CryptoCurrency;
import com.crypto.event.CryptoUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ✅ VERSÃO OTIMIZADA - SCHEDULER GLOBAL REMOVIDO
 *
 * MUDANÇAS:
 * - Scheduler automático foi completamente removido
 * - SmartCacheService controla updates a cada hora
 * - Monitoramento ocorre apenas quando um usuário ativa
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoMonitoringService {

    private final CryptoService cryptoService;
    private final ApplicationEventPublisher eventPublisher;
    private final WebSocketService webSocketService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private LocalDateTime lastSuccessfulRun = null;

    /**
     * ✅ ATUALIZAÇÃO MANUAL (para usuário específico)
     *
     * Usado pelo MonitoringControlService quando usuário ativa monitoramento
     */
    public void updateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🔄 Processando alertas para: {}", userEmail);

            // Buscar preços (cacheados pelo SmartCache)
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            if (currentCryptos.isEmpty()) {
                log.error("❌ Sem dados para: {}", userEmail);
                return;
            }

            publishCryptoUpdateEvent(
                    currentCryptos,
                    userEmail,
                    CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE
            );

            log.info("✅ Alertas processados para: {}", userEmail);

        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {}: {}", userEmail, e.getMessage());
        }
    }

    /**
     * 📡 Envia preços via WebSocket
     */
    public void broadcastPrices() {
        try {
            List<CryptoCurrency> cryptos = cryptoService.getCurrentPrices();

            if (!cryptos.isEmpty()) {
                webSocketService.broadcastPrices(cryptos);
                log.debug("📡 Broadcast: {} moedas", cryptos.size());
            }

        } catch (Exception e) {
            log.error("❌ Erro no broadcast: {}", e.getMessage());
        }
    }

    /**
     * 📤 Publica evento para processamento de alertas
     */
    private void publishCryptoUpdateEvent(
            List<CryptoCurrency> cryptos,
            String userEmail,
            CryptoUpdateEvent.UpdateType type) {

        try {
            CryptoUpdateEvent event = (userEmail == null)
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
     * 📊 Estatísticas do monitoramento
     */
    public MonitoringStats getMonitoringStats() {
        try {
            List<CryptoCurrency> savedCryptos = cryptoService.getAllSavedCryptos();

            return MonitoringStats.builder()
                    .totalCryptocurrencies(savedCryptos.size())
                    .isSchedulerRunning(false)  // Agora sempre false
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
