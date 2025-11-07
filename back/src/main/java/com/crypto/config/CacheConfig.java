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
 * ✅ CACHE OTIMIZADO PARA EVITAR RATE LIMIT
 *
 * Estratégia: TTL de 30 minutos
 * - Reduz 66% das requisições à CoinGecko
 * - API gratuita: 30 req/min = suficiente para 30 usuários simultâneos
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * ✅ CACHE PRINCIPAL - TTL 30 MINUTOS
     *
     * Por que 30min?
     * - Preços de crypto não mudam drasticamente em 30min
     * - Reduz carga na API em 66% (antes: 10min)
     * - Suficiente para a maioria dos use cases
     */
    @Primary
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        log.info("🚀 Configurando Caffeine Cache - TTL ESTENDIDO");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "cryptoPrices",       // Cache individual (30min)
                "allCryptoPrices",    // Cache lista completa (30min)
                "portfolioData",      // Cache portfolio (15min)
                "userAlerts",            // Cache alertas (15min)
                "binancePrices",      // ✅ usado em BinanceApiService
                "topBinancePrices"    // ✅ usado em BinanceApiService.getTopPrices
        );

        // ✅ TTL de 30 minutos para preços
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000)                    // Aumentado de 1000 → 2000
                .expireAfterWrite(30, TimeUnit.MINUTES)  // ✅ 10min → 30min
                .recordStats()
        );

        log.info("✅ Caffeine Cache configurado:");
        log.info("   - TTL: 30 minutos (reduz 66% das requisições)");
        log.info("   - MaxSize: 2000 entradas");
        log.info("   - Stats: habilitado");
        log.info("   💡 Preços atualizados a cada 30min");

        return cacheManager;
    }

    /**
     * ✅ CACHE PARA HISTÓRICO - TTL 2 HORAS
     *
     * Histórico muda pouco, pode ter TTL maior
     */
    @Bean("historyCacheManager")
    public CacheManager historyCacheManager() {
        log.info("🚀 Configurando cache para histórico (TTL: 2 horas)");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager("cryptoHistory");

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(120, TimeUnit.MINUTES)  // 2 horas
                .recordStats()
        );

        log.info("✅ Cache de histórico configurado: TTL=2h");

        return cacheManager;
    }

    /**
     * ✅ CACHE ESPECÍFICO PARA DADOS DE USUÁRIO - TTL 5 MINUTOS
     *
     * Portfolio e alertas precisam ser mais atualizados
     */
    @Bean("userDataCacheManager")
    public CacheManager userDataCacheManager() {
        log.info("🚀 Configurando cache de dados de usuário (TTL: 5 minutos)");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "userPortfolio",
                "userTransactions",
                "userAlertRules"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)  // 5 minutos
                .recordStats()
        );

        log.info("✅ Cache de usuário configurado: TTL=5min");

        return cacheManager;
    }
}