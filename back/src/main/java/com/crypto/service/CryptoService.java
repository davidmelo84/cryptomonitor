// back/src/main/java/com/crypto/service/CryptoService.java
package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    private final WebClient webClient;
    private final CryptoCurrencyRepository cryptoRepository;
    private final EmailService emailService;

    @Value("${coingecko.api.url:https://api.coingecko.com/api/v3}")
    private String coinGeckoApiUrl;

    @Value("${crypto.coins:bitcoin,ethereum,cardano,polkadot,chainlink}")
    private String coinsToMonitor;

    @Value("${alert.buy.threshold:-1.0}")
    private double buyThreshold;

    @Value("${alert.sell.threshold:1.0}")
    private double sellThreshold;

    @Value("${alert.check-interval:300000}")
    private long checkIntervalMs;

    @Value("${notification.email.cooldown-minutes:30}")
    private int alertCooldownMinutes;

    // ============================================
    // ✅ RATE LIMITING - CONTROLE DE REQUISIÇÕES
    // ============================================
    private static final int MAX_REQUESTS_PER_MINUTE = 25; // Margem de segurança (CoinGecko = 30/min)
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minuto
    private final Queue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();
    private final Object rateLimitLock = new Object();

    // ✅ FALLBACK: Usar dados mock quando API falhar
    private static final boolean USE_MOCK_ON_RATE_LIMIT = true;
    private static final Map<String, MockCryptoConfig> MOCK_CONFIGS = new ConcurrentHashMap<>();

    static {
        // Configurar mocks para as principais moedas
        MOCK_CONFIGS.put("bitcoin", new MockCryptoConfig("bitcoin", "BTC", "Bitcoin", 45000.0, 40000.0, 50000.0, BigDecimal.valueOf(845_000_000_000L)));
        MOCK_CONFIGS.put("ethereum", new MockCryptoConfig("ethereum", "ETH", "Ethereum", 2500.0, 2000.0, 3000.0, BigDecimal.valueOf(274_000_000_000L)));
        MOCK_CONFIGS.put("cardano", new MockCryptoConfig("cardano", "ADA", "Cardano", 0.50, 0.40, 0.60, BigDecimal.valueOf(20_000_000_000L)));
        MOCK_CONFIGS.put("polkadot", new MockCryptoConfig("polkadot", "DOT", "Polkadot", 7.00, 6.00, 8.00, BigDecimal.valueOf(9_000_000_000L)));
        MOCK_CONFIGS.put("chainlink", new MockCryptoConfig("chainlink", "LINK", "Chainlink", 14.00, 12.00, 16.00, BigDecimal.valueOf(8_000_000_000L)));
        MOCK_CONFIGS.put("solana", new MockCryptoConfig("solana", "SOL", "Solana", 100.0, 90.0, 110.0, BigDecimal.valueOf(42_000_000_000L)));
        MOCK_CONFIGS.put("avalanche-2", new MockCryptoConfig("avalanche-2", "AVAX", "Avalanche", 35.0, 30.0, 40.0, BigDecimal.valueOf(13_000_000_000L)));
        MOCK_CONFIGS.put("matic-network", new MockCryptoConfig("matic-network", "MATIC", "Polygon", 0.80, 0.70, 0.90, BigDecimal.valueOf(7_500_000_000L)));
        MOCK_CONFIGS.put("litecoin", new MockCryptoConfig("litecoin", "LTC", "Litecoin", 75.0, 65.0, 85.0, BigDecimal.valueOf(5_500_000_000L)));
        MOCK_CONFIGS.put("bitcoin-cash", new MockCryptoConfig("bitcoin-cash", "BCH", "Bitcoin Cash", 250.0, 220.0, 280.0, BigDecimal.valueOf(4_900_000_000L)));
    }

    private final Map<String, Map<String, Instant>> userAlertCooldown = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> userTasks = new ConcurrentHashMap<>();

    /**
     * ✅ RATE LIMITING: Aguarda se necessário antes de fazer requisição
     */
    private void waitForRateLimit() {
        synchronized (rateLimitLock) {
            // Remover timestamps antigos (fora da janela de 1 minuto)
            long now = System.currentTimeMillis();
            requestTimestamps.removeIf(timestamp -> (now - timestamp) > RATE_LIMIT_WINDOW_MS);

            // Se atingiu o limite, aguardar
            if (requestTimestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                Long oldestTimestamp = requestTimestamps.peek();
                if (oldestTimestamp != null) {
                    long waitTime = RATE_LIMIT_WINDOW_MS - (now - oldestTimestamp) + 1000; // +1s de margem
                    if (waitTime > 0) {
                        log.warn("⏰ Rate limit atingido. Aguardando {}ms antes da próxima requisição...", waitTime);
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("Aguardo de rate limit interrompido", e);
                        }
                    }
                }
                // Limpar fila após aguardar
                requestTimestamps.clear();
            }

            // Registrar nova requisição
            requestTimestamps.add(System.currentTimeMillis());
        }
    }

    // =============================
    // MONITORAMENTO MULTI-USUÁRIO
    // =============================

    public void startUserMonitoring(String userEmail) {
        if (userTasks.containsKey(userEmail)) {
            log.info("Usuário {} já possui monitoramento ativo", userEmail);
            return;
        }
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                () -> runUserCheck(userEmail),
                30_000, // ⏳ Aguardar 30s antes do primeiro ciclo (dar tempo da API resetar)
                checkIntervalMs,
                TimeUnit.MILLISECONDS
        );
        userTasks.put(userEmail, task);
        log.info("Monitoramento iniciado para usuário: {}", userEmail);
    }

    public void stopUserMonitoring(String userEmail) {
        ScheduledFuture<?> task = userTasks.remove(userEmail);
        if (task != null) {
            task.cancel(true);
            log.info("Monitoramento parado para usuário: {}", userEmail);
        }
        userAlertCooldown.remove(userEmail);
    }

    public void runUserCheck(String userEmail) {
        try {
            List<CryptoCurrency> cryptos = getCurrentPrices();
            checkAutomaticAlertsForUser(cryptos, userEmail);
        } catch (Exception e) {
            log.error("Erro ao executar monitoramento para {}: {}", userEmail, e.getMessage());
        }
    }

    private void checkAutomaticAlertsForUser(List<CryptoCurrency> cryptos, String userEmail) {
        Map<String, Instant> cooldownMap = userAlertCooldown.computeIfAbsent(userEmail, k -> new ConcurrentHashMap<>());
        Instant now = Instant.now();

        for (CryptoCurrency crypto : cryptos) {
            if (crypto.getPriceChange24h() == null) continue;

            double change24h = crypto.getPriceChange24h();
            String coinId = crypto.getCoinId();

            if (change24h <= buyThreshold && isCooldownPassed(cooldownMap, coinId, now)) {
                sendBuyAlert(crypto, userEmail);
                cooldownMap.put(coinId, now);
            }

            if (change24h >= sellThreshold && isCooldownPassed(cooldownMap, coinId, now)) {
                sendSellAlert(crypto, userEmail);
                cooldownMap.put(coinId, now);
            }
        }
    }

    private boolean isCooldownPassed(Map<String, Instant> cooldownMap, String coinId, Instant now) {
        Instant lastAlert = cooldownMap.get(coinId);
        return lastAlert == null || Duration.between(lastAlert, now).toMinutes() >= alertCooldownMinutes;
    }

    private void sendBuyAlert(CryptoCurrency crypto, String userEmail) {
        String subject = String.format("🟢 OPORTUNIDADE DE COMPRA - %s", crypto.getName());
        String message = String.format(
                "Criptomoeda: %s (%s)\nPreço Atual: $%.2f\nVariação 24h: %.2f%%\nThreshold Configurado: %.1f%%",
                crypto.getName(), crypto.getSymbol(), crypto.getCurrentPrice(), crypto.getPriceChange24h(), buyThreshold
        );
        emailService.sendEmail(userEmail, subject, message);
        log.info("🟢 ALERTA DE COMPRA enviado para {}: {} caiu {}%", userEmail, crypto.getName(), crypto.getPriceChange24h());
    }

    private void sendSellAlert(CryptoCurrency crypto, String userEmail) {
        String subject = String.format("🔴 ALERTA DE VENDA - %s", crypto.getName());
        String message = String.format(
                "Criptomoeda: %s (%s)\nPreço Atual: $%.2f\nVariação 24h: +%.2f%%\nThreshold Configurado: +%.1f%%",
                crypto.getName(), crypto.getSymbol(), crypto.getCurrentPrice(), crypto.getPriceChange24h(), sellThreshold
        );
        emailService.sendEmail(userEmail, subject, message);
        log.info("🔴 ALERTA DE VENDA enviado para {}: {} subiu +{}%", userEmail, crypto.getName(), crypto.getPriceChange24h());
    }

    // =============================
    // MÉTODOS COM CACHE E RATE LIMITING
    // =============================

    /**
     * ✅ Busca crypto por ID com rate limiting e fallback para mock
     */
    @Cacheable(value = "cryptoPrices", key = "#coinId", unless = "#result == null || #result.isEmpty()")
    public Optional<CryptoCurrency> getCryptoByCoinId(String coinId) {
        try {
            // ✅ Aguardar rate limit antes de fazer requisição
            waitForRateLimit();

            String url = String.format("%s/coins/markets?vs_currency=usd&ids=%s&price_change_percentage=1h,24h,7d",
                    coinGeckoApiUrl, coinId);

            List<CryptoCurrency> cryptos = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CryptoCurrency>>() {})
                    .block();

            if (cryptos != null && !cryptos.isEmpty()) {
                log.debug("✅ Preço obtido da API: {}", coinId);
                return Optional.of(cryptos.get(0));
            }
        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("⚠️ RATE LIMIT atingido para {}, usando mock como fallback", coinId);
            if (USE_MOCK_ON_RATE_LIMIT && MOCK_CONFIGS.containsKey(coinId.toLowerCase())) {
                return Optional.of(getMockCrypto(coinId.toLowerCase()));
            }
        } catch (Exception e) {
            log.error("Erro ao buscar cotação de {}: {}", coinId, e.getMessage());
            if (USE_MOCK_ON_RATE_LIMIT && MOCK_CONFIGS.containsKey(coinId.toLowerCase())) {
                log.warn("⚠️ Usando mock como fallback para {}", coinId);
                return Optional.of(getMockCrypto(coinId.toLowerCase()));
            }
        }
        return Optional.empty();
    }

    /**
     * ✅ Cache com duração de 5 MINUTOS + Rate Limiting
     */
    @Cacheable(value = "allCryptoPrices", unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getCurrentPrices() {
        try {
            // ✅ Aguardar rate limit antes de fazer requisição
            waitForRateLimit();

            String url = String.format("%s/coins/markets?vs_currency=usd&ids=%s&order=market_cap_desc&per_page=100&page=1&sparkline=false&price_change_percentage=1h,24h,7d",
                    coinGeckoApiUrl, coinsToMonitor);

            List<CryptoCurrency> cryptos = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CryptoCurrency>>() {})
                    .block();

            if (cryptos != null && !cryptos.isEmpty()) {
                log.info("🌐 Preços atualizados da CoinGecko: {} moedas", cryptos.size());
                return cryptos;
            }
        } catch (WebClientResponseException.TooManyRequests e) {
            log.error("❌ RATE LIMIT EXCEDIDO! Usando dados mock como fallback");
            return getMockCryptoList();
        } catch (WebClientResponseException e) {
            log.error("Erro ao buscar cotações da CoinGecko API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar cotações: {}", e.getMessage(), e);
        }

        // ✅ Fallback para mock
        if (USE_MOCK_ON_RATE_LIMIT) {
            log.warn("⚠️ API falhou, retornando lista mock");
            return getMockCryptoList();
        }

        return Collections.emptyList();
    }

    /**
     * ✅ Invalida cache ao salvar crypto
     */
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

    @CacheEvict(value = {"cryptoPrices", "allCryptoPrices"}, allEntries = true)
    public void clearCache() {
        log.info("🗑️ Cache de criptomoedas limpo manualmente");
    }

    @CachePut(value = "cryptoPrices", key = "#crypto.coinId", unless = "#crypto == null")
    public CryptoCurrency updateCache(CryptoCurrency crypto) {
        log.debug("🔄 Cache atualizado para: {}", crypto.getCoinId());
        return crypto;
    }

    public void warmUpCache() {
        log.info("🔥 Aquecendo cache...");
        try {
            List<CryptoCurrency> cryptos = getCurrentPrices();
            log.info("✅ Cache aquecido com {} criptomoedas", cryptos.size());
        } catch (Exception e) {
            log.error("❌ Erro ao aquecer cache: {}", e.getMessage());
        }
    }

    // =============================
    // SISTEMA DE MOCK DINÂMICO
    // =============================

    private static final Random random = new Random();

    private CryptoCurrency getMockCrypto(String coinId) {
        MockCryptoConfig config = MOCK_CONFIGS.get(coinId);
        if (config == null) return null;

        double percentVariation = (random.nextDouble() - 0.5) * 0.02;
        double variation = config.currentPrice * percentVariation;
        config.currentPrice += variation;

        config.currentPrice = Math.max(config.minPrice, Math.min(config.currentPrice, config.maxPrice));

        BigDecimal currentPrice = BigDecimal.valueOf(config.currentPrice);

        CryptoCurrency mock = new CryptoCurrency();
        mock.setCoinId(config.coinId);
        mock.setSymbol(config.symbol);
        mock.setName(config.name);
        mock.setCurrentPrice(currentPrice);
        mock.setPriceChange24h(random.nextDouble() * 6 - 3);
        mock.setPriceChange1h(random.nextDouble() * 2 - 1);
        mock.setPriceChange7d(random.nextDouble() * 20 - 10);
        mock.setMarketCap(config.marketCap);
        mock.setTotalVolume(config.marketCap.divide(BigDecimal.valueOf(30), RoundingMode.HALF_UP));
        mock.setLastUpdated(LocalDateTime.now());

        log.debug("🎮 Mock {} @ ${}", config.symbol, currentPrice);
        return mock;
    }

    private List<CryptoCurrency> getMockCryptoList() {
        List<CryptoCurrency> mockList = new ArrayList<>();
        MOCK_CONFIGS.forEach((coinId, config) -> {
            CryptoCurrency mock = getMockCrypto(coinId);
            if (mock != null) {
                mockList.add(mock);
            }
        });
        log.info("🎮 Retornando {} moedas do sistema mock", mockList.size());
        return mockList;
    }

    public void resetAllMockPrices() {
        MOCK_CONFIGS.forEach((coinId, config) -> config.currentPrice = (config.minPrice + config.maxPrice) / 2);
        log.info("🔄 Todos os preços mock foram resetados");
    }

    public void resetMockPrice(String coinId) {
        MockCryptoConfig config = MOCK_CONFIGS.get(coinId.toLowerCase());
        if (config != null) {
            config.currentPrice = (config.minPrice + config.maxPrice) / 2;
            log.info("🔄 Preço do mock {} resetado para ${}", config.symbol, config.currentPrice);
        }
    }

    public Map<String, String> getAvailableMockCoins() {
        Map<String, String> coins = new HashMap<>();
        MOCK_CONFIGS.forEach((coinId, config) ->
                coins.put(coinId, String.format("%s (%s) - $%.2f", config.name, config.symbol, config.currentPrice))
        );
        return coins;
    }

    private static class MockCryptoConfig {
        String coinId;
        String symbol;
        String name;
        double currentPrice;
        double minPrice;
        double maxPrice;
        BigDecimal marketCap;

        MockCryptoConfig(String coinId, String symbol, String name,
                         double initialPrice, double minPrice, double maxPrice,
                         BigDecimal marketCap) {
            this.coinId = coinId;
            this.symbol = symbol;
            this.name = name;
            this.currentPrice = initialPrice;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.marketCap = marketCap;
        }
    }
}