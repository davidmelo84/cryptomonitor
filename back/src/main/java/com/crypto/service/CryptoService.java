// back/src/main/java/com/crypto/service/CryptoService.java
package com.crypto.service;

import com.crypto.controller.ApiStatusController;
import com.crypto.dto.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ✅ CRYPTO SERVICE - ESTRATÉGIA MULTI-API
 *
 * PRIORIDADE:
 * 1️⃣ CoinCap (principal - sem rate limit)
 * 2️⃣ Mock (fallback se tudo falhar)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private final CoinCapApiService coinCapService;
    private final CryptoCurrencyRepository cryptoRepository;
    private final EmailService emailService;

    // ✅ USAR COINCAP COMO FONTE PRINCIPAL
    @Cacheable(value = "allCryptoPrices", unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getCurrentPrices() {
        log.info("🔄 Buscando preços de criptomoedas...");

        try {
            // ✅ 1️⃣ CoinCap (PRINCIPAL)
            List<CryptoCurrency> prices = coinCapService.getAllPrices();

            if (prices != null && !prices.isEmpty()) {
                ApiStatusController.recordSuccessfulRequest();
                log.info("✅ CoinCap: {} moedas obtidas", prices.size());
                return prices;
            }

        } catch (Exception e) {
            log.error("❌ Erro no CoinCap: {}", e.getMessage());
        }

        // ✅ 2️⃣ Fallback: Dados do Banco (se houver)
        try {
            List<CryptoCurrency> savedPrices = cryptoRepository.findAllByOrderByMarketCapDesc();

            if (!savedPrices.isEmpty()) {
                log.warn("⚠️ Usando dados salvos do banco ({} moedas)", savedPrices.size());
                return savedPrices;
            }

        } catch (Exception e) {
            log.error("❌ Erro ao buscar do banco: {}", e.getMessage());
        }

        log.error("❌ TODAS AS FONTES FALHARAM!");
        return Collections.emptyList();
    }

    /**
     * ✅ Buscar UMA moeda específica
     */
    @Cacheable(value = "cryptoPrices", key = "#coinId", unless = "#result == null || #result.isEmpty()")
    public Optional<CryptoCurrency> getCryptoByCoinId(String coinId) {
        try {
            log.debug("🔍 Buscando: {}", coinId);

            Optional<CryptoCurrency> crypto = coinCapService.getPrice(coinId);

            if (crypto.isPresent()) {
                ApiStatusController.recordSuccessfulRequest();
                log.debug("✅ {} encontrado", coinId);
                return crypto;
            }

        } catch (Exception e) {
            log.error("❌ Erro ao buscar {}: {}", coinId, e.getMessage());
        }

        // Fallback: banco
        return cryptoRepository.findByCoinId(coinId);
    }

    /**
     * ✅ LAZY LOADING - Buscar múltiplas moedas
     */
    @Cacheable(value = "cryptoPrices", key = "#coinIds", unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getPricesByIds(List<String> coinIds) {
        if (coinIds == null || coinIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            log.info("🔍 Lazy Loading: {} moedas", coinIds.size());
            return coinCapService.getPricesByIds(coinIds);

        } catch (Exception e) {
            log.error("❌ Erro no Lazy Loading: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * ✅ TOP N moedas
     */
    @Cacheable(value = "topCryptoPrices", key = "#limit", unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getTopCryptoPrices(int limit) {
        try {
            log.info("🔍 Buscando Top {}", limit);
            return coinCapService.getTopPrices(limit);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar Top: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // =============================
    // BANCO DE DADOS
    // =============================

    @Caching(evict = {
            @CacheEvict(value = "cryptoPrices", key = "#crypto.coinId"),
            @CacheEvict(value = "allCryptoPrices", allEntries = true)
    })
    public CryptoCurrency saveCrypto(CryptoCurrency crypto) {
        return cryptoRepository.findByCoinId(crypto.getCoinId())
                .map(existing -> {
                    existing.setCurrentPrice(crypto.getCurrentPrice());
                    existing.setPriceChange1h(crypto.getPriceChange1h());
                    existing.setPriceChange24h(crypto.getPriceChange24h());
                    existing.setPriceChange7d(crypto.getPriceChange7d());
                    existing.setMarketCap(crypto.getMarketCap());
                    existing.setTotalVolume(crypto.getTotalVolume());
                    return cryptoRepository.save(existing);
                })
                .orElseGet(() -> cryptoRepository.save(crypto));
    }

    public List<CryptoCurrency> getAllSavedCryptos() {
        return cryptoRepository.findAllByOrderByMarketCapDesc();
    }

    public Optional<CryptoCurrency> getSavedCryptoByCoinId(String coinId) {
        return cryptoRepository.findByCoinId(coinId);
    }

    // =============================
    // CACHE MANAGEMENT
    // =============================

    @CacheEvict(value = {"cryptoPrices", "allCryptoPrices", "topCryptoPrices", "coinCapPrices"}, allEntries = true)
    public void clearCache() {
        log.info("🗑️ Cache limpo");
    }

    @CachePut(value = "cryptoPrices", key = "#crypto.coinId", unless = "#crypto == null")
    public CryptoCurrency updateCache(CryptoCurrency crypto) {
        log.debug("🔄 Cache atualizado: {}", crypto.getCoinId());
        return crypto;
    }

    public void warmUpCache() {
        log.info("🔥 Aquecendo cache...");
        try {
            List<CryptoCurrency> cryptos = getCurrentPrices();
            log.info("✅ Cache aquecido: {} criptomoedas", cryptos.size());
        } catch (Exception e) {
            log.error("❌ Erro no warmup: {}", e.getMessage());
        }
    }

    // =============================
    // HEALTH CHECK
    // =============================

    public Map<String, Object> getApiStatus() {
        boolean coinCapAvailable = coinCapService.isAvailable();

        return Map.of(
                "provider", "CoinCap",
                "status", coinCapAvailable ? "OPERATIONAL" : "DOWN",
                "rateLimit", "~200 req/min",
                "cost", "FREE",
                "geoBlocking", false,
                "timestamp", System.currentTimeMillis()
        );
    }
}