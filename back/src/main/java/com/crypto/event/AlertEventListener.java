package com.crypto.event;

import com.crypto.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;



@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventListener {

    private final AlertService alertService;


    @Async
    @EventListener
    public void handleCryptoUpdate(CryptoUpdateEvent event) {
        try {
            log.debug("📬 Evento recebido: {} cryptos, tipo: {}",
                    event.getCryptoCurrencies().size(),
                    event.getType()
            );

            if (event.isGlobalUpdate()) {
                log.debug("🌍 Processando alertas globais");
                alertService.processAlerts(event.getCryptoCurrencies());

            } else {
                log.debug("👤 Processando alertas para: {}", event.getUserEmail());
                alertService.processAlertsForUser(
                        event.getCryptoCurrencies(),
                        event.getUserEmail()
                );
            }

        } catch (Exception e) {
            log.error("❌ Erro ao processar evento de atualização: {}", e.getMessage(), e);
        }
    }
}