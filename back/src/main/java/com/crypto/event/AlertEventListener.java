// back/src/main/java/com/crypto/event/AlertEventListener.java
package com.crypto.event;

import com.crypto.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener que reage a eventos de atualização de criptomoedas
 * Processa alertas de forma assíncrona e desacoplada
 *
 * ✅ RESOLVE DEPENDÊNCIA CIRCULAR:
 * - Antes: CryptoMonitoringService → AlertService → CryptoService → CryptoMonitoringService
 * - Agora: CryptoMonitoringService → Event → AlertEventListener → AlertService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventListener {

    private final AlertService alertService;

    /**
     * Processa alertas quando cryptos são atualizadas
     * Executado de forma ASSÍNCRONA para não bloquear o publisher
     */
    @Async
    @EventListener
    public void handleCryptoUpdate(CryptoUpdateEvent event) {
        try {
            log.debug("📬 Evento recebido: {} cryptos, tipo: {}",
                    event.getCryptoCurrencies().size(),
                    event.getType()
            );

            // Processar alertas baseado no tipo de atualização
            if (event.isGlobalUpdate()) {
                // Atualização global - processar para todos usuários
                log.debug("🌍 Processando alertas globais");
                alertService.processAlerts(event.getCryptoCurrencies());

            } else {
                // Atualização de usuário específico
                log.debug("👤 Processando alertas para: {}", event.getUserEmail());
                alertService.processAlertsForUser(
                        event.getCryptoCurrencies(),
                        event.getUserEmail()
                );
            }

        } catch (Exception e) {
            log.error("❌ Erro ao processar evento de atualização: {}", e.getMessage(), e);
            // Não propagar exceção - não queremos falhar o publisher
        }
    }
}