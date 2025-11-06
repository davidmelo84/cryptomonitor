package com.crypto.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * ✅ SPRINT 2 - WEBSOCKET PARA REAL-TIME
 *
 * CORREÇÕES:
 * - Removido endpoint duplicado
 * - CORS simplificado e correto
 * - SockJS habilitado como fallback
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // ✅ Broker simples em memória
        config.enableSimpleBroker("/topic");

        // ✅ Prefixo para mensagens do cliente
        config.setApplicationDestinationPrefixes("/app");

        log.info("✅ WebSocket Message Broker configurado");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ✅ CORRIGIDO: Endpoint único com SockJS
        registry.addEndpoint("/ws/crypto")
                .setAllowedOriginPatterns(
                        "https://cryptomonitor-theta.vercel.app",
                        "https://*.vercel.app",
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "http://127.0.0.1:*"
                )
                .withSockJS();  // ✅ SockJS como fallback

        log.info("✅ WebSocket endpoint registrado: /ws/crypto");
        log.info("   📡 STOMP destination: /topic/prices");
        log.info("   🌐 CORS: Vercel + localhost");
    }
}