package com.crypto.controller;

import com.crypto.model.AlertRule;
import com.crypto.dto.CryptoCurrency;
import com.crypto.model.dto.AlertRuleDTO;
import com.crypto.service.AlertService;
import com.crypto.service.CryptoService;
import com.crypto.service.CryptoMonitoringService;
import com.crypto.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoService cryptoService;
    private final AlertService alertService;
    private final NotificationService notificationService;
    private final CryptoMonitoringService monitoringService;
    private final WebClient webClient; // ✅ Injetado via Spring

    @Value("${coingecko.api.url:https://api.coingecko.com/api/v3}")
    private String coinGeckoApiUrl; // ✅ URL da API CoinGecko

    @GetMapping("/current")
    public ResponseEntity<List<CryptoCurrency>> getCurrentPrices() {
        try {
            List<CryptoCurrency> cryptos = cryptoService.getCurrentPrices();
            return ResponseEntity.ok(cryptos);
        } catch (Exception e) {
            log.error("Erro ao buscar cotações atuais: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/current/{coinId}")
    public ResponseEntity<CryptoCurrency> getCryptoByCoinId(@PathVariable String coinId) {
        try {
            Optional<CryptoCurrency> crypto = cryptoService.getCryptoByCoinId(coinId);
            return crypto.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Erro ao buscar cotação de {}: {}", coinId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/saved")
    public ResponseEntity<List<CryptoCurrency>> getAllSavedCryptos() {
        try {
            List<CryptoCurrency> cryptos = cryptoService.getAllSavedCryptos();
            return ResponseEntity.ok(cryptos);
        } catch (Exception e) {
            log.error("Erro ao buscar criptomoedas salvas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/saved/{coinId}")
    public ResponseEntity<CryptoCurrency> getSavedCryptoByCoinId(@PathVariable String coinId) {
        try {
            Optional<CryptoCurrency> crypto = cryptoService.getSavedCryptoByCoinId(coinId);
            return crypto.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Erro ao buscar crypto salva {}: {}", coinId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> forceUpdate() {
        try {
            monitoringService.forceUpdateAndProcessAlerts();

            Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Atualização iniciada com sucesso",
                    "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao forçar atualização: {}", e.getMessage());
            Map<String, Object> response = Map.of(
                    "status", "error",
                    "message", "Erro ao iniciar atualização: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/alerts")
    public ResponseEntity<AlertRule> createAlertRule(@Valid @RequestBody AlertRuleDTO alertRuleDTO) {
        try {
            AlertRule alertRule = alertRuleDTO.toEntity();
            AlertRule savedRule = alertService.createAlertRule(alertRule);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRule);
        } catch (Exception e) {
            log.error("Erro ao criar regra de alerta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertRule>> getActiveAlertRules() {
        try {
            List<AlertRule> rules = alertService.getActiveAlertRules();
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            log.error("Erro ao buscar regras de alerta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/alerts/{ruleId}")
    public ResponseEntity<Map<String, String>> deactivateAlertRule(@PathVariable Long ruleId) {
        try {
            alertService.deactivateAlertRule(ruleId);
            Map<String, String> response = Map.of(
                    "status", "success",
                    "message", "Regra de alerta desativada com sucesso"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao desativar regra de alerta: {}", e.getMessage());
            Map<String, String> response = Map.of(
                    "status", "error",
                    "message", "Erro ao desativar regra de alerta: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/test-notification")
    public ResponseEntity<Map<String, String>> sendTestNotification() {
        try {
            notificationService.sendTestNotification();
            Map<String, String> response = Map.of(
                    "status", "success",
                    "message", "Notificação de teste enviada com sucesso"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de teste: {}", e.getMessage());
            Map<String, String> response = Map.of(
                    "status", "error",
                    "message", "Erro ao enviar notificação de teste: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            List<CryptoCurrency> savedCryptos = cryptoService.getAllSavedCryptos();
            List<AlertRule> activeRules = alertService.getActiveAlertRules();
            Map<String, Object> status = Map.of(
                    "status", "online",
                    "timestamp", System.currentTimeMillis(),
                    "cryptos_monitored", savedCryptos.size(),
                    "active_alert_rules", activeRules.size(),
                    "last_update", savedCryptos.isEmpty() ? null :
                            savedCryptos.get(0).getLastUpdated()
            );
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Erro ao obter status: {}", e.getMessage());
            Map<String, Object> status = Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }

        @GetMapping("/history/{coinId}")
    public ResponseEntity<Map<String, Object>> getCryptoHistory(
            @PathVariable String coinId,
            @RequestParam(defaultValue = "7") int days
    ) {
        try {
            String url = String.format(
                    "%s/coins/%s/market_chart?vs_currency=usd&days=%d",
                    coinGeckoApiUrl, coinId, days
            );

            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || !response.containsKey("prices")) {
                return ResponseEntity.notFound().build();
            }

            List<List<Object>> prices = (List<List<Object>>) response.get("prices");

            List<Map<String, ? extends Serializable>> formattedData = prices.stream()
                    .map(price -> {
                        long timestamp = ((Number) price.get(0)).longValue();
                        double priceValue = ((Number) price.get(1)).doubleValue();

                        LocalDateTime date = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(timestamp),
                                ZoneId.systemDefault()
                        );

                        return Map.of(
                                "date", date.format(DateTimeFormatter.ofPattern("dd/MM")),
                                "price", priceValue,
                                "volume", 0
                        );
                    })
                    .collect(Collectors.toList());

            double min = prices.stream()
                    .mapToDouble(p -> ((Number) p.get(1)).doubleValue())
                    .min()
                    .orElse(0);

            double max = prices.stream()
                    .mapToDouble(p -> ((Number) p.get(1)).doubleValue())
                    .max()
                    .orElse(0);

            double current = ((Number) prices.get(prices.size() - 1).get(1)).doubleValue();
            double change = ((current - min) / min) * 100;

            return ResponseEntity.ok(Map.of(
                    "data", formattedData,
                    "stats", Map.of(
                            "min", min,
                            "max", max,
                            "current", current,
                            "change", change
                    )
            ));

        } catch (Exception e) {
            log.error("Erro ao buscar histórico de {}: {}", coinId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/process-alerts/{coinId}")
    public ResponseEntity<Map<String, String>> processAlertsForCrypto(@PathVariable String coinId) {
        try {
            monitoringService.processAlertsForCrypto(coinId);
            Map<String, String> response = Map.of(
                    "status", "success",
                    "message", "Alertas processados para " + coinId
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao processar alertas para {}: {}", coinId, e.getMessage());
            Map<String, String> response = Map.of(
                    "status", "error",
                    "message", "Erro ao processar alertas: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
