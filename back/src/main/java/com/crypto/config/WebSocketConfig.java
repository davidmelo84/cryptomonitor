package com.crypto.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * ✅ SPRINT 2 - WEBSOCKET PARA REAL-TIME
 *
 * Permite push automático de preços para o frontend
 * sem necessidade de polling.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // ✅ Broker simples em memória (para começar)
        config.enableSimpleBroker("/topic");

        // ✅ Prefixo para mensagens do cliente
        config.setApplicationDestinationPrefixes("/app");

        log.info("✅ WebSocket Message Broker configurado");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ✅ Endpoint de conexão WebSocket
        registry.addEndpoint("/ws/crypto")
                .setAllowedOriginPatterns("*") // CORS
                .withSockJS(); // Fallback para navegadores antigos

        log.info("✅ WebSocket endpoint registrado: /ws/crypto");
    }
}