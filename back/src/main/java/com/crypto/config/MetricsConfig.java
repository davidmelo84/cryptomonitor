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
 * - crypto_api_errors_total: Total de erros
 * - crypto_rate_limit_hits_total: Rate limit atingido
 * - crypto_email_sent_total: Emails enviados
 * - crypto_alert_triggered_total: Alertas disparados
 * - crypto_bot_trades_total: Trades dos bots
 * - crypto_coingecko_request_duration_seconds: Latência CoinGecko
 * - crypto_alert_processing_duration_seconds: Tempo de processamento alertas
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
     * ✅ Customizador para adicionar tags globais sem causar dependências
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
                    // ✅ Filtrar métricas desnecessárias
                    .meterFilter(MeterFilter.deny(id -> {
                        String name = id.getName();
                        return name.startsWith("jvm.") ||
                                name.startsWith("process.") ||
                                name.startsWith("system.") ||
                                name.startsWith("tomcat.") ||
                                name.startsWith("hikaricp.");
                    }));

            log.info("✅ Métricas Prometheus configuradas");
            log.info("   Endpoint: /actuator/prometheus");
        };
    }

    /**
     * ✅ Timer para requisições CoinGecko API
     * Uso: coinGeckoRequestTimer.record(() -> { ... });
     */
    @Bean
    public Timer coinGeckoRequestTimer(MeterRegistry registry) {
        return Timer.builder("crypto.coingecko.request.duration")
                .description("Duração de requisições para CoinGecko API")
                .tag("api", "coingecko")
                .register(registry);
    }

    /**
     * ✅ Timer para processamento de alertas
     * Uso: alertProcessingTimer.record(() -> { ... });
     */
    @Bean
    public Timer alertProcessingTimer(MeterRegistry registry) {
        return Timer.builder("crypto.alert.processing.duration")
                .description("Tempo de processamento de alertas")
                .tag("type", "alert")
                .register(registry);
    }

    /**
     * ✅ SPRINT 2: Métricas de WebSocket
     */
    @Bean
    public Counter websocketConnectionsCounter(MeterRegistry registry) {
        return Counter.builder("crypto.websocket.connections")
                .description("Total de conexões WebSocket")
                .tag("type", "connection")
                .register(registry);
    }

    @Bean
    public Counter websocketMessagesCounter(MeterRegistry registry) {
        return Counter.builder("crypto.websocket.messages")
                .description("Mensagens enviadas via WebSocket")
                .tag("type", "message")
                .register(registry);
    }
}