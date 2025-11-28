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

            boolean available = coinGeckoService.isAvailable();
            details.put("provider", "CoinGecko");
            details.put("tier", "FREE");
            details.put("available", available);

            Map<String, Object> rateLimitInfo = coinGeckoService.getRateLimitInfo();
            details.put("rateLimit", rateLimitInfo);

            var cache = cacheManager.getCache("allCryptoPrices");
            boolean cacheHealthy = cache != null;
            details.put("cache", Map.of(
                    "name", "allCryptoPrices",
                    "healthy", cacheHealthy,
                    "ttl", "30 minutes"
            ));

            details.put("lastCheck", LocalDateTime.now());

            if (available && cacheHealthy) {
                return Health.up()
                        .withDetails(details)
                        .build();
            } else if (cacheHealthy) {
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