package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ✅ CRYPTO SERVICE - VERSÃO OTIMIZADA COM SMART CACHE
 *
 * 🚀 CARACTERÍSTICAS:
 * - Cache inteligente via SmartCacheService
 * - 3 camadas: Memória (30min), Banco (2h), CoinGecko (fallback)
 * - Sem uso direto de Caffeine aqui (evita conflitos)
 * - Sem schedulers locais — agendamento centralizado no SmartCache
 * - Reduz drasticamente o rate limit: ~2 req/hora (≈98% menos)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private final SmartCacheService smartCache;
    private final CoinGeckoApiService coinGeckoService;
    private final CryptoCurrencyRepository cryptoRepository;

    // ======================================
    // 🔹 MÉTODOS PRINCIPAIS
    // ======================================

    /**
     * ✅ Buscar todas as criptomoedas
     *
     * Usa SmartCache que gerencia:
     * - Memória (30min)
     * - Banco (2h)
     * - CoinGecko (fallback)
     */
    public List<CryptoCurrency> getCurrentPrices() {
        return smartCache.getCurrentPrices();
    }

    /**
     * ✅ Buscar uma moeda específica
     */
    public Optional<CryptoCurrency> getCryptoByCoinId(String coinId) {
        try {
            log.debug("🔍 Buscando: {}", coinId);

            // Busca na lista cacheada (evita requisições externas)
            return getCurrentPrices().stream()
                    .filter(c -> c.getCoinId().equalsIgnoreCase(coinId))
                    .findFirst();

        } catch (Exception e) {
            log.error("❌ Erro ao buscar {}: {}", coinId, e.getMessage());

            // Fallback: banco de dados
            return cryptoRepository.findByCoinId(coinId);
        }
    }

    /**
     * ✅ Buscar múltiplas moedas (lazy loading)
     */
    public List<CryptoCurrency> getPricesByIds(List<String> coinIds) {
        if (coinIds == null || coinIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("🔍 Lazy Loading: {} moedas", coinIds.size());

        // Filtra da lista já carregada em cache
        return getCurrentPrices().stream()
                .filter(c -> coinIds.contains(c.getCoinId()))
                .toList();
    }

    /**
     * ✅ Buscar Top N moedas
     */
    public List<CryptoCurrency> getTopCryptoPrices(int limit) {
        return getCurrentPrices().stream()
                .limit(limit)
                .toList();
    }

    /**
     * ✅ Histórico de preços (para gráficos)
     */
    public List<Map<String, Object>> getHistory(String coinId, int days) {
        try {
            // Endpoint leve — busca direto da CoinGecko
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

    // ======================================
    // 💾 BANCO DE DADOS
    // ======================================

    public List<CryptoCurrency> getAllSavedCryptos() {
        return cryptoRepository.findAllByOrderByMarketCapDesc();
    }

    public Optional<CryptoCurrency> getSavedCryptoByCoinId(String coinId) {
        return cryptoRepository.findByCoinId(coinId);
    }

    // ======================================
    // ⚙️ CACHE MANAGEMENT
    // ======================================

    /**
     * ✅ Limpa todos os caches
     */
    public void clearCache() {
        smartCache.clearCache();
        log.info("🗑️ Cache limpo manualmente");
    }

    /**
     * ✅ Força atualização completa (CoinGecko → Banco → Cache)
     */
    public void forceUpdate() {
        log.warn("⚠️ FORCE UPDATE solicitado — atualizando todas as fontes...");
        smartCache.forceUpdate();
    }

    /**
     * ✅ Pré-carrega o cache na inicialização
     */
    public void warmUpCache() {
        log.info("🔥 Aquecendo cache...");
        try {
            List<CryptoCurrency> cryptos = getCurrentPrices();
            log.info("✅ Cache aquecido: {} criptomoedas", cryptos.size());
        } catch (Exception e) {
            log.error("❌ Erro ao aquecer cache: {}", e.getMessage());
        }
    }

    // ======================================
    // 🩺 HEALTH CHECK / STATUS
    // ======================================

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
