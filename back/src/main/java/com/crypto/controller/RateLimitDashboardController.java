// back/src/main/java/com/crypto/controller/RateLimitDashboardController.java
package com.crypto.controller;

import com.crypto.service.CoinGeckoApiService;
import com.crypto.service.CryptoService;
import com.crypto.service.RateLimitMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ✅ DASHBOARD COMPLETO - MONITORAMENTO DE RATE LIMITING
 *
 * Endpoints:
 * - GET /api/rate-limit/dashboard    → Dashboard completo
 * - GET /api/rate-limit/status       → Status resumido
 * - GET /api/rate-limit/metrics      → Métricas detalhadas
 * - POST /api/rate-limit/clear-cache → Limpar cache
 * - POST /api/rate-limit/reset-stats → Resetar estatísticas
 */
@Slf4j
@RestController
@RequestMapping("/api/rate-limit")
@RequiredArgsConstructor
public class RateLimitDashboardController {

    private final CoinGeckoApiService coinGeckoService;
    private final CryptoService cryptoService;
    private final RateLimitMetricsService metricsService;
    private final CacheManager cacheManager;

    /**
     * ✅ DASHBOARD COMPLETO
     *
     * GET /api/rate-limit/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            // 1. Status da API
            dashboard.put("coinGecko", Map.of(
                    "provider", "CoinGecko",
                    "tier", "FREE",
                    "available", coinGeckoService.isAvailable(),
                    "rateLimit", "30 req/min",
                    "apiUrl", "https://api.coingecko.com/api/v3"
            ));

            // 2. Métricas de uso
            Map<String, Object> metrics = metricsService.getStatistics();
            dashboard.put("metrics", metrics);

            // 3. Status do Cache
            Map<String, Object> cacheStats = new HashMap<>();
            Collection<String> cacheNames = cacheManager.getCacheNames();
            cacheStats.put("totalCaches", cacheNames.size());
            cacheStats.put("cacheNames", cacheNames);
            cacheStats.put("ttl", "30 minutos");
            cacheStats.put("strategy", "Caffeine (in-memory)");

            dashboard.put("cache", cacheStats);

            // 4. Comparação de Uso (estimativa)
            Map<String, Object> usage = new HashMap<>();
            usage.put("effectiveRequestsPerHour", 2);
            usage.put("theoreticalMaxPerHour", 1800); // 30 * 60
            usage.put("reduction", "99.89%");
            usage.put("strategy", "Cache 30min + WebSocket");
            usage.put("schedulerInterval", "30 minutos");

            dashboard.put("usage", usage);

            // 5. Alertas
            List<String> alerts = new ArrayList<>();
            if (metricsService.isNearRateLimit()) {
                alerts.add("⚠️ Requests por minuto próximo do limite!");
            }
            if (!(boolean) dashboard.get("coinGecko.available")) {
                alerts.add("❌ CoinGecko API indisponível!");
            }
            if (alerts.isEmpty()) {
                alerts.add("✅ Tudo funcionando normalmente");
            }
            dashboard.put("alerts", alerts);

            // 6. Recomendações
            List<String> recommendations = List.of(
                    "✅ Cache está ativo (TTL: 30min)",
                    "✅ WebSocket habilitado (tempo real)",
                    "✅ Fallback para banco configurado",
                    "✅ Scheduler executando a cada 30min",
                    "💡 Total estimado: ~48 requests/dia ao CoinGecko"
            );
            dashboard.put("recommendations", recommendations);

            // 7. Timestamp
            dashboard.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("❌ Erro ao gerar dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ STATUS RESUMIDO
     *
     * GET /api/rate-limit/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            Map<String, Object> metrics = metricsService.getStatistics();
            Map<String, Object> apiStatus = cryptoService.getApiStatus();

            Map<String, Object> status = new HashMap<>();
            status.put("healthy", coinGeckoService.isAvailable());
            status.put("rateLimitHits", metrics.get("rateLimitHits"));
            status.put("successRate", metrics.get("successRate"));
            status.put("requestsLastMinute", metrics.get("requestsLastMinute"));
            status.put("requestsLastHour", metrics.get("requestsLastHour"));
            status.put("provider", "CoinGecko");
            status.put("apiStatus", apiStatus);
            status.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("❌ Erro ao obter status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ MÉTRICAS DETALHADAS
     *
     * GET /api/rate-limit/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        try {
            Map<String, Object> metrics = metricsService.getStatistics();
            metrics.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("❌ Erro ao obter métricas: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ⚠️ LIMPAR CACHE (força nova request)
     *
     * POST /api/rate-limit/clear-cache
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<?> clearCache() {
        try {
            log.warn("🗑️ Limpando cache via API...");

            cryptoService.clearCache();

            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.info("✅ Cache [{}] limpo", cacheName);
                }
            });

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache limpo. Próxima request irá buscar dados novos do CoinGecko.",
                    "warning", "Isso consumirá rate limit!",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao limpar cache: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 🔥 AQUECER CACHE (útil após deploy)
     *
     * POST /api/rate-limit/warm-cache
     */
    @PostMapping("/warm-cache")
    public ResponseEntity<?> warmCache() {
        try {
            log.info("🔥 Aquecendo cache via API...");
            cryptoService.warmUpCache();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache aquecido com sucesso!",
                    "note", "Dados já disponíveis para consulta",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao aquecer cache: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 🗑️ RESETAR ESTATÍSTICAS
     *
     * POST /api/rate-limit/reset-stats
     */
    @PostMapping("/reset-stats")
    public ResponseEntity<?> resetStats() {
        try {
            log.warn("🗑️ Resetando estatísticas via API...");

            metricsService.reset();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estatísticas resetadas com sucesso",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao resetar stats: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ CALCULAR ECONOMIA DE REQUESTS
     *
     * GET /api/rate-limit/savings
     */
    @GetMapping("/savings")
    public ResponseEntity<?> calculateSavings() {
        Map<String, Object> savings = new HashMap<>();

        // Sem cache: Frontend faz 1 request/usuário/minuto
        int usersSimulated = 10;
        int requestsPerUserPerHour = 60;
        int withoutCachePerHour = usersSimulated * requestsPerUserPerHour;

        // Com cache: Backend faz 2 requests/hora
        int withCachePerHour = 2;

        int savedRequests = withoutCachePerHour - withCachePerHour;
        double reductionPercent = ((double) savedRequests / withoutCachePerHour) * 100;

        savings.put("scenario", Map.of(
                "users", usersSimulated,
                "requestsPerUserPerHour", requestsPerUserPerHour
        ));

        savings.put("withoutCache", Map.of(
                "requestsPerHour", withoutCachePerHour,
                "requestsPerDay", withoutCachePerHour * 24,
                "requestsPerMonth", withoutCachePerHour * 24 * 30
        ));

        savings.put("withCache", Map.of(
                "requestsPerHour", withCachePerHour,
                "requestsPerDay", withCachePerHour * 24,
                "requestsPerMonth", withCachePerHour * 24 * 30
        ));

        savings.put("savings", Map.of(
                "requestsPerHour", savedRequests,
                "requestsPerDay", savedRequests * 24,
                "requestsPerMonth", savedRequests * 24 * 30,
                "reductionPercent", String.format("%.2f%%", reductionPercent)
        ));

        return ResponseEntity.ok(savings);
    }
}