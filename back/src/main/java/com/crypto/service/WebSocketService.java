package com.crypto.service;

import com.crypto.model.CryptoCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;


    public void broadcastPrices(List<CryptoCurrency> cryptos) {
        try {
            messagingTemplate.convertAndSend("/topic/prices", cryptos);

            log.debug("📡 Broadcast: {} cryptos enviadas via WebSocket", cryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro ao fazer broadcast via WebSocket: {}", e.getMessage());
        }
    }


    public void sendCryptoUpdate(CryptoCurrency crypto) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/crypto/" + crypto.getCoinId(),
                    crypto
            );

            log.debug("📡 Update enviado: {}", crypto.getSymbol());

        } catch (Exception e) {
            log.error("❌ Erro ao enviar update: {}", e.getMessage());
        }
    }


    public void broadcastSystemStatus(Map<String, Object> status) {
        try {
            messagingTemplate.convertAndSend("/topic/system/status", status);

        } catch (Exception e) {
            log.error("❌ Erro ao enviar status: {}", e.getMessage());
        }
    }
}