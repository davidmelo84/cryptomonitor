package com.crypto.service;

import com.crypto.model.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



@Slf4j
@Service
@RequiredArgsConstructor
public class SmartCacheService {

    private final CoinGeckoApiService coinGeckoService;
    private final CryptoCurrencyRepository repository;

    private final Map<String, CachedCrypto> memoryCache = new ConcurrentHashMap<>();
    private volatile LocalDateTime lastFullUpdate = null;


    private final AtomicInteger requestsThisMinute = new AtomicInteger(0);
    private volatile LocalDateTime minuteStart = LocalDateTime.now();
    private volatile boolean rateLimitActive = false;


    private static final int MEMORY_CACHE_TTL_MINUTES = 30;
    private static final int DB_CACHE_TTL_MINUTES = 120; // 2 horas
    private static final int MAX_REQUESTS_PER_MINUTE = 25; // Buffer de segurança
    private static final int FULL_UPDATE_INTERVAL_MINUTES = 60; // 1 hora


    public List<CryptoCurrency> getCurrentPrices() {
        log.debug("🔍 SmartCache: Buscando preços...");

        if (isMemoryCacheValid()) {
            log.info("✅ SmartCache: Usando memória (fresco)");
            return getCachedPrices();
        }

        List<CryptoCurrency> dbPrices = getFromDatabase();
        if (isDbCacheValid(dbPrices)) {
            log.info("✅ SmartCache: Usando banco (aceitável)");
            updateMemoryCache(dbPrices);
            return dbPrices;
        }

        if (canMakeApiRequest()) {
            return fetchFromApi();
        }

        log.warn("⚠️ SmartCache: Usando fallback (rate limit ativo)");
        return dbPrices.isEmpty() ? getCachedPrices() : dbPrices;
    }


    private List<CryptoCurrency> fetchFromApi() {
        try {
            log.info("🌐 SmartCache: Buscando do CoinGecko...");

            recordApiRequest();

            List<CryptoCurrency> prices = coinGeckoService.getAllPrices();

            if (prices != null && !prices.isEmpty()) {
                log.info("✅ SmartCache: {} moedas obtidas", prices.size());

                updateMemoryCache(prices);
                saveToDatabase(prices);
                lastFullUpdate = LocalDateTime.now();

                return prices;
            }

        } catch (Exception e) {
            log.error("❌ SmartCache: Erro na API: {}", e.getMessage());

            if (e.getMessage().contains("429") || e.getMessage().contains("Rate limit")) {
                activateRateLimitProtection();
            }
        }

        return getFromDatabase();
    }


    private boolean canMakeApiRequest() {
        if (Duration.between(minuteStart, LocalDateTime.now()).toSeconds() >= 60) {
            resetMinuteCounter();
        }

        if (requestsThisMinute.get() >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("⚠️ SmartCache: Limite de requests/minuto atingido ({}/{})",
                    requestsThisMinute.get(), MAX_REQUESTS_PER_MINUTE);
            return false;
        }

        if (rateLimitActive) {
            log.warn("⚠️ SmartCache: Proteção de rate limit ATIVA");
            return false;
        }

        if (lastFullUpdate != null) {
            long minutesSinceUpdate = Duration.between(lastFullUpdate, LocalDateTime.now()).toMinutes();

            if (minutesSinceUpdate < FULL_UPDATE_INTERVAL_MINUTES) {
                log.debug("⏰ SmartCache: Update muito recente ({}min atrás), aguardando...",
                        minutesSinceUpdate);
                return false;
            }
        }

        return true;
    }


    private void activateRateLimitProtection() {
        rateLimitActive = true;
        log.error("🚨 PROTEÇÃO DE RATE LIMIT ATIVADA (5 minutos)");

        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000); // 5 minutos
                rateLimitActive = false;
                log.info("✅ Proteção de rate limit DESATIVADA");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }


    private void recordApiRequest() {
        requestsThisMinute.incrementAndGet();
        log.debug("📊 Requests este minuto: {}/{}",
                requestsThisMinute.get(), MAX_REQUESTS_PER_MINUTE);
    }



    private void resetMinuteCounter() {
        int requests = requestsThisMinute.getAndSet(0);
        minuteStart = LocalDateTime.now();

        if (requests > 0) {
            log.info("📊 Minuto anterior: {} requests (limite: {})",
                    requests, MAX_REQUESTS_PER_MINUTE);
        }
    }


    private boolean isMemoryCacheValid() {
        if (memoryCache.isEmpty()) {
            return false;
        }

        CachedCrypto sample = memoryCache.values().iterator().next();
        long minutesOld = Duration.between(sample.cachedAt, LocalDateTime.now()).toMinutes();

        return minutesOld < MEMORY_CACHE_TTL_MINUTES;
    }

    private boolean isDbCacheValid(List<CryptoCurrency> cryptos) {
        if (cryptos.isEmpty()) {
            return false;
        }

        LocalDateTime lastUpdate = cryptos.get(0).getLastUpdated();
        if (lastUpdate == null) {
            return false;
        }

        long minutesOld = Duration.between(lastUpdate, LocalDateTime.now()).toMinutes();
        return minutesOld < DB_CACHE_TTL_MINUTES;
    }


    private void updateMemoryCache(List<CryptoCurrency> cryptos) {
        memoryCache.clear();
        LocalDateTime now = LocalDateTime.now();

        for (CryptoCurrency crypto : cryptos) {
            memoryCache.put(crypto.getCoinId(),
                    new CachedCrypto(crypto, now));
        }

        log.debug("💾 Memória cache atualizada: {} moedas", cryptos.size());
    }


    private List<CryptoCurrency> getCachedPrices() {
        return memoryCache.values().stream()
                .map(cached -> cached.crypto)
                .toList();
    }

    private List<CryptoCurrency> getFromDatabase() {
        try {
            return repository.findAllByOrderByMarketCapDesc();
        } catch (Exception e) {
            log.error("❌ Erro ao buscar do banco: {}", e.getMessage());
            return new ArrayList<>();
        }
    }


    private void saveToDatabase(List<CryptoCurrency> cryptos) {
        try {
            for (CryptoCurrency crypto : cryptos) {
                repository.findByCoinId(crypto.getCoinId())
                        .ifPresentOrElse(
                                existing -> {
                                    existing.setCurrentPrice(crypto.getCurrentPrice());
                                    existing.setPriceChange1h(crypto.getPriceChange1h());
                                    existing.setPriceChange24h(crypto.getPriceChange24h());
                                    existing.setPriceChange7d(crypto.getPriceChange7d());
                                    existing.setMarketCap(crypto.getMarketCap());
                                    existing.setTotalVolume(crypto.getTotalVolume());
                                    existing.setLastUpdated(LocalDateTime.now());
                                    repository.save(existing);
                                },
                                () -> {
                                    crypto.setLastUpdated(LocalDateTime.now());
                                    repository.save(crypto);
                                }
                        );
            }
            log.info("💾 Banco atualizado: {} moedas", cryptos.size());
        } catch (Exception e) {
            log.error("❌ Erro ao salvar no banco: {}", e.getMessage());
        }
    }


    @Scheduled(fixedDelay = 3600000, initialDelay = 60000) // 1 hora
    public void scheduledUpdate() {
        log.info("⏰ SmartCache: Scheduler iniciado (1x/hora)");

        if (canMakeApiRequest()) {
            List<CryptoCurrency> prices = fetchFromApi();

            if (!prices.isEmpty()) {
                log.info("✅ SmartCache: Update automático concluído");
            } else {
                log.warn("⚠️ SmartCache: Update falhou, usando cache existente");
            }
        } else {
            log.info("⏭️ SmartCache: Pulando update (proteção ativa)");
        }
    }


    public void clearCache() {
        memoryCache.clear();
        lastFullUpdate = null;
        requestsThisMinute.set(0);
        log.info("🗑️ SmartCache: Cache limpo");
    }


    public void forceUpdate() {
        log.warn("⚠️ SmartCache: FORCE UPDATE solicitado");

        // Temporariamente desativar proteção
        boolean wasActive = rateLimitActive;
        rateLimitActive = false;

        try {
            fetchFromApi();
        } finally {
            rateLimitActive = wasActive;
        }
    }


    public Map<String, Object> getStats() {
        long minutesSinceUpdate = lastFullUpdate != null
                ? Duration.between(lastFullUpdate, LocalDateTime.now()).toMinutes()
                : -1;

        return Map.of(
                "memoryCacheSize", memoryCache.size(),
                "lastUpdateMinutesAgo", minutesSinceUpdate,
                "requestsThisMinute", requestsThisMinute.get(),
                "rateLimitActive", rateLimitActive,
                "maxRequestsPerMinute", MAX_REQUESTS_PER_MINUTE,
                "memoryCacheTTL", MEMORY_CACHE_TTL_MINUTES + " min",
                "dbCacheTTL", DB_CACHE_TTL_MINUTES + " min",
                "fullUpdateInterval", FULL_UPDATE_INTERVAL_MINUTES + " min"
        );
    }


    private record CachedCrypto(CryptoCurrency crypto, LocalDateTime cachedAt) {}
}