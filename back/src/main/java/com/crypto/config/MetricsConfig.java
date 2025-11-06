// back/src/main/java/com/crypto/config/MetricsConfig.java
package com.crypto.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ SPRINT 1 - MÉTRICAS COM PROMETHEUS
 *
 * Configuração de métricas customizadas para monitoramento.
 *
 * Métricas disponíveis:
 * - crypto_api_requests_total - Total de requisições
 * - crypto_api_errors_total - Total de erros
 * - crypto_rate_limit_hits_total - Rate limit atingido
 * - crypto_email_sent_total - Emails enviados
 * - crypto_alert_triggered_total - Alertas disparados
 * - crypto_bot_trades_total - Trades dos bots
 *
 * Acesso: http://localhost:8080/crypto-monitor/actuator/prometheus
 */
@Slf4j
@Configuration
public class MetricsConfig {

    /**
     * ✅ Counter: Requisições API
     */
    @Bean
    public Counter apiRequestsCounter(MeterRegistry registry) {
        return Counter.builder("crypto_api_requests_total")
                .description("Total de requisições à API")
                .tag("application", "crypto-monitor")
                .register(registry);
    }

    /**
     * ✅ Counter: Erros da API
     */
    @Bean
    public Counter apiErrorsCounter(MeterRegistry registry) {
        return Counter.builder("crypto_api_errors_total")
                .description("Total de erros na API")
                .tag("application", "crypto-monitor")
                .register(registry);
    }

    /**
     * ✅ Counter: Rate limit hits
     */
    @Bean
    public Counter rateLimitHitsCounter(MeterRegistry registry) {
        return Counter.builder("crypto_rate_limit_hits_total")
                .description("Total de vezes que rate limit foi atingido")
                .tag("application", "crypto-monitor")
                .register(registry);
    }

    /**
     * ✅ Counter: Emails enviados
     */
    @Bean
    public Counter emailsSentCounter(MeterRegistry registry) {
        return Counter.builder("crypto_email_sent_total")
                .description("Total de emails enviados")
                .tag("application", "crypto-monitor")
                .register(registry);
    }

    /**
     * ✅ Counter: Alertas disparados
     */
    @Bean
    public Counter alertsTriggeredCounter(MeterRegistry registry) {
        return Counter.builder("crypto_alert_triggered_total")
                .description("Total de alertas disparados")
                .tag("application", "crypto-monitor")
                .register(registry);
    }

    /**
     * ✅ Counter: Trades dos bots
     */
    @Bean
    public Counter botTradesCounter(MeterRegistry registry) {
        return Counter.builder("crypto_bot_trades_total")
                .description("Total de trades executados pelos bots")
                .tag("application", "crypto-monitor")
                .register(registry);
    }

    /**
     * ✅ Timer: Latência de requisições CoinGecko
     */
    @Bean
    public Timer coinGeckoRequestTimer(MeterRegistry registry) {
        return Timer.builder("crypto_coingecko_request_duration_seconds")
                .description("Duração de requisições à CoinGecko API")
                .tag("application", "crypto-monitor")
                .register(registry);
    }

    /**
     * ✅ Timer: Latência de processamento de alertas
     */
    @Bean
    public Timer alertProcessingTimer(MeterRegistry registry) {
        return Timer.builder("crypto_alert_processing_duration_seconds")
                .description("Duração do processamento de alertas")
                .tag("application", "crypto-monitor")
                .register(registry);
    }

    /**
     * ✅ Configuração adicional do MeterRegistry
     */
    @Bean
    public MeterRegistry configureMeterRegistry(MeterRegistry registry) {
        // Tags globais
        registry.config().commonTags(
                "application", "crypto-monitor",
                "version", "2.0.1-sprint1"
        );

        log.info("✅ Métricas Prometheus configuradas");
        log.info("   📊 Endpoint: /actuator/prometheus");

        return registry;
    }
}