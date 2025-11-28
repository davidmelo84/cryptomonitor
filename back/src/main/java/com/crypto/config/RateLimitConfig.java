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



@Slf4j
@Configuration
public class RateLimitConfig {

    @Value("${rate-limit.api.requests-per-minute:100}")
    private int apiRequestsPerMinute;

    @Value("${rate-limit.auth.requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Value("${rate-limit.admin.requests-per-minute:50}")
    private int adminRequestsPerMinute;


    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();


    @Bean("apiBucket")
    public Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(
                apiRequestsPerMinute,
                Refill.intervally(apiRequestsPerMinute, Duration.ofMinutes(1))
        );

        log.info("✅ Rate Limit API configurado: {} req/min", apiRequestsPerMinute);
        return Bucket.builder().addLimit(limit).build();
    }


    @Bean("authBucket")
    public Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(
                authRequestsPerMinute,
                Refill.intervally(authRequestsPerMinute, Duration.ofMinutes(1))
        );

        log.info("✅ Rate Limit Auth configurado: {} req/min", authRequestsPerMinute);
        return Bucket.builder().addLimit(limit).build();
    }


    @Bean("adminBucket")
    public Bucket createAdminBucket() {
        Bandwidth limit = Bandwidth.classic(
                adminRequestsPerMinute,
                Refill.intervally(adminRequestsPerMinute, Duration.ofMinutes(1))
        );

        log.info("✅ Rate Limit Admin configurado: {} req/min", adminRequestsPerMinute);
        return Bucket.builder().addLimit(limit).build();
    }


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


    public enum BucketType {
        API,    // Endpoints gerais (100/min)
        AUTH,   // Autenticação (10/min)
        ADMIN   // Admin (50/min)
    }


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


    public Map<String, Object> getStats() {
        return Map.of(
                "totalBuckets", buckets.size(),
                "apiLimit", apiRequestsPerMinute + " req/min",
                "authLimit", authRequestsPerMinute + " req/min",
                "adminLimit", adminRequestsPerMinute + " req/min"
        );
    }
}