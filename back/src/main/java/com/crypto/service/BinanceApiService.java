// back/src/main/java/com/crypto/service/BinanceApiService.java
package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ BINANCE API SERVICE - 100% GRÁTIS, SEM RATE LIMIT
 *
 * NÃO PRECISA:
 * - ❌ API Key
 * - ❌ Conta na Binance
 * - ❌ Pagamento
 *
 * VANTAGENS:
 * - ✅ ~1200 requisições/minuto (vs 30 CoinGecko)
 * - ✅ Latência menor (~100ms)
 * - ✅ Dados diretos da maior exchange
 *
 * Docs: https://binance-docs.github.io/apidocs/spot/en/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceApiService {

    private final WebClient webClient;

    private static final String BINANCE_API_URL = "https://api.binance.com/api/v3";

    /**
     * Mapeamento: CoinGecko coinId → Binance symbol
     * Exemplo: "bitcoin" → "BTCUSDT"
     */
    private static final Map<String, String> COINGECKO_TO_BINANCE = Map.ofEntries(
            Map.entry("bitcoin", "BTCUSDT"),
            Map.entry("ethereum", "ETHUSDT"),
            Map.entry("cardano", "ADAUSDT"),
            Map.entry("polkadot", "DOTUSDT"),
            Map.entry("chainlink", "LINKUSDT"),
            Map.entry("solana", "SOLUSDT"),
            Map.entry("avalanche-2", "AVAXUSDT"),
            Map.entry("matic-network", "MATICUSDT"),
            Map.entry("polygon", "MATICUSDT"),
            Map.entry("litecoin", "LTCUSDT"),
            Map.entry("bitcoin-cash", "BCHUSDT"),
            Map.entry("ripple", "XRPUSDT"),
            Map.entry("dogecoin", "DOGEUSDT"),
            Map.entry("binancecoin", "BNBUSDT")
    );

    /**
     * Nomes legíveis das moedas
     */
    private static final Map<String, String> COIN_NAMES = Map.ofEntries(
            Map.entry("BTCUSDT", "Bitcoin"),
            Map.entry("ETHUSDT", "Ethereum"),
            Map.entry("ADAUSDT", "Cardano"),
            Map.entry("DOTUSDT", "Polkadot"),
            Map.entry("LINKUSDT", "Chainlink"),
            Map.entry("SOLUSDT", "Solana"),
            Map.entry("AVAXUSDT", "Avalanche"),
            Map.entry("MATICUSDT", "Polygon"),
            Map.entry("LTCUSDT", "Litecoin"),
            Map.entry("BCHUSDT", "Bitcoin Cash"),
            Map.entry("XRPUSDT", "Ripple"),
            Map.entry("DOGEUSDT", "Dogecoin"),
            Map.entry("BNBUSDT", "BNB")
    );

    /**
     * ✅ MÉTODO PRINCIPAL - Buscar todas as moedas
     * Cache: 30 minutos
     */
    @Cacheable(value = "binancePrices", unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getAllPrices() {
        try {
            log.info("🔄 Buscando preços da Binance API...");
            long startTime = System.currentTimeMillis();

            String url = BINANCE_API_URL + "/ticker/24hr";

            List<Map<String, Object>> tickers = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            if (tickers == null || tickers.isEmpty()) {
                log.warn("⚠️ Binance retornou lista vazia");
                return Collections.emptyList();
            }

            // Filtrar apenas os símbolos que nos interessam
            List<CryptoCurrency> cryptos = tickers.stream()
                    .filter(ticker -> {
                        String symbol = (String) ticker.get("symbol");
                        return symbol != null && COINGECKO_TO_BINANCE.containsValue(symbol);
                    })
                    .map(this::mapToCryptoCurrency)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(CryptoCurrency::getMarketCap,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ Binance: {} moedas em {}ms", cryptos.size(), elapsed);

            return cryptos;

        } catch (Exception e) {
            log.error("❌ Erro ao buscar preços na Binance: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * ✅ Buscar preço de UMA moeda específica
     */
    @Cacheable(value = "binancePrices", key = "#coinId")
    public Optional<CryptoCurrency> getPriceByCoinId(String coinId) {
        String binanceSymbol = COINGECKO_TO_BINANCE.get(coinId.toLowerCase());

        if (binanceSymbol == null) {
            log.warn("⚠️ CoinId '{}' não mapeado para Binance", coinId);
            return Optional.empty();
        }

        try {
            String url = BINANCE_API_URL + "/ticker/24hr?symbol=" + binanceSymbol;

            log.debug("🔍 Buscando {} na Binance...", coinId);

            Map<String, Object> ticker = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (ticker != null) {
                CryptoCurrency crypto = mapToCryptoCurrency(ticker);
                log.debug("✅ {} = ${}", coinId, crypto.getCurrentPrice());
                return Optional.ofNullable(crypto);
            }

        } catch (Exception e) {
            log.error("❌ Erro ao buscar {} na Binance: {}", coinId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * ✅ Buscar múltiplas moedas específicas (LAZY LOADING)
     */
    @Cacheable(value = "binancePrices", key = "#coinIds")
    public List<CryptoCurrency> getPricesByIds(List<String> coinIds) {
        if (coinIds == null || coinIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("🔍 Lazy Loading: {} moedas", coinIds.size());

        return coinIds.stream()
                .map(this::getPriceByCoinId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Top N moedas por market cap
     */
    @Cacheable(value = "topBinancePrices", key = "#limit")
    public List<CryptoCurrency> getTopPrices(int limit) {
        List<CryptoCurrency> allPrices = getAllPrices();

        return allPrices.stream()
                .sorted(Comparator.comparing(CryptoCurrency::getMarketCap,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * ✅ CONVERTER formato Binance → CryptoCurrency
     */
    private CryptoCurrency mapToCryptoCurrency(Map<String, Object> ticker) {
        try {
            String binanceSymbol = (String) ticker.get("symbol");
            String baseSymbol = binanceSymbol.replace("USDT", "");

            // Encontrar coinId correspondente
            String coinId = COINGECKO_TO_BINANCE.entrySet().stream()
                    .filter(e -> e.getValue().equals(binanceSymbol))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(baseSymbol.toLowerCase());

            CryptoCurrency crypto = new CryptoCurrency();

            // Identificação
            crypto.setCoinId(coinId);
            crypto.setSymbol(baseSymbol);
            crypto.setName(COIN_NAMES.getOrDefault(binanceSymbol, baseSymbol));

            // Preço atual
            String lastPrice = (String) ticker.get("lastPrice");
            crypto.setCurrentPrice(new BigDecimal(lastPrice));

            // Variação 24h (%)
            String priceChangePercent = (String) ticker.get("priceChangePercent");
            crypto.setPriceChange24h(Double.parseDouble(priceChangePercent));

            // Volume 24h (em USDT)
            String quoteVolume = (String) ticker.get("quoteVolume");
            crypto.setTotalVolume(new BigDecimal(quoteVolume));

            // Market Cap (estimado: volume * 50)
            BigDecimal estimatedMarketCap = crypto.getTotalVolume()
                    .multiply(BigDecimal.valueOf(50));
            crypto.setMarketCap(estimatedMarketCap);

            // Variações extras (Binance não fornece 1h e 7d no endpoint 24hr)
            crypto.setPriceChange1h(null);
            crypto.setPriceChange7d(null);

            crypto.setLastUpdated(LocalDateTime.now());

            return crypto;

        } catch (Exception e) {
            log.error("❌ Erro ao mapear ticker: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Verificar se moeda é suportada
     */
    public boolean isCoinSupported(String coinId) {
        return COINGECKO_TO_BINANCE.containsKey(coinId.toLowerCase());
    }

    /**
     * ✅ Listar todas as moedas suportadas
     */
    public List<String> getSupportedCoins() {
        return new ArrayList<>(COINGECKO_TO_BINANCE.keySet());
    }

    /**
     * ✅ Health check da Binance API
     */
    public boolean isApiHealthy() {
        try {
            String url = BINANCE_API_URL + "/ping";

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return response != null;

        } catch (Exception e) {
            log.error("❌ Binance health check falhou: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Obter informações sobre rate limiting
     */
    public Map<String, Object> getRateLimitInfo() {
        return Map.of(
                "provider", "Binance",
                "rateLimit", "~1200 req/min",
                "cost", "FREE",
                "requiresApiKey", false,
                "supportedCoins", COINGECKO_TO_BINANCE.size()
        );
    }
}