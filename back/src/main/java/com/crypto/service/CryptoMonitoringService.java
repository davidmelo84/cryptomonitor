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


@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoMonitoringService {

    private final CryptoService cryptoService;
    private final ApplicationEventPublisher eventPublisher;
    private final WebSocketService webSocketService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private LocalDateTime lastSuccessfulRun = null;


    public void updateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🔄 Processando alertas para: {}", userEmail);

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
