// back/src/main/java/com/crypto/controller/CacheAdminController.java
package com.crypto.controller;

import com.crypto.config.CacheConfig;
import com.crypto.service.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ✅ NOVO: Endpoints administrativos para gerenciar cache
 *
 * Útil para:
 * - Debug de problemas de cache
 * - Monitoramento de performance
 * - Limpeza manual quando necessário
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheAdminController {

    private final CryptoService cryptoService;
    private final CacheManager cacheManager;
    private final CacheConfig.CacheStatsLogger cacheStatsLogger;

    /**
     * Limpar todo o cache
     */
    @PostMapping("/clear")
    public ResponseEntity<?> clearCache() {
        try {
            log.info("🗑️ Limpando cache via API...");

            cryptoService.clearCache();

            // Limpar todos os caches do CacheManager
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.info("✅ Cache [{}] limpo", cacheName);
                }
            });

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache limpo com sucesso",
                    "timestamp", System.currentTimeMillis()
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
     * Aquecer cache (warm-up)
     */
    @PostMapping("/warmup")
    public ResponseEntity<?> warmUpCache() {
        try {
            log.info("🔥 Aquecendo cache via API...");

            cryptoService.warmUpCache();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache aquecido com sucesso",
                    "timestamp", System.currentTimeMillis()
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
     * Estatísticas do cache
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getCacheStats() {
        try {
            log.info("📊 Obtendo estatísticas de cache...");

            List<Map<String, Object>> cacheStats = new ArrayList<>();

            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("name", cacheName);
                    stats.put("type", cache.getClass().getSimpleName());

                    // Tentar obter tamanho (se disponível)
                    try {
                        var nativeCache = cache.getNativeCache();
                        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                            var caffeineCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;
                            var caffeineStats = caffeineCache.stats();

                            stats.put("size", caffeineCache.estimatedSize());
                            stats.put("hits", caffeineStats.hitCount());
                            stats.put("misses", caffeineStats.missCount());
                            stats.put("hitRate", String.format("%.2f%%", caffeineStats.hitRate() * 100));
                            stats.put("evictions", caffeineStats.evictionCount());
                        }
                    } catch (Exception e) {
                        stats.put("error", "Stats not available: " + e.getMessage());
                    }

                    cacheStats.add(stats);
                }
            });

            // Log stats também
            cacheStatsLogger.logStats();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "caches", cacheStats,
                    "totalCaches", cacheManager.getCacheNames().size(),
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Listar nomes de todos os caches
     */
    @GetMapping("/names")
    public ResponseEntity<?> getCacheNames() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "cacheNames", cacheManager.getCacheNames(),
                "count", cacheManager.getCacheNames().size()
        ));
    }

    /**
     * Limpar cache específico
     */
    @PostMapping("/clear/{cacheName}")
    public ResponseEntity<?> clearSpecificCache(@PathVariable String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);

            if (cache == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Cache não encontrado: " + cacheName
                ));
            }

            cache.clear();
            log.info("✅ Cache [{}] limpo", cacheName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache " + cacheName + " limpo com sucesso"
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao limpar cache {}: {}", cacheName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}