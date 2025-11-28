package com.crypto.service;

import com.crypto.model.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private final SmartCacheService smartCache;
    private final CoinGeckoApiService coinGeckoService;
    private final CryptoCurrencyRepository cryptoRepository;


    public List<CryptoCurrency> getCurrentPrices() {
        return smartCache.getCurrentPrices();
    }


    public Optional<CryptoCurrency> getCryptoByCoinId(String coinId) {
        try {
            log.debug("🔍 Buscando: {}", coinId);

            return getCurrentPrices().stream()
                    .filter(c -> c.getCoinId().equalsIgnoreCase(coinId))
                    .findFirst();

        } catch (Exception e) {
            log.error("❌ Erro ao buscar {}: {}", coinId, e.getMessage());

            return cryptoRepository.findByCoinId(coinId);
        }
    }


    public List<CryptoCurrency> getPricesByIds(List<String> coinIds) {
        if (coinIds == null || coinIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("🔍 Lazy Loading: {} moedas", coinIds.size());

        return getCurrentPrices().stream()
                .filter(c -> coinIds.contains(c.getCoinId()))
                .toList();
    }


    public List<CryptoCurrency> getTopCryptoPrices(int limit) {
        return getCurrentPrices().stream()
                .limit(limit)
                .toList();
    }


    public List<Map<String, Object>> getHistory(String coinId, int days) {
        try {
            List<? extends Map<String, ? extends Number>> rawHistory =
                    coinGeckoService.getHistory(coinId, days);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, ? extends Number> entry : rawHistory) {
                result.add(new HashMap<>(entry));
            }

            return result;
        } catch (Exception e) {
            log.error("❌ Erro ao buscar histórico de {}: {}", coinId, e.getMessage());
            return Collections.emptyList();
        }
    }


    public List<CryptoCurrency> getAllSavedCryptos() {
        return cryptoRepository.findAllByOrderByMarketCapDesc();
    }

    public Optional<CryptoCurrency> getSavedCryptoByCoinId(String coinId) {
        return cryptoRepository.findByCoinId(coinId);
    }


    public void clearCache() {
        smartCache.clearCache();
        log.info("🗑️ Cache limpo manualmente");
    }


    public void forceUpdate() {
        log.warn("⚠️ FORCE UPDATE solicitado — atualizando todas as fontes...");
        smartCache.forceUpdate();
    }


    public void warmUpCache() {
        log.info("🔥 Aquecendo cache...");
        try {
            List<CryptoCurrency> cryptos = getCurrentPrices();
            log.info("✅ Cache aquecido: {} criptomoedas", cryptos.size());
        } catch (Exception e) {
            log.error("❌ Erro ao aquecer cache: {}", e.getMessage());
        }
    }


    public Map<String, Object> getApiStatus() {
        boolean coinGeckoAvailable = coinGeckoService.isAvailable();
        Map<String, Object> cacheStats = smartCache.getStats();

        Map<String, Object> status = new HashMap<>();
        status.put("provider", "CoinGecko");
        status.put("status", coinGeckoAvailable ? "OPERATIONAL" : "DOWN");
        status.put("tier", "FREE");
        status.put("rateLimit", "30 req/min");
        status.put("smartCache", cacheStats);
        status.put("effectiveRequests", "~2 req/hour (≈98% reduction)");
        status.put("timestamp", System.currentTimeMillis());

        return status;
    }
}
