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
 * ✅ VERSÃO OTIMIZADA - SCHEDULER GLOBAL DESABILITADO
 *
 * MUDANÇAS:
 * - Scheduler automático REMOVIDO (causava rate limit)
 * - Apenas SmartCacheService faz updates (1x/hora)
 * - Monitoramento só acontece quando usuário ativa
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
     * ❌ SCHEDULER GLOBAL - PERMANENTEMENTE DESABILITADO
     *
     * @deprecated Removido para evitar rate limit. Será excluído na v3.0.0.
     * Use SmartCacheService.scheduledUpdate() para atualizações automáticas.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void scheduledUpdate() {
        throw new UnsupportedOperationException(
                "Scheduler global desabilitado. Use SmartCacheService.scheduledUpdate()."
        );
    }

    /**
     * ✅ ATUALIZAÇÃO MANUAL (para usuário específico)
     *
     * Usado pelo MonitoringControlService quando usuário ativa monitoramento
     */
    public void updateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🔄 Processando alertas para: {}", userEmail);

            // Buscar preços (já cacheados pelo SmartCache)
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            if (currentCryptos.isEmpty()) {
                log.error("❌ Sem dados para: {}", userEmail);
                return;
            }

            // Publicar evento (processamento de alertas)
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
     * ✅ BROADCAST VIA WEBSOCKET (manual)
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
                    .isSchedulerRunning(false)  // Sempre false agora
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
