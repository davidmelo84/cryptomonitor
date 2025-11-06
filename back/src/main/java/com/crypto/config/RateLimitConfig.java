// back/src/main/java/com/crypto/config/RateLimitConfig.java
package com.crypto.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ SPRINT 1 - RATE LIMITING
 *
 * Configuração de rate limiting por IP usando Bucket4j.
 *
 * Limites:
 * - API Geral: 100 req/min por IP
 * - Auth Endpoints: 10 req/min por IP (proteção contra brute force)
 * - Admin Endpoints: 50 req/min por IP
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    @Value("${rate-limit.api.requests-per-minute:100}")
    private int apiRequestsPerMinute;

    @Value("${rate-limit.auth.requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Value("${rate-limit.admin.requests-per-minute:50}")
    private int adminRequestsPerMinute;

    /**
     * ✅ Cache de buckets por IP
     * Key: IP do cliente
     * Value: Bucket com limites configurados
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * ✅ Bucket para endpoints gerais da API
     * 100 requisições por minuto por IP
     */
    @Bean("apiBucket")
    public Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(
                apiRequestsPerMinute,
                Refill.intervally(apiRequestsPerMinute, Duration.ofMinutes(1))
        );

        log.info("✅ Rate Limit API configurado: {} req/min", apiRequestsPerMinute);
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * ✅ Bucket para endpoints de autenticação
     * 10 requisições por minuto por IP (proteção contra brute force)
     */
    @Bean("authBucket")
    public Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(
                authRequestsPerMinute,
                Refill.intervally(authRequestsPerMinute, Duration.ofMinutes(1))
        );

        log.info("✅ Rate Limit Auth configurado: {} req/min", authRequestsPerMinute);
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * ✅ Bucket para endpoints administrativos
     * 50 requisições por minuto por IP
     */
    @Bean("adminBucket")
    public Bucket createAdminBucket() {
        Bandwidth limit = Bandwidth.classic(
                adminRequestsPerMinute,
                Refill.intervally(adminRequestsPerMinute, Duration.ofMinutes(1))
        );

        log.info("✅ Rate Limit Admin configurado: {} req/min", adminRequestsPerMinute);
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * ✅ Resolver bucket por IP e tipo de endpoint
     */
    public Bucket resolveBucket(String key, BucketType type) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit;

            switch (type) {
                case AUTH:
                    limit = Bandwidth.classic(
                            authRequestsPerMinute,
                            Refill.intervally(authRequestsPerMinute, Duration.ofMinutes(1))
                    );
                    break;
                case ADMIN:
                    limit = Bandwidth.classic(
                            adminRequestsPerMinute,
                            Refill.intervally(adminRequestsPerMinute, Duration.ofMinutes(1))
                    );
                    break;
                case API:
                default:
                    limit = Bandwidth.classic(
                            apiRequestsPerMinute,
                            Refill.intervally(apiRequestsPerMinute, Duration.ofMinutes(1))
                    );
                    break;
            }

            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * ✅ Tipos de buckets
     */
    public enum BucketType {
        API,    // Endpoints gerais (100/min)
        AUTH,   // Autenticação (10/min)
        ADMIN   // Admin (50/min)
    }

    /**
     * ✅ Limpar buckets inativos (cleanup periódico)
     */
    public void clearInactiveBuckets() {
        int sizeBefore = buckets.size();

        buckets.entrySet().removeIf(entry -> {
            Bucket bucket = entry.getValue();
            // Remove se bucket está vazio e não foi usado recentemente
            return bucket.getAvailableTokens() == apiRequestsPerMinute;
        });

        int sizeAfter = buckets.size();

        if (sizeBefore != sizeAfter) {
            log.debug("🗑️ Buckets limpos: {} removidos ({} -> {})",
                    sizeBefore - sizeAfter, sizeBefore, sizeAfter);
        }
    }

    /**
     * ✅ Estatísticas de rate limiting
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "totalBuckets", buckets.size(),
                "apiLimit", apiRequestsPerMinute + " req/min",
                "authLimit", authRequestsPerMinute + " req/min",
                "adminLimit", adminRequestsPerMinute + " req/min"
        );
    }
}