// back/src/main/java/com/crypto/controller/RateLimitMonitoringController.java
package com.crypto.controller;

import com.crypto.config.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ✅ SPRINT 1 - RATE LIMITING MONITORING
 *
 * Endpoint para monitorar estatísticas de rate limiting.
 *
 * **IMPORTANTE**: Este endpoint também está sujeito a rate limiting!
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/rate-limit")
@RequiredArgsConstructor
public class RateLimitMonitoringController {

    private final RateLimitConfig rateLimitConfig;

    /**
     * ✅ Obter estatísticas de rate limiting
     *
     * GET /api/admin/rate-limit/stats
     *
     * Response:
     * {
     *   "totalBuckets": 45,
     *   "apiLimit": "100 req/min",
     *   "authLimit": "10 req/min",
     *   "adminLimit": "50 req/min"
     * }
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = rateLimitConfig.getStats();

            log.debug("📊 Rate limit stats requested");

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Erro ao obter stats de rate limit: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Limpar buckets inativos (cleanup manual)
     *
     * POST /api/admin/rate-limit/cleanup
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupBuckets() {
        try {
            log.info("🗑️ Iniciando limpeza de buckets inativos");

            Map<String, Object> statsBefore = rateLimitConfig.getStats();
            rateLimitConfig.clearInactiveBuckets();
            Map<String, Object> statsAfter = rateLimitConfig.getStats();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cleanup concluído",
                    "before", statsBefore,
                    "after", statsAfter
            ));

        } catch (Exception e) {
            log.error("❌ Erro no cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Health check do rate limiting
     *
     * GET /api/admin/rate-limit/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> stats = rateLimitConfig.getStats();

            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "rateLimiting", "enabled",
                    "stats", stats,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "status", "DOWN",
                            "error", e.getMessage()
                    ));
        }
    }
}