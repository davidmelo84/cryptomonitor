// back/src/main/java/com/crypto/controller/CacheAdminController.java
package com.crypto.controller;

import com.crypto.service.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheAdminController {

    private final CryptoService cryptoService;
    private final CacheManager cacheManager;

    @PostMapping("/clear")
    public ResponseEntity<?> clearCache() {
        try {
            log.info("🗑️ Limpando cache via API...");

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

    @PostMapping("/warmup")
    public ResponseEntity<?> warmUpCache() {
        try {
            log.info("🔥 Aquecendo cache via API...");
            cryptoService.warmUpCache();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cache aquecido com sucesso"
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao aquecer cache: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getCacheStats() {
        try {
            List<Map<String, Object>> cacheStats = new ArrayList<>();

            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("name", cacheName);
                    stats.put("type", cache.getClass().getSimpleName());
                    cacheStats.add(stats);
                }
            });

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "caches", cacheStats,
                    "totalCaches", cacheManager.getCacheNames().size()
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/names")
    public ResponseEntity<?> getCacheNames() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "cacheNames", cacheManager.getCacheNames(),
                "count", cacheManager.getCacheNames().size()
        ));
    }

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