package com.crypto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class ApiStatusController {

    private static final AtomicInteger rateLimitHits = new AtomicInteger(0);
    private static final AtomicInteger successfulRequests = new AtomicInteger(0);
    private static final AtomicLong lastRateLimitTime = new AtomicLong(0);
    private static final Map<String, Integer> requestsPerMinute = new ConcurrentHashMap<>();


    @GetMapping
    public ResponseEntity<?> getApiStatus() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRateLimit = currentTime - lastRateLimitTime.get();
        boolean isRateLimited = timeSinceLastRateLimit < 60_000; // Últimos 60 segundos

        Map<String, Object> status = Map.of(
                "timestamp", LocalDateTime.now(),
                "coingeckoApi", Map.of(
                        "status", isRateLimited ? "RATE_LIMITED" : "OPERATIONAL",
                        "usingMockData", isRateLimited,
                        "rateLimitHits", rateLimitHits.get(),
                        "successfulRequests", successfulRequests.get(),
                        "lastRateLimitSeconds", timeSinceLastRateLimit / 1000,
                        "message", isRateLimited
                                ? "⚠️ Usando dados simulados (rate limit atingido)"
                                : "✅ Dados em tempo real da CoinGecko"
                ),
                "cache", Map.of(
                        "ttl", "30 minutos",
                        "type", "Caffeine (local)",
                        "size", "2000 entradas"
                ),
                "recommendations", isRateLimited
                        ? "Os dados estão simulados. Aguarde 1 minuto para dados reais."
                        : "Tudo funcionando normalmente!"
        );

        return ResponseEntity.ok(status);
    }


    public static void recordRateLimitHit() {
        rateLimitHits.incrementAndGet();
        lastRateLimitTime.set(System.currentTimeMillis());
        log.warn("⚠️ Rate Limit registrado (total: {})", rateLimitHits.get());
    }


    public static void recordSuccessfulRequest() {
        successfulRequests.incrementAndGet();
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        long timeSinceLastRateLimit = System.currentTimeMillis() - lastRateLimitTime.get();
        boolean healthy = timeSinceLastRateLimit > 300_000; // 5 minutos sem rate limit

        Map<String, Object> health = Map.of(
                "status", healthy ? "UP" : "DEGRADED",
                "message", healthy
                        ? "Sistema operando normalmente"
                        : "Sistema operando com dados simulados",
                "details", Map.of(
                        "rateLimitStatus", timeSinceLastRateLimit > 60_000 ? "OK" : "ACTIVE",
                        "cacheStatus", "OK",
                        "databaseStatus", "OK"
                )
        );

        return ResponseEntity.ok(health);
    }


    @PostMapping("/reset-stats")
    public ResponseEntity<?> resetStats() {
        rateLimitHits.set(0);
        successfulRequests.set(0);
        lastRateLimitTime.set(0);
        requestsPerMinute.clear();

        log.info("📊 Estatísticas de API resetadas");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Estatísticas resetadas com sucesso"
        ));
    }
}