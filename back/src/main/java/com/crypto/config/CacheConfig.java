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
 * ✅ CONFIGURAÇÃO DE CACHE - OTIMIZADA PARA RATE LIMITING
 *
 * Estratégia: Aumentar TTL do cache para reduzir requisições à API
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * ✅ CACHE LOCAL - CAFFEINE COM TTL ESTENDIDO
     *
     * Mudanças:
     * - cryptoPrices: 5min → 10min (reduz 50% das requisições)
     * - allCryptoPrices: 5min → 10min (reduz 50% das requisições)
     */
    @Primary
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        log.info("🚀 Configurando Caffeine Cache com TTL estendido");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "cryptoPrices",       // Cache individual (10min)
                "allCryptoPrices",    // Cache lista completa (10min)
                "portfolioData",      // Cache portfolio (5min)
                "userAlerts"          // Cache alertas (5min)
        );

        // ✅ CONFIGURAÇÃO GLOBAL: 10 minutos de TTL
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)  // ✅ Aumentado de 5min para 10min
                .recordStats()
        );

        log.info("✅ Caffeine Cache configurado:");
        log.info("   - TTL: 10 minutos (reduz 50% das requisições)");
        log.info("   - MaxSize: 1000 entradas");
        log.info("   - Stats: habilitado");

        return cacheManager;
    }

    /**
     * ✅ CACHE ESPECÍFICO PARA HISTÓRICO (TTL MAIOR)
     */
    @Bean("historyCacheManager")
    public CacheManager historyCacheManager() {
        log.info("🚀 Configurando cache para histórico (TTL: 1 hora)");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager("cryptoHistory");

        // Histórico muda pouco, pode ter TTL maior
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(60, TimeUnit.MINUTES)  // 1 hora
                .recordStats()
        );

        log.info("✅ Cache de histórico configurado: TTL=1h");

        return cacheManager;
    }
}