package com.crypto.config;

import com.crypto.exception.RateLimitExceededException;
import com.crypto.security.JwtUtil;
import lombok.RequiredArgsConstructor;
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

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    /**
     * 🛡 Estrutura com contador + último uso
     * Evita memory leak e mantém controle preciso por sessão
     */
    private final Map<String, SessionRateLimit> sessionLimits = new ConcurrentHashMap<>();

    private static class SessionRateLimit {
        AtomicInteger counter = new AtomicInteger(0);
        long lastActivity = System.currentTimeMillis();
    }

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
                .setHandshakeHandler(new UserHandshakeHandler()) // 🔥 obrigatório p/ autenticação
                .withSockJS();

        log.info("✅ WebSocket endpoint registrado: /ws/crypto");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(64 * 1024)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(20_000)
                .setTimeToFirstMessage(30_000);

        log.info("⏳ WebSocket transport configurado com timeouts");
    }

    /**
     * 🔐 AUTENTICAÇÃO + RATE LIMIT por sessão
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {

                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                // =======================================================
                // 🔐 1. AUTENTICAÇÃO NA CONEXÃO (CONNECT FRAME)
                // =======================================================
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    String tokenHeader = accessor.getFirstNativeHeader("Authorization");

                    if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
                        log.warn("❌ WebSocket rejeitado: ausência de Authorization Bearer");
                        throw new IllegalArgumentException("Missing Authorization header");
                    }

                    String token = tokenHeader.substring(7);

                    try {
                        String username = jwtUtil.extractUsername(token);

                        if (!jwtUtil.validateToken(token)) {
                            throw new IllegalArgumentException("Invalid JWT");
                        }

                        // Define usuário autenticado na sessão WebSocket
                        accessor.setUser(new Principal() {
                            @Override
                            public String getName() {
                                return username;
                            }
                        });

                        log.info("🔐 WebSocket conectado: usuário {}", username);

                    } catch (Exception e) {
                        log.warn("❌ WebSocket rejeitado: JWT inválido ({})", e.getMessage());
                        throw new IllegalArgumentException("JWT validation failed");
                    }
                }

                // =======================================================
                // ⚡ 2. RATE LIMIT POR SESSÃO (SEND FRAME)
                // =======================================================
                if (StompCommand.SEND.equals(accessor.getCommand())) {

                    String sessionId = accessor.getSessionId();

                    SessionRateLimit rl = sessionLimits.computeIfAbsent(sessionId, s -> new SessionRateLimit());

                    int count = rl.counter.incrementAndGet();
                    rl.lastActivity = System.currentTimeMillis();

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
     * 🧹 LIMPEZA INTELIGENTE — evita memory leak
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanupOldSessions() {

        long now = System.currentTimeMillis();
        long inactivityLimit = 2 * 60_000; // 2 minutos

        sessionLimits.entrySet().removeIf(entry -> {
            boolean expired = now - entry.getValue().lastActivity > inactivityLimit;
            if (expired) {
                log.info("🗑 Removendo sessão WebSocket inativa: {}", entry.getKey());
            }
            return expired;
        });

        // Reset contador das sessões ainda ativas
        sessionLimits.values().forEach(v -> {
            v.counter.set(0);
        });

        log.debug("♻ Sessões ativas após limpeza: {}", sessionLimits.size());
    }
}
