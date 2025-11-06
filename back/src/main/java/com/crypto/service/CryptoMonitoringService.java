// back/src/main/java/com/crypto/service/CryptoMonitoringService.java
package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.event.CryptoUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ✅ REFATORADO - SEM DEPENDÊNCIA CIRCULAR
 *
 * ANTES:
 * CryptoMonitoringService → AlertService → CryptoService → [circular]
 *
 * AGORA:
 * CryptoMonitoringService → CryptoService
 *                         → Event Publisher
 *                              ↓
 *                         AlertEventListener → AlertService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoMonitoringService {

    private final CryptoService cryptoService;
    private final ApplicationEventPublisher eventPublisher;
    private final WebSocketService webSocketService; // ✅ ADICIONADO

    /**
     * ✅ Atualização e processamento de alertas para todos os usuários
     * Agora com broadcast via WebSocket
     */
    public void updateAndProcessAlerts() {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento...");

            // 1. Buscar preços atuais
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
            log.info("📊 Obtidos preços de {} criptomoedas", currentCryptos.size());

            // 2. Salvar os dados atualizados
            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            // 3. Publicar evento
            publishCryptoUpdateEvent(currentCryptos, null, CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE);

            // ✅ 4. NOVO - Broadcast via WebSocket
            webSocketService.broadcastPrices(currentCryptos);

            log.info("✅ Ciclo de monitoramento concluído com sucesso");

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento: {}", e.getMessage(), e);
        }
    }

    public void updateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento para email: {}", userEmail);

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
            log.info("📊 Obtidos preços de {} criptomoedas", currentCryptos.size());

            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            publishCryptoUpdateEvent(currentCryptos, userEmail, CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE);
            log.info("✅ Ciclo de monitoramento concluído para: {}", userEmail);

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento para {}: {}", userEmail, e.getMessage(), e);
        }
    }

    public void forceUpdateAndProcessAlerts() {
        try {
            log.info("🚀 Forçando atualização manual...");

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            publishCryptoUpdateEvent(currentCryptos, null, CryptoUpdateEvent.UpdateType.MANUAL_UPDATE);
            webSocketService.broadcastPrices(currentCryptos); // ✅ Broadcast também no modo manual

            log.info("✅ Atualização manual concluída. {} moedas processadas", currentCryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro na atualização manual: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na atualização manual", e);
        }
    }

    public void forceUpdateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🚀 Forçando atualização manual para: {}", userEmail);

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            publishCryptoUpdateEvent(currentCryptos, userEmail, CryptoUpdateEvent.UpdateType.MANUAL_UPDATE);
            webSocketService.broadcastPrices(currentCryptos); // ✅ Broadcast também por usuário

            log.info("✅ Atualização manual concluída para {}. {} moedas processadas",
                    userEmail, currentCryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro na atualização manual para {}: {}", userEmail, e.getMessage(), e);
            throw new RuntimeException("Falha na atualização manual", e);
        }
    }

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

                                webSocketService.broadcastPrices(List.of(savedCrypto)); // ✅ Broadcast unitário
                                log.info("✅ Alertas processados para {}", coinId);
                            },
                            () -> log.warn("⚠️ Criptomoeda {} não encontrada", coinId)
                    );
        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {}: {}", coinId, e.getMessage());
        }
    }

    public void processAlertsForCryptoAndUser(String coinId, String userEmail) {
        try {
            log.info("🔍 Processando alertas de {} para {}", coinId, userEmail);

            cryptoService.getCryptoByCoinId(coinId)
                    .ifPresentOrElse(
                            crypto -> {
                                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);

                                publishCryptoUpdateEvent(
                                        List.of(savedCrypto),
                                        userEmail,
                                        CryptoUpdateEvent.UpdateType.SINGLE_CRYPTO
                                );

                                webSocketService.broadcastPrices(List.of(savedCrypto)); // ✅ Broadcast unitário
                                log.info("✅ Alertas processados para {} (usuário: {})", coinId, userEmail);
                            },
                            () -> log.warn("⚠️ Criptomoeda {} não encontrada", coinId)
                    );
        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {} (usuário: {}): {}",
                    coinId, userEmail, e.getMessage());
        }
    }

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

    private void publishCryptoUpdateEvent(List<CryptoCurrency> cryptos, String userEmail, CryptoUpdateEvent.UpdateType type) {
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

    @lombok.Builder
    @lombok.Data
    public static class MonitoringStats {
        private int totalCryptocurrencies;
        private long totalActiveAlerts;
        private java.time.LocalDateTime lastUpdate;
    }
}
