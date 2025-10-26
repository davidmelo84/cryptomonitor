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
    private final ApplicationEventPublisher eventPublisher; // ✅ NOVO

    /**
     * ✅ MANTIDO - Compatibilidade com código existente
     * Atualização e processamento de alertas para todos os usuários
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

            // 3. ✅ NOVO: Publicar evento ao invés de chamar AlertService diretamente
            publishCryptoUpdateEvent(currentCryptos, null, CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE);

            log.info("✅ Ciclo de monitoramento concluído com sucesso");

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ MANTIDO - Atualização para usuário específico
     */
    public void updateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento para email: {}", userEmail);

            // 1. Buscar preços atuais
            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();
            log.info("📊 Obtidos preços de {} criptomoedas", currentCryptos.size());

            // 2. Salvar os dados atualizados
            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            // 3. ✅ NOVO: Publicar evento para usuário específico
            publishCryptoUpdateEvent(currentCryptos, userEmail, CryptoUpdateEvent.UpdateType.SCHEDULED_UPDATE);

            log.info("✅ Ciclo de monitoramento concluído para: {}", userEmail);

        } catch (Exception e) {
            log.error("❌ Erro no ciclo de monitoramento para {}: {}", userEmail, e.getMessage(), e);
        }
    }

    /**
     * ✅ MANTIDO - Atualização manual (compatibilidade com endpoint /api/crypto/update)
     */
    public void forceUpdateAndProcessAlerts() {
        try {
            log.info("🚀 Forçando atualização manual...");

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            // ✅ NOVO: Publicar evento de atualização manual
            publishCryptoUpdateEvent(currentCryptos, null, CryptoUpdateEvent.UpdateType.MANUAL_UPDATE);

            log.info("✅ Atualização manual concluída. {} moedas processadas", currentCryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro na atualização manual: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na atualização manual", e);
        }
    }

    /**
     * ✅ MANTIDO - Atualização manual para usuário específico
     */
    public void forceUpdateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🚀 Forçando atualização manual para: {}", userEmail);

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            for (CryptoCurrency crypto : currentCryptos) {
                cryptoService.saveCrypto(crypto);
            }

            // ✅ NOVO: Publicar evento para usuário específico
            publishCryptoUpdateEvent(currentCryptos, userEmail, CryptoUpdateEvent.UpdateType.MANUAL_UPDATE);

            log.info("✅ Atualização manual concluída para {}. {} moedas processadas",
                    userEmail, currentCryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro na atualização manual para {}: {}", userEmail, e.getMessage(), e);
            throw new RuntimeException("Falha na atualização manual", e);
        }
    }

    /**
     * ✅ MANTIDO - Processa alertas para uma crypto específica
     */
    public void processAlertsForCrypto(String coinId) {
        try {
            cryptoService.getCryptoByCoinId(coinId)
                    .ifPresentOrElse(
                            crypto -> {
                                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);

                                // ✅ NOVO: Publicar evento para crypto específica
                                publishCryptoUpdateEvent(
                                        List.of(savedCrypto),
                                        null,
                                        CryptoUpdateEvent.UpdateType.SINGLE_CRYPTO
                                );

                                log.info("✅ Alertas processados para {}", coinId);
                            },
                            () -> log.warn("⚠️ Criptomoeda {} não encontrada", coinId)
                    );
        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {}: {}", coinId, e.getMessage());
        }
    }

    /**
     * ✅ MANTIDO - Processa alertas para crypto e usuário específicos
     */
    public void processAlertsForCryptoAndUser(String coinId, String userEmail) {
        try {
            log.info("🔍 Processando alertas de {} para {}", coinId, userEmail);

            cryptoService.getCryptoByCoinId(coinId)
                    .ifPresentOrElse(
                            crypto -> {
                                CryptoCurrency savedCrypto = cryptoService.saveCrypto(crypto);

                                // ✅ NOVO: Publicar evento para usuário específico
                                publishCryptoUpdateEvent(
                                        List.of(savedCrypto),
                                        userEmail,
                                        CryptoUpdateEvent.UpdateType.SINGLE_CRYPTO
                                );

                                log.info("✅ Alertas processados para {} (usuário: {})", coinId, userEmail);
                            },
                            () -> log.warn("⚠️ Criptomoeda {} não encontrada", coinId)
                    );
        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas para {} (usuário: {}): {}",
                    coinId, userEmail, e.getMessage());
        }
    }

    /**
     * ✅ MANTIDO - Estatísticas do monitoramento
     */
    public MonitoringStats getMonitoringStats() {
        try {
            List<CryptoCurrency> savedCryptos = cryptoService.getAllSavedCryptos();

            return MonitoringStats.builder()
                    .totalCryptocurrencies(savedCryptos.size())
                    .totalActiveAlerts(0) // AlertService não é mais dependência direta
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

    /**
     * ✅ NOVO - Método auxiliar para publicar eventos
     */
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

    /**
     * ✅ MANTIDO - Classe interna para estatísticas
     */
    @lombok.Builder
    @lombok.Data
    public static class MonitoringStats {
        private int totalCryptocurrencies;
        private long totalActiveAlerts;
        private java.time.LocalDateTime lastUpdate;
    }
}