// back/src/main/java/com/crypto/service/CryptoService.java
package com.crypto.service;

import com.crypto.dto.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * ✅ CRYPTO SERVICE - 100% BINANCE API
 *
 * MUDANÇAS:
 * - ❌ Removido CoinGecko (rate limit problemático)
 * - ✅ Adicionado Binance (grátis, sem rate limit)
 * - ❌ Removido sistema de mock (não precisa mais!)
 * - ❌ Removido rate limiting manual (Binance aguenta)
 * - ✅ Mantido cache de 30min
 * - ✅ Mantido monitoramento multi-usuário
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoService {

    // ✅ INJETAR BINANCE API SERVICE
    private final BinanceApiService binanceApiService;
    private final CryptoCurrencyRepository cryptoRepository;
    private final EmailService emailService;

    @Value("${alert.buy.threshold:-5.0}")
    private double buyThreshold;

    @Value("${alert.sell.threshold:10.0}")
    private double sellThreshold;

    @Value("${alert.check-interval:600000}")
    private long checkIntervalMs;

    @Value("${notification.email.cooldown-minutes:60}")
    private int alertCooldownMinutes;

    // =============================
    // MONITORAMENTO MULTI-USUÁRIO
    // =============================
    private final Map<String, Map<String, Instant>> userAlertCooldown = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> userTasks = new ConcurrentHashMap<>();

    public void startUserMonitoring(String userEmail) {
        if (userTasks.containsKey(userEmail)) {
            log.info("Usuário {} já possui monitoramento ativo", userEmail);
            return;
        }

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                () -> runUserCheck(userEmail),
                60_000, // 1 minuto de delay inicial
                checkIntervalMs,
                TimeUnit.MILLISECONDS
        );

        userTasks.put(userEmail, task);
        log.info("✅ Monitoramento iniciado: {} (intervalo: {}min)",
                userEmail, checkIntervalMs / 60000);
    }

    public void stopUserMonitoring(String userEmail) {
        ScheduledFuture<?> task = userTasks.remove(userEmail);
        if (task != null) {
            task.cancel(true);
            log.info("🛑 Monitoramento parado: {}", userEmail);
        }
        userAlertCooldown.remove(userEmail);
    }

    public void runUserCheck(String userEmail) {
        try {
            log.debug("🔄 Executando check para: {}", userEmail);
            List<CryptoCurrency> cryptos = getCurrentPrices();
            checkAutomaticAlertsForUser(cryptos, userEmail);
        } catch (Exception e) {
            log.error("❌ Erro ao executar monitoramento para {}: {}",
                    userEmail, e.getMessage());
        }
    }

    private void checkAutomaticAlertsForUser(List<CryptoCurrency> cryptos, String userEmail) {
        Map<String, Instant> cooldownMap = userAlertCooldown
                .computeIfAbsent(userEmail, k -> new ConcurrentHashMap<>());
        Instant now = Instant.now();

        for (CryptoCurrency crypto : cryptos) {
            if (crypto.getPriceChange24h() == null) continue;

            double change24h = crypto.getPriceChange24h();
            String coinId = crypto.getCoinId();

            // Alerta de COMPRA (queda)
            if (change24h <= buyThreshold && isCooldownPassed(cooldownMap, coinId, now)) {
                sendBuyAlert(crypto, userEmail);
                cooldownMap.put(coinId, now);
            }

            // Alerta de VENDA (alta)
            if (change24h >= sellThreshold && isCooldownPassed(cooldownMap, coinId, now)) {
                sendSellAlert(crypto, userEmail);
                cooldownMap.put(coinId, now);
            }
        }
    }

    private boolean isCooldownPassed(Map<String, Instant> cooldownMap, String coinId, Instant now) {
        Instant lastAlert = cooldownMap.get(coinId);
        return lastAlert == null ||
                Duration.between(lastAlert, now).toMinutes() >= alertCooldownMinutes;
    }

    private void sendBuyAlert(CryptoCurrency crypto, String userEmail) {
        String subject = String.format("🟢 OPORTUNIDADE DE COMPRA - %s", crypto.getName());
        String message = String.format(
                "Criptomoeda: %s (%s)\nPreço Atual: $%.2f\nVariação 24h: %.2f%%\nThreshold: %.1f%%",
                crypto.getName(), crypto.getSymbol(), crypto.getCurrentPrice(),
                crypto.getPriceChange24h(), buyThreshold
        );
        emailService.sendEmailAsync(userEmail, subject, message);
        log.info("🟢 Alerta COMPRA: {} → {} ({}%)",
                userEmail, crypto.getSymbol(), crypto.getPriceChange24h());
    }

    private void sendSellAlert(CryptoCurrency crypto, String userEmail) {
        String subject = String.format("🔴 ALERTA DE VENDA - %s", crypto.getName());
        String message = String.format(
                "Criptomoeda: %s (%s)\nPreço Atual: $%.2f\nVariação 24h: +%.2f%%\nThreshold: +%.1f%%",
                crypto.getName(), crypto.getSymbol(), crypto.getCurrentPrice(),
                crypto.getPriceChange24h(), sellThreshold
        );
        emailService.sendEmailAsync(userEmail, subject, message);
        log.info("🔴 Alerta VENDA: {} → {} (+{}%)",
                userEmail, crypto.getSymbol(), crypto.getPriceChange24h());
    }

    // =============================
    // ✅ MÉTODOS PRINCIPAIS - BINANCE
    // =============================

    /**
     * ✅ Busca crypto por ID - Cache 30min
     * AGORA: Usando Binance API
     */
    @Cacheable(value = "cryptoPrices", key = "#coinId",
            unless = "#result == null || #result.isEmpty()")
    public Optional<CryptoCurrency> getCryptoByCoinId(String coinId) {
        try {
            log.debug("🔍 Buscando {} via Binance...", coinId);
            return binanceApiService.getPriceByCoinId(coinId);
        } catch (Exception e) {
            log.error("❌ Erro ao buscar {}: {}", coinId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * ✅ Lista completa - Cache 30min
     * AGORA: Usando Binance API
     */
    @Cacheable(value = "allCryptoPrices",
            unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getCurrentPrices() {
        try {
            log.info("🔄 Buscando preços via Binance API...");
            List<CryptoCurrency> cryptos = binanceApiService.getAllPrices();

            if (cryptos != null && !cryptos.isEmpty()) {
                log.info("✅ Binance: {} moedas obtidas", cryptos.size());
                return cryptos;
            }

            log.warn("⚠️ Binance retornou lista vazia");
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Erro ao buscar preços: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * ✅ LAZY LOADING - Busca apenas IDs específicos
     * AGORA: Usando Binance API
     */
    @Cacheable(value = "cryptoPrices", key = "#coinIds",
            unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getPricesByIds(List<String> coinIds) {
        if (coinIds == null || coinIds.isEmpty()) {
            log.warn("⚠️ getPricesByIds: lista vazia");
            return Collections.emptyList();
        }

        try {
            log.info("🔍 Lazy Loading: {} moedas via Binance", coinIds.size());
            return binanceApiService.getPricesByIds(coinIds);
        } catch (Exception e) {
            log.error("❌ Erro Lazy Loading: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * ✅ TOP N - Busca apenas as top moedas
     * AGORA: Usando Binance API
     */
    @Cacheable(value = "topCryptoPrices", key = "#limit",
            unless = "#result == null || #result.isEmpty()")
    public List<CryptoCurrency> getTopCryptoPrices(int limit) {
        try {
            log.info("🔍 Top {} via Binance", limit);
            return binanceApiService.getTopPrices(limit);
        } catch (Exception e) {
            log.error("❌ Erro Top: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // =============================
    // CACHE MANAGEMENT
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

    @CacheEvict(value = {"cryptoPrices", "allCryptoPrices", "topCryptoPrices",
            "binancePrices", "topBinancePrices"}, allEntries = true)
    public void clearCache() {
        log.info("🗑️ Cache limpo (incluindo Binance)");
    }

    @CachePut(value = "cryptoPrices", key = "#crypto.coinId", unless = "#crypto == null")
    public CryptoCurrency updateCache(CryptoCurrency crypto) {
        log.debug("🔄 Cache atualizado: {}", crypto.getCoinId());
        return crypto;
    }

    public void warmUpCache() {
        log.info("🔥 Aquecendo cache com Binance API...");
        try {
            List<CryptoCurrency> cryptos = getCurrentPrices();
            log.info("✅ Cache aquecido: {} criptomoedas", cryptos.size());
        } catch (Exception e) {
            log.error("❌ Erro no warmup: {}", e.getMessage());
        }
    }

    // =============================
    // ✅ NOVOS MÉTODOS UTILITÁRIOS
    // =============================

    /**
     * ✅ Verificar se moeda é suportada pela Binance
     */
    public boolean isCoinSupported(String coinId) {
        return binanceApiService.isCoinSupported(coinId);
    }

    /**
     * ✅ Listar todas as moedas suportadas
     */
    public List<String> getSupportedCoins() {
        return binanceApiService.getSupportedCoins();
    }

    /**
     * ✅ Health check da Binance API
     */
    public boolean isBinanceHealthy() {
        return binanceApiService.isApiHealthy();
    }

    /**
     * ✅ Informações sobre a API atual
     */
    public Map<String, Object> getApiInfo() {
        Map<String, Object> info = binanceApiService.getRateLimitInfo();
        info.put("cacheEnabled", true);
        info.put("cacheTTL", "30 minutes");
        info.put("monitoringEnabled", true);
        return info;
    }
}