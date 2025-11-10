// back/src/main/java/com/crypto/service/CryptoMonitoringService.java
package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.event.CryptoUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ✅ VERSÃO CORRIGIDA - SCHEDULER GLOBAL DESABILITADO
 *
 * MUDANÇAS:
 * - @Scheduled REMOVIDO (não dispara mais automaticamente)
 * - Alertas SÓ processam quando usuário EXPLICITAMENTE inicia monitoramento
 * - Scheduler user-specific no MonitoringControlService
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

    /**
     * ❌ SCHEDULER GLOBAL - **DESABILITADO**
     *
     * ⚠️ MOTIVO: Disparava alertas automaticamente para TODOS os usuários
     * mesmo sem ninguém estar logado/monitorando.
     *
     * ✅ SOLUÇÃO: Usar apenas schedulers USER-SPECIFIC no MonitoringControlService
     */
    // @Scheduled(fixedRate = 1800000, initialDelay = 60000) // ❌ DESABILITADO
    public void scheduledUpdate() {
        log.warn("⚠️ Scheduler Global: DESABILITADO");
        log.warn("   Use /api/monitoring/start para ativar monitoramento por usuário");
    }

    /**
     * ✅ ATUALIZAÇÃO GLOBAL (PROTEGIDA)
     *
     * Usa getCurrentPrices() que:
     * - Verifica cache primeiro (TTL 30min)
     * - Se cache expirado, enfileira request
     * - Se request falhar, usa banco
     */
    public void updateAndProcessAlerts() {
        try {
            log.info("🔄 Iniciando ciclo de monitoramento...");

            List<CryptoCurrency> currentCryptos = cryptoService.getCurrentPrices();

            if (currentCryptos.isEmpty()) {
                log.error("❌ NENHUM DADO DISPONÍVEL");
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
     * ✅ ATUALIZAÇÃO POR USUÁRIO (CHAMADO PELO SCHEDULER USER-SPECIFIC)
     */
    public void updateAndProcessAlertsForUser(String userEmail) {
        try {
            log.info("🔄 Processando alertas para: {}", userEmail);

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