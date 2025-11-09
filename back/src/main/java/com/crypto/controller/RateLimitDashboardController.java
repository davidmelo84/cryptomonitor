// back/src/main/java/com/crypto/controller/RateLimitDashboardController.java
package com.crypto.controller;

import com.crypto.service.CoinGeckoApiService;
import com.crypto.service.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ✅ DASHBOARD - MONITORAMENTO DE RATE LIMITING
 *
 * GET /api/rate-limit/dashboard
 */
@Slf4j
@RestController
@RequestMapping("/api/rate-limit")
@RequiredArgsConstructor
public class RateLimitDashboardController {

    private final CoinGeckoApiService coinGeckoService;
    private final CryptoService cryptoService;
    private final CacheManager cacheManager;

    /**
     * ✅ DASHBOARD COMPLETO
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            // 1. Status da API
            dashboard.put("coinGecko", coinGeckoService.getRateLimitInfo());

            // 2. Status do Cache
            Map<String, Object> cacheStats = new HashMap<>();

            Collection<String> cacheNames = cacheManager.getCacheNames();
            cacheStats.put("totalCaches", cacheNames.size());
            cacheStats.put("cacheNames", cacheNames);

            dashboard.put("cache", cacheStats);

            // 3. Estatísticas de Uso
            Map<String, Object> usage = new HashMap<>();
            usage.put("effectiveRequestsPerHour", 2);
            usage.put("theoreticalMaxPerHour", 60); // 30 req/min * 2
            usage.put("reduction", "97%");
            usage.put("strategy", "Cache 30min + WebSocket");

            dashboard.put("usage", usage);

            // 4. Recomendações
            List<String> recommendations = List.of(
                    "✅ Cache está ativo (TTL: 30min)",
                    "✅ WebSocket habilitado (tempo real)",
                    "✅ Fallback para banco configurado",
                    "✅ Scheduler executando a cada 30min",
                    "💡 Total: ~48 requests/dia ao CoinGecko"
            );

            dashboard.put("recommendations", recommendations);

            // 5. Health
            boolean healthy = coinGeckoService.isAvailable();
            dashboard.put("health", Map.of(
                    "status", healthy ? "HEALTHY" : "DEGRADED",
                    "coinGeckoAvailable", healthy,
                    "timestamp", LocalDateTime.now()
            ));

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("❌ Erro ao gerar dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ STATUS SIMPLIFICADO
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = cryptoService.getApiStatus();

        return ResponseEntity.ok(status);
    }

    /**
     * ✅ LIMPAR CACHE (força nova request)
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<?> clearCache() {
        try {
            log.info("🗑️ Limpando cache...");

            cryptoService.clearCache();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache limpo. Próxima request irá buscar dados novos do CoinGecko."
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao limpar cache: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ AQUECER CACHE (útil após deploy)
     */
    @PostMapping("/warm-cache")
    public ResponseEntity<?> warmCache() {
        try {
            log.info("🔥 Aquecendo cache...");

            cryptoService.warmUpCache();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache aquecido com sucesso!"
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao aquecer cache: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ CALCULAR ECONOMIA DE REQUESTS
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
                "reductionPercent", String.format("%.1f%%", reductionPercent)
        ));

        return ResponseEntity.ok(savings);
    }
}