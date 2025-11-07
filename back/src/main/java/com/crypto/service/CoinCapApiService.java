// back/src/main/java/com/crypto/service/CoinCapApiService.java
package com.crypto.service;

import com.crypto.controller.ApiStatusController;
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
 * ✅ COINCAP API - Alternativa Gratuita e Sem Geo-Block
 *
 * Vantagens:
 * - Grátis e sem rate limit rígido (200 req/min)
 * - Dados agregados de múltiplas exchanges
 * - Sem restrições geográficas
 * - Preços em tempo real
 *
 * Documentação: https://docs.coincap.io/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoinCapApiService {

    private final WebClient webClient;

    private static final String COINCAP_API_URL = "https://api.coincap.io/v2";

    // Mapeamento CoinGecko ID → CoinCap ID
    private static final Map<String, String> COIN_MAP = Map.ofEntries(
            Map.entry("bitcoin", "bitcoin"),
            Map.entry("ethereum", "ethereum"),
            Map.entry("cardano", "cardano"),
            Map.entry("polkadot", "polkadot"),
            Map.entry("chainlink", "chainlink"),
            Map.entry("solana", "solana"),
            Map.entry("avalanche-2", "avalanche"),
            Map.entry("matic-network", "polygon"),
            Map.entry("litecoin", "litecoin"),
            Map.entry("bitcoin-cash", "bitcoin-cash"),
            Map.entry("ripple", "xrp"),
            Map.entry("dogecoin", "dogecoin"),
            Map.entry("binancecoin", "binance-coin")
    );

    /**
     * ✅ Busca lista de criptomoedas do CoinCap
     */
    @Cacheable(value = "coinCapPrices", unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getAllPrices() {
        try {
            log.info("🔄 Buscando preços via CoinCap API...");

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
                    .collect(Collectors.toList());

            ApiStatusController.recordSuccessfulRequest();
            log.info("✅ CoinCap: {} moedas obtidas", cryptos.size());

            return cryptos;

        } catch (Exception e) {
            log.error("❌ Erro ao buscar preços no CoinCap: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * ✅ Busca preço de uma moeda específica
     */
    @Cacheable(value = "coinCapPrices", key = "#coinId")
    public Optional<CryptoCurrency> getPrice(String coinId) {
        String coinCapId = COIN_MAP.get(coinId);
        if (coinCapId == null) {
            log.warn("⚠️ Moeda {} não mapeada no CoinCap", coinId);
            return Optional.empty();
        }

        try {
            String url = COINCAP_API_URL + "/assets/" + coinCapId;

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> asset = (Map<String, Object>) response.get("data");

                ApiStatusController.recordSuccessfulRequest();
                return Optional.ofNullable(mapCoinCapToCrypto(asset));
            }

        } catch (Exception e) {
            log.error("❌ Erro ao buscar {} no CoinCap: {}", coinId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * ✅ Converte formato CoinCap → CryptoCurrency
     */
    private CryptoCurrency mapCoinCapToCrypto(Map<String, Object> asset) {
        try {
            String coinCapId = (String) asset.get("id");
            String symbol = (String) asset.get("symbol");
            String name = (String) asset.get("name");

            // Encontrar coinId correspondente (formato CoinGecko)
            String coinId = COIN_MAP.entrySet().stream()
                    .filter(e -> e.getValue().equals(coinCapId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(coinCapId);

            CryptoCurrency crypto = new CryptoCurrency();
            crypto.setCoinId(coinId);
            crypto.setSymbol(symbol);
            crypto.setName(name);

            // Preço atual (USD)
            String priceUsd = (String) asset.get("priceUsd");
            crypto.setCurrentPrice(new BigDecimal(priceUsd));

            // Variação 24h
            String changePercent24Hr = (String) asset.get("changePercent24Hr");
            if (changePercent24Hr != null && !changePercent24Hr.isEmpty()) {
                crypto.setPriceChange24h(Double.parseDouble(changePercent24Hr));
            }

            // Market Cap
            String marketCapUsd = (String) asset.get("marketCapUsd");
            if (marketCapUsd != null && !marketCapUsd.isEmpty()) {
                crypto.setMarketCap(new BigDecimal(marketCapUsd).setScale(0, RoundingMode.HALF_UP));
            }

            // Volume 24h
            String volumeUsd24Hr = (String) asset.get("volumeUsd24Hr");
            if (volumeUsd24Hr != null && !volumeUsd24Hr.isEmpty()) {
                crypto.setTotalVolume(new BigDecimal(volumeUsd24Hr).setScale(0, RoundingMode.HALF_UP));
            }

            crypto.setLastUpdated(LocalDateTime.now());

            return crypto;

        } catch (Exception e) {
            log.error("❌ Erro ao mapear asset do CoinCap: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Busca histórico de preços (para gráficos)
     */
    @Cacheable(value = "coinCapHistory", key = "#coinId + '_' + #interval")
    public List<Map<String, Object>> getHistory(String coinId, String interval) {
        String coinCapId = COIN_MAP.get(coinId);
        if (coinCapId == null) {
            log.warn("⚠️ Moeda {} não mapeada", coinId);
            return Collections.emptyList();
        }

        try {
            // interval: m1, m5, m15, m30, h1, h2, h6, h12, d1
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
     * ✅ Verifica se API está disponível
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
}