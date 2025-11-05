// back/src/main/java/com/crypto/config/CacheConfig.java
package com.crypto.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * ✅ CONFIGURAÇÃO DE CACHE - APENAS CAFFEINE
 * Redis removido para simplificar deploy
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * ✅ CACHE LOCAL - CAFFEINE
     */
    @Primary
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        log.info("🚀 Configurando Caffeine Cache (Local)");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "cryptoPrices",
                "allCryptoPrices",
                "portfolioData",
                "userAlerts"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
        );

        log.info("✅ Caffeine Cache configurado: TTL=5min, MaxSize=1000");
        return cacheManager;
    }
}