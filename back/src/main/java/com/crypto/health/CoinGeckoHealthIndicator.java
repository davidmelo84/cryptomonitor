// back/src/main/java/com/crypto/health/CoinGeckoHealthIndicator.java
package com.crypto.health;

import com.crypto.controller.ApiStatusController;
import com.crypto.service.CoinGeckoApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ✅ HEALTH CHECK ESPECÍFICO DO COINGECKO
 *
 * Exposto em: /actuator/health
 *
 * Mostra:
 * - Status da API
 * - Rate limit hits
 * - Requests bem-sucedidos
 * - Status do cache
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoinGeckoHealthIndicator implements HealthIndicator {

    private final CoinGeckoApiService coinGeckoService;
    private final CacheManager cacheManager;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();

            // ✅ 1. Verificar disponibilidade da API
            boolean available = coinGeckoService.isAvailable();
            details.put("provider", "CoinGecko");
            details.put("tier", "FREE");
            details.put("available", available);

            // ✅ 2. Informações de rate limit
            Map<String, Object> rateLimitInfo = coinGeckoService.getRateLimitInfo();
            details.put("rateLimit", rateLimitInfo);

            // ✅ 3. Status do cache
            var cache = cacheManager.getCache("allCryptoPrices");
            boolean cacheHealthy = cache != null;
            details.put("cache", Map.of(
                    "name", "allCryptoPrices",
                    "healthy", cacheHealthy,
                    "ttl", "30 minutes"
            ));

            // ✅ 4. Timestamp
            details.put("lastCheck", LocalDateTime.now());

            // ✅ 5. Determinar status geral
            if (available && cacheHealthy) {
                return Health.up()
                        .withDetails(details)
                        .build();
            } else if (cacheHealthy) {
                // API indisponível mas cache funcionando
                return Health.status("DEGRADED")
                        .withDetails(details)
                        .withDetail("reason", "API indisponível, usando cache")
                        .build();
            } else {
                return Health.down()
                        .withDetails(details)
                        .withDetail("reason", "API e cache indisponíveis")
                        .build();
            }

        } catch (Exception e) {
            log.error("❌ Erro no health check: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}