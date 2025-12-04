package com.crypto.service;

import com.crypto.model.CryptoCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 🔥 Broadcast otimizado para reduzir o payload WebSocket
     */
    public void broadcastPrices(List<CryptoCurrency> cryptos) {
        try {
            // Enviar apenas os campos realmente necessários ao dashboard
            List<Map<String, Object>> lightweightData = cryptos.stream()
                    .map(crypto -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("coinId", crypto.getCoinId());
                        m.put("symbol", crypto.getSymbol());
                        m.put("price", crypto.getCurrentPrice());
                        m.put("change24h", crypto.getPriceChange24h() != null
                                ? crypto.getPriceChange24h()
                                : 0.0);
                        return m;
                    })
                    .toList();

            messagingTemplate.convertAndSend("/topic/prices", lightweightData);

            log.debug("📡 Broadcast otimizado: {} cryptos enviadas", cryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro ao fazer broadcast: {}", e.getMessage());
        }
    }

    /**
     * Envia atualização individual de uma crypto (mantido sem alterações)
     */
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

    /**
     * Envia status do sistema (mantido sem alterações)
     */
    public void broadcastSystemStatus(Map<String, Object> status) {
        try {
            messagingTemplate.convertAndSend("/topic/system/status", status);

        } catch (Exception e) {
            log.error("❌ Erro ao enviar status: {}", e.getMessage());
        }
    }
}
