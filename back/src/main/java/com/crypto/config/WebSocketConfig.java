package com.crypto.config;

import com.crypto.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.config.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** 🔐 Rate-limit por sessão WebSocket */
    private final Map<String, AtomicInteger> messageCount = new ConcurrentHashMap<>();


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");

        log.info("✅ WebSocket Message Broker configurado");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws/crypto")
                .setAllowedOriginPatterns(
                        "https://cryptomonitor-theta.vercel.app",
                        "https://*.vercel.app",
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "http://127.0.0.1:*"
                )
                .withSockJS();

        log.info("✅ WebSocket endpoint registrado: /ws/crypto");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(64 * 1024)          // 64 KB por mensagem
                .setSendBufferSizeLimit(512 * 1024)      // 512 KB de buffer
                .setSendTimeLimit(20 * 1000)             // 20 segundos para enviar dados
                .setTimeToFirstMessage(30 * 1000);       // 30 segundos para enviar primeiro heartbeat
        log.info("⏳ WebSocket transport configurado com timeouts");
    }

    /**
     * 🔐 RATE-LIMIT contra flood de WebSocket
     * Limite: 100 mensagens por minuto por sessão
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.SEND.equals(accessor.getCommand())) {

                    String sessionId = accessor.getSessionId();
                    int count = messageCount
                            .computeIfAbsent(sessionId, k -> new AtomicInteger(0))
                            .incrementAndGet();

                    if (count > 100) {
                        log.warn("⚠️ Rate limit WebSocket atingido - Sessão {}", sessionId);
                        throw new RateLimitExceededException("WebSocket rate limit exceeded");
                    }
                }

                return message;
            }
        });
    }

    /**
     * 🔁 Limpa contador a cada minuto
     */
    @Scheduled(fixedDelay = 60000)
    public void resetWebSocketRateLimits() {
        if (!messageCount.isEmpty()) {
            log.debug("🧹 Reset WebSocket rate limits ({} sessões)", messageCount.size());
            messageCount.clear();
        }
    }
}
