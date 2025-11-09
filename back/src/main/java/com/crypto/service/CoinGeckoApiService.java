// back/src/main/java/com/crypto/service/CoinGeckoApiService.java
package com.crypto.service;

import com.crypto.controller.ApiStatusController;
import com.crypto.dto.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * ✅ COINGECKO API SERVICE - FREE TIER OPTIMIZADO
 *
 * Agora com integração com CoinGeckoRequestQueue:
 * → Enfileira chamadas para respeitar rate limit global.
 */
@Slf4j
@Service
public class CoinGeckoApiService {

    private final WebClient webClient;
    private final CryptoCurrencyRepository cryptoRepository;
    private final RateLimitMetricsService metricsService;
    private final CoinGeckoRequestQueue requestQueue;

    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3";
    private static final long MIN_REQUEST_INTERVAL_MS = 500;
    private final AtomicLong lastRequestTime = new AtomicLong(0);

    private static final List<String> COIN_IDS = List.of(
            "bitcoin", "ethereum", "cardano", "polkadot", "chainlink",
            "solana", "avalanche-2", "matic-network", "litecoin",
            "bitcoin-cash", "ripple", "dogecoin", "binancecoin"
    );

    // ✅ Construtor manual (injeção explícita)
    public CoinGeckoApiService(
            WebClient webClient,
            CryptoCurrencyRepository cryptoRepository,
            RateLimitMetricsService metricsService,
            CoinGeckoRequestQueue requestQueue
    ) {
        this.webClient = webClient;
        this.cryptoRepository = cryptoRepository;
        this.metricsService = metricsService;
        this.requestQueue = requestQueue;
    }

    // ==========================================================
    // ✅ MÉTODO PRINCIPAL - agora usa fila de requisições
    // ==========================================================
    @CircuitBreaker(name = "coingecko", fallbackMethod = "getFallbackPrices")
    @Cacheable(value = "allCryptoPrices", unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getAllPrices() {
        try {
            CompletableFuture<List<CryptoCurrency>> future = requestQueue.enqueue(
                    this::performGetAllPrices,
                    CoinGeckoRequestQueue.RequestPriority.NORMAL
            );
            return future.get(30, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            log.error("⏱️ Timeout ao aguardar resposta da CoinGecko API");
            metricsService.recordFailure();
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ Erro ao enfileirar chamada CoinGecko: {}", e.getMessage(), e);
            metricsService.recordFailure();
            return Collections.emptyList();
        }
    }

    // ==========================================================
    // 🔍 Lógica interna original (antes dentro de getAllPrices)
    // ==========================================================
    private List<CryptoCurrency> performGetAllPrices() {
        try {
            log.info("🔄 Buscando preços via CoinGecko API...");
            long startTime = System.currentTimeMillis();

            waitForRateLimit();

            String ids = String.join(",", COIN_IDS);
            String url = String.format(
                    "%s/coins/markets?vs_currency=usd&ids=%s" +
                            "&order=market_cap_desc" +
                            "&per_page=50&page=1&sparkline=false" +
                            "&price_change_percentage=1h,24h,7d",
                    COINGECKO_API_URL, ids
            );

            List<Map<String, Object>> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.value() == 429, clientResponse -> {
                        log.error("❌ RATE LIMIT 429 atingido!");
                        metricsService.recordRateLimitHit();
                        return Mono.error(new RuntimeException("Rate limit exceeded"));
                    })
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .maxBackoff(Duration.ofSeconds(10))
                            .doBeforeRetry(signal ->
                                    log.warn("⚠️ Retry {} após erro: {}",
                                            signal.totalRetries() + 1,
                                            signal.failure().getMessage())
                            ))
                    .block();

            if (response == null || response.isEmpty()) {
                log.warn("⚠️ CoinGecko retornou lista vazia");
                metricsService.recordFailure();
                return Collections.emptyList();
            }

            List<CryptoCurrency> cryptos = response.stream()
                    .map(this::mapCoinGeckoToCrypto)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(CryptoCurrency::getMarketCap,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            long elapsed = System.currentTimeMillis() - startTime;
            metricsService.recordSuccess();
            ApiStatusController.recordSuccessfulRequest();
            log.info("✅ CoinGecko: {} moedas em {}ms", cryptos.size(), elapsed);

            return cryptos;

        } catch (Exception e) {
            log.error("❌ Erro ao buscar preços: {}", e.getMessage(), e);
            metricsService.recordFailure();

            if (e.getMessage().contains("429") || e.getMessage().contains("Rate limit")) {
                metricsService.recordRateLimitHit();
            }

            return Collections.emptyList();
        }
    }

    // ==========================================================
    // 🔁 Fallback - usado pelo CircuitBreaker
    // ==========================================================
    @SuppressWarnings("unused")
    private List<CryptoCurrency> getFallbackPrices(Exception e) {
        log.warn("⚠️ Circuit breaker aberto, usando fallback local (banco de dados). Erro: {}", e.getMessage());
        try {
            List<CryptoCurrency> cached = cryptoRepository.findAllByOrderByMarketCapDesc();
            if (cached.isEmpty()) {
                log.warn("⚠️ Nenhum dado local encontrado no fallback!");
            } else {
                log.info("✅ Fallback retornou {} criptomoedas do banco local", cached.size());
            }
            return cached;
        } catch (Exception ex) {
            log.error("❌ Erro no fallback: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    // ==========================================================
    // 🧩 Métodos auxiliares
    // ==========================================================
    private CryptoCurrency mapCoinGeckoToCrypto(Map<String, Object> coin) {
        try {
            CryptoCurrency crypto = new CryptoCurrency();

            crypto.setCoinId((String) coin.get("id"));
            crypto.setSymbol((String) coin.get("symbol"));
            crypto.setName((String) coin.get("name"));

            Object currentPrice = coin.get("current_price");
            if (currentPrice instanceof Number)
                crypto.setCurrentPrice(BigDecimal.valueOf(((Number) currentPrice).doubleValue()));

            Object marketCap = coin.get("market_cap");
            if (marketCap instanceof Number)
                crypto.setMarketCap(BigDecimal.valueOf(((Number) marketCap).doubleValue()));

            Object volume = coin.get("total_volume");
            if (volume instanceof Number)
                crypto.setTotalVolume(BigDecimal.valueOf(((Number) volume).doubleValue()));

            Object change1h = coin.get("price_change_percentage_1h_in_currency");
            if (change1h instanceof Number)
                crypto.setPriceChange1h(((Number) change1h).doubleValue());

            Object change24h = coin.get("price_change_percentage_24h");
            if (change24h instanceof Number)
                crypto.setPriceChange24h(((Number) change24h).doubleValue());

            Object change7d = coin.get("price_change_percentage_7d_in_currency");
            if (change7d instanceof Number)
                crypto.setPriceChange7d(((Number) change7d).doubleValue());

            crypto.setLastUpdated(LocalDateTime.now());
            return crypto;

        } catch (Exception e) {
            log.error("❌ Erro ao mapear coin: {}", e.getMessage());
            return null;
        }
    }

    private void waitForRateLimit() {
        long now = System.currentTimeMillis();
        long lastRequest = lastRequestTime.get();
        long elapsed = now - lastRequest;

        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            long waitTime = MIN_REQUEST_INTERVAL_MS - elapsed;
            try {
                log.debug("⏳ Rate limit: aguardando {}ms...", waitTime);
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ Rate limit wait interrompido");
            }
        }

        lastRequestTime.set(System.currentTimeMillis());
    }

    public boolean isAvailable() {
        try {
            String url = COINGECKO_API_URL + "/ping";

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            boolean available = response != null && "pong".equals(response.get("gecko_says"));
            log.info(available ? "✅ CoinGecko API disponível" : "❌ CoinGecko API indisponível");
            return available;

        } catch (Exception e) {
            log.error("❌ CoinGecko health check falhou: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getRateLimitInfo() {
        return Map.of(
                "provider", "CoinGecko",
                "tier", "FREE",
                "rateLimit", "30 req/min",
                "requestInterval", MIN_REQUEST_INTERVAL_MS + "ms",
                "cacheTTL", "30 minutes",
                "supportedCoins", COIN_IDS.size(),
                "coins", COIN_IDS
        );
    }
}
