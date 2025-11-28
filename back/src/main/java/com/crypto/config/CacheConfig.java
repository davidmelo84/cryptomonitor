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

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Primary
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        log.info("🚀 Configurando Caffeine Cache - TTL 30min");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "cryptoPrices",
                "allCryptoPrices",
                "portfolioData",
                "userAlerts",
                "binancePrices",
                "topBinancePrices",
                "coinCapPrices",
                "coinCapHistory",
                "topCoinCapPrices",
                "cryptoHistory",
                "topCryptoPrices",
                "userPortfolio",
                "userTransactions",
                "userAlertRules"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .recordStats()
        );

        // Permitir criação dinâmica
        cacheManager.setAllowNullValues(false);

        log.info("✅ Caffeine Cache configurado com TTL=30min + expireAfterAccess=15min (2000 entradas)");
        return cacheManager;
    }


    @Bean("historyCacheManager")
    public CacheManager historyCacheManager() {
        log.info("🕒 Configurando cache para histórico (TTL: 2h)");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager("cryptoHistory");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(120, TimeUnit.MINUTES)
                .expireAfterAccess(60, TimeUnit.MINUTES) //
                .recordStats()
        );

        return cacheManager;
    }


    @Bean("userDataCacheManager")
    public CacheManager userDataCacheManager() {
        log.info("👤 Configurando cache de dados de usuário (TTL: 5min)");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "userPortfolio",
                "userTransactions",
                "userAlertRules"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .recordStats()
        );

        return cacheManager;
    }
}
