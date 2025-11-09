// back/src/main/java/com/crypto/service/RateLimitMetricsService.java
package com.crypto.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ✅ SERVIÇO DE MÉTRICAS DE RATE LIMITING
 *
 * Rastreia:
 * - Hits de rate limit do CoinGecko (429)
 * - Requests bem-sucedidos
 * - Requests falhados
 * - Taxa de sucesso
 *
 * Integra com Prometheus se disponível
 */
@Slf4j
@Service
public class RateLimitMetricsService {

    // ✅ Contadores Prometheus (se disponível)
    private final Counter coinGeckoRateLimitHits;
    private final Counter coinGeckoSuccessRequests;
    private final Counter coinGeckoFailedRequests;

    // ✅ Contadores em memória (sempre disponível)
    private final AtomicInteger rateLimitHitsCount = new AtomicInteger(0);
    private final AtomicInteger successRequestsCount = new AtomicInteger(0);
    private final AtomicInteger failedRequestsCount = new AtomicInteger(0);
    private final Map<LocalDateTime, Integer> requestsPerMinute = new ConcurrentHashMap<>();

    private LocalDateTime lastRateLimitHit = null;

    public RateLimitMetricsService(MeterRegistry registry) {
        // ✅ Registrar counters no Prometheus
        this.coinGeckoRateLimitHits = Counter.builder("coingecko_ratelimit_hits_total")
                .description("Total de vezes que rate limit 429 foi recebido")
                .tag("provider", "coingecko")
                .register(registry);

        this.coinGeckoSuccessRequests = Counter.builder("coingecko_requests_success_total")
                .description("Total de requests bem-sucedidos ao CoinGecko")
                .tag("provider", "coingecko")
                .register(registry);

        this.coinGeckoFailedRequests = Counter.builder("coingecko_requests_failed_total")
                .description("Total de requests falhados ao CoinGecko")
                .tag("provider", "coingecko")
                .register(registry);

        log.info("✅ RateLimitMetricsService inicializado");
    }

    /**
     * ✅ Registrar hit de rate limit (429)
     */
    public void recordRateLimitHit() {
        rateLimitHitsCount.incrementAndGet();
        coinGeckoRateLimitHits.increment();
        lastRateLimitHit = LocalDateTime.now();

        log.warn("⚠️ Rate Limit 429 registrado (total: {})", rateLimitHitsCount.get());
    }

    /**
     * ✅ Registrar request bem-sucedido
     */
    public void recordSuccess() {
        successRequestsCount.incrementAndGet();
        coinGeckoSuccessRequests.increment();

        // Registrar no contador por minuto
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        requestsPerMinute.merge(now, 1, Integer::sum);

        log.debug("✅ Request bem-sucedido (total: {})", successRequestsCount.get());
    }

    /**
     * ✅ Registrar request falhado (não 429)
     */
    public void recordFailure() {
        failedRequestsCount.incrementAndGet();
        coinGeckoFailedRequests.increment();

        log.warn("❌ Request falhado (total: {})", failedRequestsCount.get());
    }

    /**
     * ✅ Obter estatísticas completas
     */
    public Map<String, Object> getStatistics() {
        cleanOldRequestCounts();

        int totalRequests = successRequestsCount.get() + failedRequestsCount.get();
        double successRate = totalRequests > 0
                ? (successRequestsCount.get() * 100.0) / totalRequests
                : 0;

        return Map.of(
                "rateLimitHits", rateLimitHitsCount.get(),
                "successRequests", successRequestsCount.get(),
                "failedRequests", failedRequestsCount.get(),
                "totalRequests", totalRequests,
                "successRate", String.format("%.2f%%", successRate),
                "lastRateLimitHit", lastRateLimitHit != null
                        ? lastRateLimitHit.toString()
                        : "Nunca",
                "requestsLastMinute", getRequestsInLastMinute(),
                "requestsLastHour", getRequestsInLastHour()
        );
    }

    /**
     * ✅ Requests no último minuto
     */
    private int getRequestsInLastMinute() {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1).withSecond(0).withNano(0);

        return requestsPerMinute.entrySet().stream()
                .filter(e -> e.getKey().isAfter(oneMinuteAgo))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    /**
     * ✅ Requests na última hora
     */
    private int getRequestsInLastHour() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1).withSecond(0).withNano(0);

        return requestsPerMinute.entrySet().stream()
                .filter(e -> e.getKey().isAfter(oneHourAgo))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    /**
     * ✅ Limpar contadores antigos (> 1 hora)
     */
    private void cleanOldRequestCounts() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        requestsPerMinute.entrySet().removeIf(e -> e.getKey().isBefore(oneHourAgo));
    }

    /**
     * ✅ Verificar se rate limit está próximo
     */
    public boolean isNearRateLimit() {
        int requestsLastMinute = getRequestsInLastMinute();
        return requestsLastMinute >= 25; // 25 de 30 = 83%
    }

    /**
     * ✅ Resetar estatísticas (para testes)
     */
    public void reset() {
        rateLimitHitsCount.set(0);
        successRequestsCount.set(0);
        failedRequestsCount.set(0);
        requestsPerMinute.clear();
        lastRateLimitHit = null;

        log.info("🗑️ Métricas resetadas");
    }
}