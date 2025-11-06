package com.crypto.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ SPRINT 1: Configuração de Métricas Prometheus
 *
 * Métricas disponíveis:
 * - crypto_api_requests_total: Total de requisições
 * - crypto_rate_limit_hits_total: Rate limit atingido
 * - crypto_coingecko_request_duration_seconds: Latência CoinGecko
 * - crypto_alert_processing_duration_seconds: Tempo de processamento alertas
 * - crypto_websocket_connections_total: Conexões WebSocket
 * - crypto_websocket_messages_total: Mensagens WebSocket
 *
 * Endpoint: /actuator/prometheus
 */
@Slf4j
@Configuration
public class MetricsConfig {

    @Value("${spring.application.name:crypto-monitor}")
    private String applicationName;

    @Value("${app.version:2.0.1-sprint1}")
    private String version;

    @Value("${spring.profiles.active:dev}")
    private String environment;

    /**
     * ✅ Customizador para adicionar tags globais e filtrar métricas irrelevantes
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config()
                    .commonTags(
                            "application", applicationName,
                            "version", version,
                            "environment", environment
                    )
                    .meterFilter(MeterFilter.deny(id -> {
                        String name = id.getName();
                        return name.startsWith("jvm.") ||
                                name.startsWith("process.") ||
                                name.startsWith("system.") ||
                                name.startsWith("tomcat.") ||
                                name.startsWith("hikaricp.");
                    }));

            log.info("✅ Métricas Prometheus configuradas");
            log.info("   📊 Endpoint: /actuator/prometheus");
        };
    }

    // ✅ Timer: Requisições CoinGecko
    @Bean
    public Timer coinGeckoRequestTimer(MeterRegistry registry) {
        return Timer.builder("crypto_coingecko_request_duration_seconds")
                .description("Duração de requisições para CoinGecko API")
                .tag("api", "coingecko")
                .register(registry);
    }

    // ✅ Timer: Processamento de alertas
    @Bean
    public Timer alertProcessingTimer(MeterRegistry registry) {
        return Timer.builder("crypto_alert_processing_duration_seconds")
                .description("Tempo de processamento de alertas")
                .tag("type", "alert")
                .register(registry);
    }

    // ✅ Counter: Total de conexões WebSocket
    @Bean
    public Counter websocketConnectionsCounter(MeterRegistry registry) {
        return Counter.builder("crypto_websocket_connections_total")
                .description("Total de conexões WebSocket")
                .tag("type", "connection")
                .register(registry);
    }

    // ✅ Counter: Total de mensagens WebSocket
    @Bean
    public Counter websocketMessagesCounter(MeterRegistry registry) {
        return Counter.builder("crypto_websocket_messages_total")
                .description("Total de mensagens enviadas via WebSocket")
                .tag("type", "message")
                .register(registry);
    }

    // ✅ Counter: Total de vezes que o rate limit foi atingido
    @Bean
    public Counter rateLimitHitsCounter(MeterRegistry registry) {
        return Counter.builder("crypto_rate_limit_hits_total")
                .description("Total de vezes que o rate limit foi atingido")
                .tag("type", "ratelimit")
                .register(registry);
    }

    // ✅ Counter: Total de requisições à API (necessário para RateLimitFilter)
    @Bean
    public Counter apiRequestsCounter(MeterRegistry registry) {
        return Counter.builder("crypto_api_requests_total")
                .description("Total de requisições processadas pela API")
                .tag("type", "request")
                .register(registry);
    }
}
