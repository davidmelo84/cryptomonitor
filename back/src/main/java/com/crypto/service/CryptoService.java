package com.crypto.service;


import com.crypto.dto.CryptoCurrency;
import com.crypto.repository.CryptoCurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.time.Duration;


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

    @Value("${alert.check-interval:300000}") // 5 minutos padrão
    private long checkIntervalMs;

    @Value("${notification.email.cooldown-minutes:30}")
    private int alertCooldownMinutes;

    // Mantém o estado de cooldown por usuário e coin
    private final Map<String, Map<String, Instant>> userAlertCooldown = new ConcurrentHashMap<>();

    // Pool de threads para monitoramento
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private final Map<String, ScheduledFuture<?>> userTasks = new ConcurrentHashMap<>();

    // ===========================================
    // MÉTODOS PÚBLICOS PARA FRONTEND
    // ===========================================

    /**
     * Inicia monitoramento periódico para um usuário específico
     */
    public void startUserMonitoring(String userEmail) {
        if (userTasks.containsKey(userEmail)) {
            log.info("Usuário {} já possui monitoramento ativo", userEmail);
            return;
        }

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                () -> runUserCheck(userEmail),
                0,
                checkIntervalMs,
                TimeUnit.MILLISECONDS
        );

        userTasks.put(userEmail, task);
        log.info("Monitoramento iniciado para usuário: {}", userEmail);
    }

    /**
     * Para monitoramento do usuário
     */
    public void stopUserMonitoring(String userEmail) {
        ScheduledFuture<?> task = userTasks.remove(userEmail);
        if (task != null) {
            task.cancel(true);
            log.info("Monitoramento parado para usuário: {}", userEmail);
        }
        userAlertCooldown.remove(userEmail);
    }

    /**
     * Executa verificação de alertas para um usuário específico
     */
    public void runUserCheck(String userEmail) {
        try {
            List<CryptoCurrency> cryptos = getCurrentPrices();
            checkAutomaticAlertsForUser(cryptos, userEmail);
        } catch (Exception e) {
            log.error("Erro ao executar monitoramento para {}: {}", userEmail, e.getMessage());
        }
    }

    // ===========================================
    // ALERTAS MULTI-USUÁRIO
    // ===========================================

    private void checkAutomaticAlertsForUser(List<CryptoCurrency> cryptos, String userEmail) {
        Map<String, Instant> cooldownMap = userAlertCooldown.computeIfAbsent(userEmail, k -> new ConcurrentHashMap<>());
        Instant now = Instant.now();

        for (CryptoCurrency crypto : cryptos) {
            if (crypto.getPriceChange24h() == null) continue;

            double change24h = crypto.getPriceChange24h();
            String coinId = crypto.getCoinId();

            // COMPRA
            if (change24h <= buyThreshold && isCooldownPassed(cooldownMap, coinId, now)) {
                sendBuyAlert(crypto, userEmail);
                cooldownMap.put(coinId, now);
            }

            // VENDA
            if (change24h >= sellThreshold && isCooldownPassed(cooldownMap, coinId, now)) {
                sendSellAlert(crypto, userEmail);
                cooldownMap.put(coinId, now);
            }
        }
    }

    private boolean isCooldownPassed(Map<String, Instant> cooldownMap, String coinId, Instant now) {
        Instant lastAlert = cooldownMap.get(coinId);
        if (lastAlert == null) return true;

        long minutesPassed = Duration.between(lastAlert, now).toMinutes();
        return minutesPassed >= alertCooldownMinutes;
    }

    private void sendBuyAlert(CryptoCurrency crypto, String userEmail) {
        String subject = String.format("🟢 OPORTUNIDADE DE COMPRA - %s", crypto.getName());
        String message = String.format(
                "Alerta de Compra - Crypto Monitor\n\n" +
                        "🟢 OPORTUNIDADE DE COMPRA DETECTADA!\n\n" +
                        "Criptomoeda: %s (%s)\n" +
                        "Preço Atual: $%.2f\n" +
                        "Variação 24h: %.2f%%\n" +
                        "Threshold Configurado: %.1f%%\n\n" +
                        "Esta criptomoeda caiu além do seu limite configurado.\n" +
                        "Considere esta oportunidade de compra!\n\n" +
                        "---\n" +
                        "Crypto Monitor - Sistema Automático de Alertas",
                crypto.getName(),
                crypto.getSymbol().toUpperCase(),
                crypto.getCurrentPrice(),
                crypto.getPriceChange24h(),
                buyThreshold
        );
        emailService.sendEmail(userEmail, subject, message);
        log.info("🟢 ALERTA DE COMPRA enviado para {}: {} caiu {}%", userEmail, crypto.getName(), crypto.getPriceChange24h());
    }

    private void sendSellAlert(CryptoCurrency crypto, String userEmail) {
        String subject = String.format("🔴 ALERTA DE VENDA - %s", crypto.getName());
        String message = String.format(
                "Alerta de Venda - Crypto Monitor\n\n" +
                        "🔴 ALERTA DE VENDA DISPARADO!\n\n" +
                        "Criptomoeda: %s (%s)\n" +
                        "Preço Atual: $%.2f\n" +
                        "Variação 24h: +%.2f%%\n" +
                        "Threshold Configurado: +%.1f%%\n\n" +
                        "Esta criptomoeda subiu além do seu limite configurado.\n" +
                        "Considere realizar lucros!\n\n" +
                        "---\n" +
                        "Crypto Monitor - Sistema Automático de Alertas",
                crypto.getName(),
                crypto.getSymbol().toUpperCase(),
                crypto.getCurrentPrice(),
                crypto.getPriceChange24h(),
                sellThreshold
        );
        emailService.sendEmail(userEmail, subject, message);
        log.info("🔴 ALERTA DE VENDA enviado para {}: {} subiu +{}%", userEmail, crypto.getName(), crypto.getPriceChange24h());
    }

    // ===========================================
    // MÉTODOS EXISTENTES PARA INTEGRAÇÃO COM BANCO
    // ===========================================

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public List<CryptoCurrency> getCurrentPrices() {
        try {
            String url = String.format("%s/coins/markets?vs_currency=usd&ids=%s" +
                            "&order=market_cap_desc&per_page=100&page=1&sparkline=false" +
                            "&price_change_percentage=1h,24h,7d",
                    coinGeckoApiUrl, coinsToMonitor);

            log.info("Buscando cotações em: {}", url);

            List<CryptoCurrency> cryptos = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CryptoCurrency>>() {})
                    .block();

            if (cryptos == null) return List.of();
            return cryptos;

        } catch (WebClientResponseException e) {
            log.error("Erro ao buscar cotações da CoinGecko API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao conectar com CoinGecko API", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar cotações: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao buscar cotações", e);
        }
    }

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

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Optional<CryptoCurrency> getCryptoByCoinId(String coinId) {
        try {
            String url = String.format("%s/coins/markets?vs_currency=usd&ids=%s&price_change_percentage=1h,24h,7d", coinGeckoApiUrl, coinId);

            List<CryptoCurrency> cryptos = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CryptoCurrency>>() {})
                    .block();

            return cryptos != null && !cryptos.isEmpty() ? Optional.of(cryptos.get(0)) : Optional.empty();

        } catch (Exception e) {
            log.error("Erro ao buscar cotação de {}: {}", coinId, e.getMessage());
            return Optional.empty();
        }
    }
}
