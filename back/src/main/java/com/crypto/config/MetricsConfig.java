package com.crypto.config;

import com.crypto.service.CoinGeckoRequestQueue;
import com.crypto.service.MonitoringControlService;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.util.Map;

/**
 * ✅ SPRINT 1 & 2: Configuração de Métricas Prometheus
 *
 * CORREÇÕES:
 * - Removido parâmetro MeterRegistry do customizer (evita dependência circular)
 * - Beans de Counter/Timer injetados via @Lazy no RateLimitFilter
 */
@Slf4j
@Configuration
public class MetricsConfig {

    @Value("${spring.application.name:crypto-monitor}")
    private String applicationName;

    @Value("${app.version:2.0.1-sprint2}")
    private String version;

    @Value("${spring.profiles.active:dev}")
    private String environment;

    /**
     * ✅ CORRIGIDO - Sem parâmetros para evitar dependência circular
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

    // ✅ Timers
    @Bean
    public Timer coinGeckoRequestTimer(MeterRegistry registry) {
        return Timer.builder("crypto_coingecko_request_duration_seconds")
                .description("Duração de requisições para CoinGecko API")
                .tag("api", "coingecko")
                .register(registry);
    }

    @Bean
    public Timer alertProcessingTimer(MeterRegistry registry) {
        return Timer.builder("crypto_alert_processing_duration_seconds")
                .description("Tempo de processamento de alertas")
                .tag("type", "alert")
                .register(registry);
    }

    // ✅ Counters
    @Bean
    public Counter websocketConnectionsCounter(MeterRegistry registry) {
        return Counter.builder("crypto_websocket_connections_total")
                .description("Total de conexões WebSocket")
                .tag("type", "connection")
                .register(registry);
    }

    @Bean
    public Counter websocketMessagesCounter(MeterRegistry registry) {
        return Counter.builder("crypto_websocket_messages_total")
                .description("Total de mensagens enviadas via WebSocket")
                .tag("type", "message")
                .register(registry);
    }

    @Bean
    public Counter rateLimitHitsCounter(MeterRegistry registry) {
        return Counter.builder("crypto_rate_limit_hits_total")
                .description("Total de vezes que o rate limit foi atingido")
                .tag("type", "ratelimit")
                .register(registry);
    }

    @Bean
    public Counter apiRequestsCounter(MeterRegistry registry) {
        return Counter.builder("crypto_api_requests_total")
                .description("Total de requisições processadas pela API")
                .tag("type", "request")
                .register(registry);
    }

    // Adicionar ao MetricsConfig.java existente

    @Bean
    public Gauge cacheHitRateGauge(MeterRegistry registry, CacheManager cacheManager) {
        return Gauge.builder("crypto_cache_hit_rate", () -> {
                    Cache cache = cacheManager.getCache("allCryptoPrices");
                    if (cache instanceof CaffeineCache) {
                        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                                ((CaffeineCache) cache).getNativeCache();
                        CacheStats stats = nativeCache.stats();

                        long hits = stats.hitCount();
                        long misses = stats.missCount();
                        long total = hits + misses;

                        return total > 0 ? (double) hits / total : 0.0;
                    }
                    return 0.0;
                })
                .description("Taxa de acertos do cache principal")
                .tag("cache", "allCryptoPrices")
                .register(registry);
    }

    @Bean
    public Gauge activeMonitoringUsersGauge(MeterRegistry registry,
                                            MonitoringControlService monitoringService) {
        return Gauge.builder("crypto_active_monitoring_users", () -> {
                    Map<String, Object> status = monitoringService.getGlobalStatus();
                    return ((Number) status.getOrDefault("totalActiveMonitors", 0)).doubleValue();
                })
                .description("Número de usuários com monitoramento ativo")
                .register(registry);
    }

    @Bean
    public Gauge rateLimitQueueSizeGauge(MeterRegistry registry,
                                         CoinGeckoRequestQueue requestQueue) {
        return Gauge.builder("crypto_rate_limit_queue_size", () -> {
                    CoinGeckoRequestQueue.QueueStats stats = requestQueue.getStats();
                    return stats.queueSize();
                })
                .description("Tamanho da fila de requisições para CoinGecko")
                .register(registry);
    }

    // Métrica de latência por endpoint
    @Bean
    public MeterFilter addEndpointTag() {
        return MeterFilter.commonTags(
                Tags.of(
                        "application", "crypto-monitor",
                        "instance", getHostname()
                )
        );
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}