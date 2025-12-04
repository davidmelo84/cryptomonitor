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

    // -------------------------
    // MEMORY CACHE
    // -------------------------
    private final Map<String, CachedCrypto> memoryCache = new ConcurrentHashMap<>();

    // 🔥 NOVO: índice símbolo → coinId para lookup instantâneo
    private final Map<String, String> symbolToCoinId = new ConcurrentHashMap<>();

    private volatile LocalDateTime lastFullUpdate = null;

    // -------------------------
    // RATE LIMIT CONTROL
    // -------------------------
    private final AtomicInteger requestsThisMinute = new AtomicInteger(0);
    private volatile LocalDateTime minuteStart = LocalDateTime.now();
    private volatile boolean rateLimitActive = false;

    // -------------------------
    // CONFIG
    // -------------------------
    private static final int MEMORY_CACHE_TTL_MINUTES = 30;
    private static final int DB_CACHE_TTL_MINUTES = 120; // 2h
    private static final int MAX_REQUESTS_PER_MINUTE = 25;
    private static final int FULL_UPDATE_INTERVAL_MINUTES = 60; // 1h


    /* =====================================================================================
     *  NOVO — BUSCAR APENAS UMA MOEDA (Lazy Loading)
     * ===================================================================================== */

    public Optional<CryptoCurrency> getCryptoPrice(String coinId) {
        log.debug("🔍 Buscando moeda individual: {}", coinId);

        // 1. Tenta memória
        CachedCrypto cached = memoryCache.get(coinId);
        if (cached != null && !isCachedExpired(cached.cachedAt)) {
            log.debug("✅ Cache HIT (memória) para {}", coinId);
            return Optional.of(cached.crypto);
        }

        // 2. Tenta banco
        Optional<CryptoCurrency> dbResult = repository.findByCoinId(coinId);
        if (dbResult.isPresent() && isDbEntryValid(dbResult.get())) {
            log.debug("📦 Cache HIT (banco) para {}", coinId);
            updateMemoryCache(List.of(dbResult.get()));
            return dbResult;
        }

        // 3. Não pode request → devolve banco mesmo expirado
        if (!canMakeApiRequest()) {
            log.warn("⚠️ Sem request permitido — retornando valor do banco (mesmo expirado)");
            return dbResult;
        }

        // 4. Buscar pela API
        try {
            Optional<CryptoCurrency> apiResult = coinGeckoService.getPrice(coinId);
            if (apiResult.isPresent()) {
                log.info("🌐 API HIT para {}", coinId);

                recordApiRequest();

                CryptoCurrency crypto = apiResult.get();

                updateMemoryCache(List.of(crypto));
                saveToDatabase(List.of(crypto));

                return Optional.of(crypto);
            }
        } catch (Exception e) {
            log.error("❌ Erro API ao buscar {}: {}", coinId, e.getMessage());
        }

        return dbResult;
    }

    private boolean isCachedExpired(LocalDateTime cachedAt) {
        return Duration.between(cachedAt, LocalDateTime.now()).toMinutes() >= MEMORY_CACHE_TTL_MINUTES;
    }

    private boolean isDbEntryValid(CryptoCurrency crypto) {
        if (crypto.getLastUpdated() == null) return false;
        long minutes = Duration.between(crypto.getLastUpdated(), LocalDateTime.now()).toMinutes();
        return minutes < DB_CACHE_TTL_MINUTES;
    }


    /* =====================================================================================
     *  BUSCA COMPLETA (mantida)
     * ===================================================================================== */

    public List<CryptoCurrency> getCurrentPrices() {
        log.debug("🔍 SmartCache: Buscando preços...");

        if (isMemoryCacheValid()) {
            log.info("✅ SmartCache: Usando memória (fresco)");
            return getCachedPrices();
        }

        List<CryptoCurrency> dbPrices = getFromDatabase();
        if (isDbCacheValid(dbPrices)) {
            log.info("📦 SmartCache: Usando banco");
            updateMemoryCache(dbPrices);
            return dbPrices;
        }

        if (canMakeApiRequest()) {
            return fetchFromApi();
        }

        log.warn("⚠️ SmartCache: Using fallback (rate limit)");
        return dbPrices.isEmpty() ? getCachedPrices() : dbPrices;
    }


    private List<CryptoCurrency> fetchFromApi() {
        try {
            log.info("🌐 SmartCache: Buscando do CoinGecko...");

            recordApiRequest();

            List<CryptoCurrency> prices = coinGeckoService.getAllPrices();

            if (prices != null && !prices.isEmpty()) {
                log.info("✅ {} moedas obtidas", prices.size());

                updateMemoryCache(prices);
                saveToDatabase(prices);
                lastFullUpdate = LocalDateTime.now();

                return prices;
            }

        } catch (Exception e) {

            log.error("❌ Erro API: {}", e.getMessage());

            if (e.getMessage().contains("429") || e.getMessage().contains("Rate limit")) {
                activateRateLimitProtection();
            }
        }

        return getFromDatabase();
    }


    /* =====================================================================================
     *  🔥 NOVO — Busca otimizada por símbolo (O(1))
     * ===================================================================================== */

    public Optional<CryptoCurrency> getCryptoBySymbol(String symbol) {
        String normalized = symbol.toUpperCase();

        // Busca em O(1)
        String coinId = symbolToCoinId.get(normalized);

        if (coinId != null) {
            CachedCrypto cached = memoryCache.get(coinId);
            if (cached != null) return Optional.of(cached.crypto);
        }

        // Fallback: varre o cache (caso raro)
        return getCachedPrices().stream()
                .filter(c -> c.getSymbol().equalsIgnoreCase(symbol))
                .findFirst();
    }


    /* =====================================================================================
     *  CACHE & DB
     * ===================================================================================== */

    private boolean isMemoryCacheValid() {
        if (memoryCache.isEmpty()) return false;

        CachedCrypto sample = memoryCache.values().iterator().next();
        long age = Duration.between(sample.cachedAt, LocalDateTime.now()).toMinutes();

        return age < MEMORY_CACHE_TTL_MINUTES;
    }

    private boolean isDbCacheValid(List<CryptoCurrency> list) {
        if (list.isEmpty()) return false;
        CryptoCurrency first = list.get(0);
        return isDbEntryValid(first);
    }

    // 🔥 MODIFICADO: atualiza índice símbolo → coinId
    private void updateMemoryCache(List<CryptoCurrency> cryptos) {
        LocalDateTime now = LocalDateTime.now();

        for (CryptoCurrency c : cryptos) {
            memoryCache.put(c.getCoinId(), new CachedCrypto(c, now));
            symbolToCoinId.put(c.getSymbol().toUpperCase(), c.getCoinId()); // novo índice
        }

        log.debug("💾 Memory cache atualizado: {} entries", memoryCache.size());
    }

    private List<CryptoCurrency> getCachedPrices() {
        return memoryCache.values().stream()
                .map(c -> c.crypto)
                .toList();
    }

    private List<CryptoCurrency> getFromDatabase() {
        try {
            return repository.findAllByOrderByMarketCapDesc();
        } catch (Exception e) {
            log.error("❌ Erro DB: {}", e.getMessage());
            return List.of();
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

            log.info("💾 DB atualizado: {} moedas", cryptos.size());

        } catch (Exception e) {
            log.error("❌ Erro ao salvar no banco: {}", e.getMessage());
        }
    }


    /* =====================================================================================
     *  RATE LIMIT
     * ===================================================================================== */

    private boolean canMakeApiRequest() {
        if (Duration.between(minuteStart, LocalDateTime.now()).toSeconds() >= 60) {
            resetMinuteCounter();
        }

        if (requestsThisMinute.get() >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("⚠️ Limite de requests atingido ({}/{})",
                    requestsThisMinute.get(), MAX_REQUESTS_PER_MINUTE);
            return false;
        }

        if (rateLimitActive) {
            log.warn("🚨 Proteção Rate Limit ATIVA");
            return false;
        }

        if (lastFullUpdate != null) {
            long minutesSinceUpdate = Duration.between(lastFullUpdate, LocalDateTime.now()).toMinutes();
            if (minutesSinceUpdate < FULL_UPDATE_INTERVAL_MINUTES) {
                return false;
            }
        }

        return true;
    }

    private void activateRateLimitProtection() {
        rateLimitActive = true;
        log.error("🚨 RATE LIMIT: ativado por 5 min");

        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000);
                rateLimitActive = false;
                log.info("🔓 Rate limit desativado");
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void resetMinuteCounter() {
        requestsThisMinute.set(0);
        minuteStart = LocalDateTime.now();
    }

    private void recordApiRequest() {
        requestsThisMinute.incrementAndGet();
    }


    /* =====================================================================================
     *  SCHEDULER
     * ===================================================================================== */

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void scheduledUpdate() {
        log.info("⏰ Scheduler executando update...");

        if (canMakeApiRequest()) {
            List<CryptoCurrency> prices = fetchFromApi();
            if (!prices.isEmpty()) {
                log.info("✅ Update automático OK");
            }
        } else {
            log.info("⏭️ Pulando update (proteção ativa)");
        }
    }


    /* =====================================================================================
     *  ADMIN
     * ===================================================================================== */


    public void clearCache() {
        memoryCache.clear();
        symbolToCoinId.clear();   // 🔥 limpa índice também
        lastFullUpdate = null;
        requestsThisMinute.set(0);
        log.info("🗑️ Cache limpo");
    }

    public void forceUpdate() {
        log.warn("⚠️ FORCE UPDATE solicitado");

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


    // Registro interno do cache individual
    private record CachedCrypto(CryptoCurrency crypto, LocalDateTime cachedAt) {}
}
