// back/src/main/java/com/crypto/service/CoinCapApiService.java
package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ COINCAP API - 100% GRÁTIS, SEM RATE LIMIT RÍGIDO
 *
 * VANTAGENS:
 * - ✅ Sem bloqueio geográfico
 * - ✅ Sem necessidade de API Key
 * - ✅ ~200 req/min (muito mais que CoinGecko)
 * - ✅ Dados agregados de múltiplas exchanges
 * - ✅ Latência baixa (~100-200ms)
 *
 * Documentação: https://docs.coincap.io/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoinCapApiService {

    private final WebClient webClient;

    private static final String COINCAP_API_URL = "https://api.coincap.io/v2";

    /**
     * ✅ Mapeamento CoinGecko ID → CoinCap ID
     *
     * IMPORTANTE: CoinCap usa IDs diferentes
     * Exemplo: "matic-network" → "polygon"
     */
    private static final Map<String, String> COIN_MAP = Map.ofEntries(
            Map.entry("bitcoin", "bitcoin"),
            Map.entry("ethereum", "ethereum"),
            Map.entry("cardano", "cardano"),
            Map.entry("polkadot", "polkadot"),
            Map.entry("chainlink", "chainlink"),
            Map.entry("solana", "solana"),
            Map.entry("avalanche-2", "avalanche"),
            Map.entry("matic-network", "polygon"),        // ✅ MATIC → Polygon
            Map.entry("polygon", "polygon"),
            Map.entry("litecoin", "litecoin"),
            Map.entry("bitcoin-cash", "bitcoin-cash"),
            Map.entry("ripple", "xrp"),
            Map.entry("dogecoin", "dogecoin"),
            Map.entry("binancecoin", "binance-coin")     // ✅ BNB
    );

    /**
     * ✅ MÉTODO PRINCIPAL - Buscar todas as moedas configuradas
     * Cache: 30 minutos
     */
    @Cacheable(value = "coinCapPrices", unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getAllPrices() {
        try {
            log.info("🔄 Buscando preços via CoinCap API...");
            long startTime = System.currentTimeMillis();

            // ✅ Buscar APENAS as moedas que nos interessam (otimizado)
            String ids = String.join(",", COIN_MAP.values());
            String url = COINCAP_API_URL + "/assets?ids=" + ids;

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || !response.containsKey("data")) {
                log.warn("⚠️ CoinCap retornou resposta vazia");
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assets = (List<Map<String, Object>>) response.get("data");

            if (assets == null || assets.isEmpty()) {
                log.warn("⚠️ CoinCap retornou lista vazia");
                return Collections.emptyList();
            }

            List<CryptoCurrency> cryptos = assets.stream()
                    .map(this::mapCoinCapToCrypto)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(CryptoCurrency::getMarketCap,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ CoinCap: {} moedas em {}ms", cryptos.size(), elapsed);

            return cryptos;

        } catch (Exception e) {
            log.error("❌ Erro ao buscar preços no CoinCap: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * ✅ Buscar UMA moeda específica
     */
    @Cacheable(value = "coinCapPrices", key = "#coinId")
    public Optional<CryptoCurrency> getPrice(String coinId) {
        String coinCapId = COIN_MAP.get(coinId.toLowerCase());

        if (coinCapId == null) {
            log.warn("⚠️ Moeda {} não mapeada no CoinCap", coinId);
            return Optional.empty();
        }

        try {
            String url = COINCAP_API_URL + "/assets/" + coinCapId;

            log.debug("🔍 Buscando {} no CoinCap...", coinId);

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> asset = (Map<String, Object>) response.get("data");

                CryptoCurrency crypto = mapCoinCapToCrypto(asset);
                log.debug("✅ {} = ${}", coinId, crypto.getCurrentPrice());

                return Optional.ofNullable(crypto);
            }

        } catch (Exception e) {
            log.error("❌ Erro ao buscar {} no CoinCap: {}", coinId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * ✅ LAZY LOADING - Buscar múltiplas moedas específicas
     */
    @Cacheable(value = "coinCapPrices", key = "#coinIds")
    public List<CryptoCurrency> getPricesByIds(List<String> coinIds) {
        if (coinIds == null || coinIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("🔍 Lazy Loading: {} moedas", coinIds.size());

        // Converter para IDs do CoinCap
        String coincapIds = coinIds.stream()
                .map(id -> COIN_MAP.get(id.toLowerCase()))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));

        if (coincapIds.isEmpty()) {
            log.warn("⚠️ Nenhuma moeda válida para buscar");
            return Collections.emptyList();
        }

        try {
            String url = COINCAP_API_URL + "/assets?ids=" + coincapIds;

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> assets = (List<Map<String, Object>>) response.get("data");

                return assets.stream()
                        .map(this::mapCoinCapToCrypto)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("❌ Erro no Lazy Loading: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * ✅ TOP N moedas por market cap
     */
    @Cacheable(value = "topCoinCapPrices", key = "#limit")
    public List<CryptoCurrency> getTopPrices(int limit) {
        try {
            String url = COINCAP_API_URL + "/assets?limit=" + limit;

            log.info("🔍 Buscando Top {}", limit);

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> assets = (List<Map<String, Object>>) response.get("data");

                return assets.stream()
                        .map(this::mapCoinCapToCrypto)
                        .filter(Objects::nonNull)
                        .limit(limit)
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("❌ Erro ao buscar Top: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * ✅ CONVERTER formato CoinCap → CryptoCurrency
     *
     * Formato CoinCap:
     * {
     *   "id": "bitcoin",
     *   "symbol": "BTC",
     *   "name": "Bitcoin",
     *   "priceUsd": "45000.50",
     *   "changePercent24Hr": "2.5",
     *   "marketCapUsd": "850000000000",
     *   "volumeUsd24Hr": "25000000000"
     * }
     */
    private CryptoCurrency mapCoinCapToCrypto(Map<String, Object> asset) {
        try {
            String coinCapId = (String) asset.get("id");
            String symbol = (String) asset.get("symbol");
            String name = (String) asset.get("name");

            // ✅ Encontrar coinId correspondente (formato CoinGecko)
            String coinId = COIN_MAP.entrySet().stream()
                    .filter(e -> e.getValue().equals(coinCapId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(coinCapId);

            CryptoCurrency crypto = new CryptoCurrency();
            crypto.setCoinId(coinId);
            crypto.setSymbol(symbol);
            crypto.setName(name);

            // ✅ Preço atual (USD)
            String priceUsd = (String) asset.get("priceUsd");
            crypto.setCurrentPrice(new BigDecimal(priceUsd));

            // ✅ Variação 24h (%)
            String changePercent24Hr = (String) asset.get("changePercent24Hr");
            if (changePercent24Hr != null && !changePercent24Hr.isEmpty()) {
                crypto.setPriceChange24h(Double.parseDouble(changePercent24Hr));
            }

            // ✅ Market Cap
            String marketCapUsd = (String) asset.get("marketCapUsd");
            if (marketCapUsd != null && !marketCapUsd.isEmpty()) {
                crypto.setMarketCap(
                        new BigDecimal(marketCapUsd).setScale(0, RoundingMode.HALF_UP)
                );
            }

            // ✅ Volume 24h
            String volumeUsd24Hr = (String) asset.get("volumeUsd24Hr");
            if (volumeUsd24Hr != null && !volumeUsd24Hr.isEmpty()) {
                crypto.setTotalVolume(
                        new BigDecimal(volumeUsd24Hr).setScale(0, RoundingMode.HALF_UP)
                );
            }

            // ✅ NOTA: CoinCap não fornece variações 1h e 7d
            crypto.setPriceChange1h(null);
            crypto.setPriceChange7d(null);

            crypto.setLastUpdated(LocalDateTime.now());

            return crypto;

        } catch (Exception e) {
            log.error("❌ Erro ao mapear asset do CoinCap: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Buscar histórico de preços (para gráficos)
     *
     * Intervalos disponíveis: m1, m5, m15, m30, h1, h2, h6, h12, d1
     */
    @Cacheable(value = "coinCapHistory", key = "#coinId + '_' + #interval")
    public List<Map<String, Object>> getHistory(String coinId, String interval) {
        String coinCapId = COIN_MAP.get(coinId.toLowerCase());

        if (coinCapId == null) {
            log.warn("⚠️ Moeda {} não mapeada", coinId);
            return Collections.emptyList();
        }

        try {
            String url = String.format("%s/assets/%s/history?interval=%s",
                    COINCAP_API_URL, coinCapId, interval);

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> history = (List<Map<String, Object>>) response.get("data");

                log.info("✅ Histórico obtido: {} pontos", history.size());
                return history;
            }

        } catch (Exception e) {
            log.error("❌ Erro ao buscar histórico: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * ✅ Health Check
     */
    public boolean isAvailable() {
        try {
            String url = COINCAP_API_URL + "/assets/bitcoin";

            webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("✅ CoinCap API disponível");
            return true;

        } catch (Exception e) {
            log.error("❌ CoinCap API indisponível: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Verificar se moeda é suportada
     */
    public boolean isCoinSupported(String coinId) {
        return COIN_MAP.containsKey(coinId.toLowerCase());
    }

    /**
     * ✅ Listar moedas suportadas
     */
    public List<String> getSupportedCoins() {
        return new ArrayList<>(COIN_MAP.keySet());
    }

    /**
     * ✅ Informações sobre rate limiting
     */
    public Map<String, Object> getRateLimitInfo() {
        return Map.of(
                "provider", "CoinCap",
                "rateLimit", "~200 req/min",
                "cost", "FREE",
                "requiresApiKey", false,
                "geoBlocking", false,
                "supportedCoins", COIN_MAP.size()
        );
    }
}