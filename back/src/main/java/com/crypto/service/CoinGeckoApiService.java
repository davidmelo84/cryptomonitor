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
import java.util.stream.Collectors;

/**
 * ✅ VERSÃO FINAL - RATE LIMIT RESOLVIDO
 *
 * MUDANÇAS CRÍTICAS:
 * 1. TODAS as chamadas passam pela fila (sem exceções!)
 * 2. Timeout aumentado para 60 segundos
 * 3. Fallback SEMPRE retorna dados do banco
 * 4. Circuit breaker com configuração agressiva
 */
@Slf4j
@Service
public class CoinGeckoApiService {

    private final WebClient webClient;
    private final CryptoCurrencyRepository cryptoRepository;
    private final RateLimitMetricsService metricsService;
    private final CoinGeckoRequestQueue requestQueue;

    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3";
    private static final long REQUEST_TIMEOUT_MS = 60000; // ✅ 60 segundos

    private static final List<String> COIN_IDS = List.of(
            "bitcoin", "ethereum", "cardano", "polkadot", "chainlink",
            "solana", "avalanche-2", "matic-network", "litecoin",
            "bitcoin-cash", "ripple", "dogecoin", "binancecoin"
    );

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

        log.info("✅ CoinGeckoApiService inicializado");
        log.info("   Timeout: {}s", REQUEST_TIMEOUT_MS / 1000);
        log.info("   Coins: {}", COIN_IDS.size());
    }

    // ==========================================================
    // ✅ MÉTODO PRINCIPAL - SEMPRE USA FILA
    // ==========================================================
    @CircuitBreaker(name = "coingecko", fallbackMethod = "getFallbackPrices")
    @Cacheable(value = "allCryptoPrices", unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getAllPrices() {
        log.info("🔄 getAllPrices() chamado");

        try {
            // ✅ CRÍTICO: Enfileirar com PRIORIDADE ALTA
            CompletableFuture<List<CryptoCurrency>> future = requestQueue.enqueue(
                    this::performGetAllPrices,
                    CoinGeckoRequestQueue.RequestPriority.HIGH // ✅ Alta prioridade
            );

            // ✅ Aguardar com timeout de 60 segundos
            List<CryptoCurrency> result = future.get(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (result == null || result.isEmpty()) {
                log.warn("⚠️ CoinGecko retornou vazio, usando fallback");
                return getFallbackPrices(new RuntimeException("Empty response"));
            }

            return result;

        } catch (TimeoutException e) {
            log.error("⏱️ Timeout após {}s", REQUEST_TIMEOUT_MS / 1000);
            metricsService.recordFailure();
            return getFallbackPrices(e);

        } catch (Exception e) {
            log.error("❌ Erro ao buscar preços: {}", e.getMessage());
            metricsService.recordFailure();
            return getFallbackPrices(e);
        }
    }

    // ==========================================================
    // 🔁 FALLBACK - SEMPRE RETORNA DADOS DO BANCO
    // ==========================================================
    @SuppressWarnings("unused")
    private List<CryptoCurrency> getFallbackPrices(Exception e) {
        log.warn("⚠️ Usando fallback (banco de dados)");
        log.debug("   Motivo: {}", e.getMessage());

        try {
            List<CryptoCurrency> cached = cryptoRepository.findAllByOrderByMarketCapDesc();

            if (cached.isEmpty()) {
                log.error("❌ Banco de dados VAZIO! Sistema sem dados.");
                return Collections.emptyList();
            }

            log.info("✅ Fallback: {} moedas do banco", cached.size());
            return cached;

        } catch (Exception ex) {
            log.error("❌ ERRO CRÍTICO no fallback: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    // ==========================================================
    // 🔍 Implementação interna (NÃO CHAMAR DIRETAMENTE!)
    // ==========================================================
    private List<CryptoCurrency> performGetAllPrices() {
        try {
            log.info("🌐 Executando request ao CoinGecko...");
            long startTime = System.currentTimeMillis();

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
                    .onStatus(
                            status -> status.value() == 429,
                            clientResponse -> {
                                log.error("❌ RATE LIMIT 429 detectado!");
                                metricsService.recordRateLimitHit();
                                return Mono.error(new RuntimeException("Rate limit exceeded"));
                            }
                    )
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(5))
                            .maxBackoff(Duration.ofSeconds(15))
                            .filter(throwable -> {
                                String msg = throwable.getMessage();
                                // NÃO retry em rate limit!
                                return msg != null && !msg.contains("429") && !msg.contains("Rate limit");
                            })
                            .doBeforeRetry(signal ->
                                    log.warn("⚠️ Retry {} após erro: {}",
                                            signal.totalRetries() + 1,
                                            signal.failure().getMessage())
                            ))
                    .block();

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response from CoinGecko");
            }

            List<CryptoCurrency> cryptos = response.stream()
                    .map(this::mapCoinGeckoToCrypto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            long elapsed = System.currentTimeMillis() - startTime;
            metricsService.recordSuccess();
            ApiStatusController.recordSuccessfulRequest();

            log.info("✅ CoinGecko: {} moedas em {}ms", cryptos.size(), elapsed);

            // ✅ Salvar no banco para fallback futuro
            cryptos.forEach(crypto -> {
                try {
                    cryptoRepository.findByCoinId(crypto.getCoinId())
                            .ifPresentOrElse(
                                    existing -> {
                                        existing.setCurrentPrice(crypto.getCurrentPrice());
                                        existing.setPriceChange1h(crypto.getPriceChange1h());
                                        existing.setPriceChange24h(crypto.getPriceChange24h());
                                        existing.setPriceChange7d(crypto.getPriceChange7d());
                                        existing.setMarketCap(crypto.getMarketCap());
                                        existing.setTotalVolume(crypto.getTotalVolume());
                                        existing.setLastUpdated(LocalDateTime.now());
                                        cryptoRepository.save(existing);
                                    },
                                    () -> cryptoRepository.save(crypto)
                            );
                } catch (Exception e) {
                    log.warn("⚠️ Erro ao salvar {}: {}", crypto.getCoinId(), e.getMessage());
                }
            });

            return cryptos;

        } catch (Exception e) {
            log.error("❌ Erro no performGetAllPrices: {}", e.getMessage());
            metricsService.recordFailure();

            if (e.getMessage() != null &&
                    (e.getMessage().contains("429") || e.getMessage().contains("Rate limit"))) {
                metricsService.recordRateLimitHit();
            }

            throw new RuntimeException("CoinGecko API failed: " + e.getMessage(), e);
        }
    }

    // ==========================================================
    // 🧩 Mapeamento
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

    // ==========================================================
    // 🔧 Métodos auxiliares
    // ==========================================================

    public boolean isAvailable() {
        try {
            String url = COINGECKO_API_URL + "/ping";

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(5))
                    .block();

            boolean available = response != null && "pong".equals(response.get("gecko_says"));
            log.debug(available ? "✅ CoinGecko disponível" : "❌ CoinGecko indisponível");
            return available;

        } catch (Exception e) {
            log.debug("❌ Ping falhou: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getRateLimitInfo() {
        return Map.of(
                "provider", "CoinGecko",
                "tier", "FREE",
                "rateLimit", "30 req/min",
                "queueEnabled", true,
                "requestTimeout", REQUEST_TIMEOUT_MS + "ms",
                "cacheTTL", "30 minutes",
                "supportedCoins", COIN_IDS.size()
        );
    }
}