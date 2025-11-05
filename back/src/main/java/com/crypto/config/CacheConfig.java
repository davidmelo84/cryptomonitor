// back/src/main/java/com/crypto/config/CacheConfig.java
package com.crypto.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ✅ CONFIGURAÇÃO DE CACHE HÍBRIDO
 *
 * ESTRATÉGIA:
 * - L1 (Caffeine): Cache local ultra-rápido (5 min)
 * - L2 (Redis): Cache distribuído para múltiplas instâncias (30 min) - OPCIONAL
 *
 * BENEFÍCIOS:
 * - Reduz latência (L1 local)
 * - Compartilha dados entre instâncias (L2 Redis)
 * - TTL automático (sem dados obsoletos)
 * - Fallback gracioso (se Redis falhar, usa Caffeine)
 */
@Slf4j
@Configuration
@EnableCaching
@EnableScheduling // ✅ permite agendamento periódico (para logs de cache)
public class CacheConfig {

    /**
     * ✅ CACHE L1 (LOCAL) - CAFFEINE
     *
     * Sempre ativo, ultra-rápido, não depende de Redis
     */
    @Primary
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        log.info("🚀 Configurando Caffeine Cache (L1 - Local)");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "cryptoPrices",     // Cache individual de cryptos
                "allCryptoPrices",  // Cache da lista completa
                "portfolioData",    // Cache de portfolios
                "userAlerts"        // Cache de alertas do usuário
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)                         // Máx. 1000 entradas
                .expireAfterWrite(5, TimeUnit.MINUTES)     // Expira após 5 min
                .recordStats()                             // Estatísticas ativas
        );

        log.info("✅ Caffeine Cache configurado: TTL=5min, MaxSize=1000");
        return cacheManager;
    }

    /**
     * ✅ CACHE L2 (DISTRIBUÍDO) - REDIS
     *
     * Ativo apenas se Redis estiver disponível e configurado
     */
    @Bean("redisCacheManager")
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        log.info("🚀 Configurando Redis Cache (L2 - Distribuído)");

        try {
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30)) // TTL padrão: 30 min
                    .disableCachingNullValues()
                    .serializeValuesWith(
                            RedisSerializationContext.SerializationPair.fromSerializer(
                                    new GenericJackson2JsonRedisSerializer()
                            )
                    );

            // Configura TTLs personalizados por cache
            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
            cacheConfigurations.put("cryptoPrices", defaultConfig.entryTtl(Duration.ofMinutes(5)));
            cacheConfigurations.put("allCryptoPrices", defaultConfig.entryTtl(Duration.ofMinutes(3)));
            cacheConfigurations.put("portfolioData", defaultConfig.entryTtl(Duration.ofMinutes(10)));
            cacheConfigurations.put("userAlerts", defaultConfig.entryTtl(Duration.ofMinutes(15)));

            RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .transactionAware()
                    .build();

            log.info("✅ Redis Cache configurado com TTLs personalizados");
            return cacheManager;

        } catch (Exception e) {
            log.warn("⚠️ Redis não disponível, fallback para Caffeine: {}", e.getMessage());
            return caffeineCacheManager();
        }
    }

    /**
     * ✅ Bean de estatísticas de cache (monitoramento)
     */
    @Bean
    public CacheStatsLogger cacheStatsLogger(CacheManager cacheManager) {
        return new CacheStatsLogger(cacheManager);
    }

    /**
     * ✅ Classe auxiliar: logger de estatísticas do cache
     */
    public static class CacheStatsLogger {
        private final CacheManager cacheManager;

        public CacheStatsLogger(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }

        public void logStats() {
            if (cacheManager instanceof CaffeineCacheManager caffeine) {
                caffeine.getCacheNames().forEach(cacheName -> {
                    var cache = caffeine.getCache(cacheName);
                    if (cache != null) {
                        var nativeCache = cache.getNativeCache();
                        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> c) {
                            var stats = c.stats();
                            log.info("📊 Cache [{}] → Hits: {}, Misses: {}, HitRate: {:.2f}%",
                                    cacheName,
                                    stats.hitCount(),
                                    stats.missCount(),
                                    stats.hitRate() * 100
                            );
                        }
                    }
                });
            }
        }
    }

    /**
     * ✅ Scheduler: executa logs de estatísticas a cada 5 minutos
     */
    private final CacheStatsLogger cacheStatsLogger;

    public CacheConfig(CacheStatsLogger cacheStatsLogger) {
        this.cacheStatsLogger = cacheStatsLogger;
    }

    @Scheduled(fixedDelay = 300000) // A cada 5 minutos
    public void logCacheStats() {
        log.info("🕒 Executando log periódico de estatísticas do cache...");
        cacheStatsLogger.logStats();
    }
}
