package com.crypto.controller;

import com.crypto.model.CryptoCurrency;
import com.crypto.model.AlertRule;
import com.crypto.service.AlertService;
import com.crypto.service.CryptoService;
import com.crypto.service.NotificationService;
import com.crypto.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;


@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final AlertService alertService;
    private final CryptoService cryptoService;
    private final NotificationService notificationService;
    private final AlertRuleRepository alertRuleRepository;


    @GetMapping("/status")
    public ResponseEntity<?> getSystemStatus() {
        try {
            log.info("🔍 Iniciando diagnóstico completo do sistema...");

            Map<String, Object> status = new HashMap<>();

            List<CryptoCurrency> cryptos = cryptoService.getCurrentPrices();
            status.put("cryptosAvailable", cryptos.size());

            List<Map<String, Object>> cryptoInfo = new ArrayList<>();
            for (CryptoCurrency crypto : cryptos) {
                Map<String, Object> info = new HashMap<>();
                info.put("coinId", crypto.getCoinId());
                info.put("name", crypto.getName());
                info.put("symbol", crypto.getSymbol());
                info.put("symbolUppercase", crypto.getSymbol().toUpperCase());
                info.put("currentPrice", crypto.getCurrentPrice());
                info.put("priceChange24h", crypto.getPriceChange24h());
                cryptoInfo.add(info);
            }
            status.put("cryptos", cryptoInfo);

            List<AlertRule> allAlerts = alertRuleRepository.findAll();
            status.put("totalAlerts", allAlerts.size());
            status.put("activeAlerts", alertRuleRepository.findByActiveTrue().size());

            List<Map<String, Object>> alertInfo = new ArrayList<>();
            for (AlertRule alert : allAlerts) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", alert.getId());
                info.put("coinSymbol", alert.getCoinSymbol());
                info.put("email", alert.getNotificationEmail());
                info.put("type", alert.getAlertType());
                info.put("threshold", alert.getThresholdValue());
                info.put("active", alert.getActive());
                alertInfo.add(info);
            }
            status.put("alerts", alertInfo);

            Map<String, Object> simulationResults = simulateAlertCheck();
            status.put("simulation", simulationResults);

            status.put("timestamp", System.currentTimeMillis());
            status.put("message", "Diagnóstico completo. Verifique os dados acima.");

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("❌ Erro no diagnóstico: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    private Map<String, Object> simulateAlertCheck() {
        Map<String, Object> results = new HashMap<>();

        try {
            List<CryptoCurrency> cryptos = cryptoService.getCurrentPrices();
            List<AlertRule> alerts = alertRuleRepository.findByActiveTrue();

            int matchedAlerts = 0;
            List<Map<String, String>> matches = new ArrayList<>();

            for (CryptoCurrency crypto : cryptos) {
                String normalizedSymbol = crypto.getSymbol().toUpperCase();

                for (AlertRule alert : alerts) {
                    boolean symbolMatch = normalizedSymbol.equals(alert.getCoinSymbol().toUpperCase());

                    if (symbolMatch && alert.getAlertType() == AlertRule.AlertType.PERCENT_CHANGE_24H) {
                        double priceChange = crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0;
                        double threshold = alert.getThresholdValue().doubleValue();

                        boolean wouldTrigger = false;
                        String reason = "";

                        if (threshold < 0) {
                            wouldTrigger = priceChange <= threshold;
                            reason = String.format("Queda: %.2f%% <= %.2f%%", priceChange, threshold);
                        } else if (threshold > 0) {
                            wouldTrigger = priceChange >= threshold;
                            reason = String.format("Alta: %.2f%% >= %.2f%%", priceChange, threshold);
                        }

                        Map<String, String> match = new HashMap<>();
                        match.put("crypto", crypto.getName() + " (" + normalizedSymbol + ")");
                        match.put("priceChange", String.format("%.2f%%", priceChange));
                        match.put("threshold", String.format("%.2f%%", threshold));
                        match.put("wouldTrigger", wouldTrigger ? "SIM ✅" : "NÃO ❌");
                        match.put("reason", reason);
                        match.put("email", alert.getNotificationEmail());

                        matches.add(match);

                        if (wouldTrigger) {
                            matchedAlerts++;
                        }
                    }
                }
            }

            results.put("totalChecks", matches.size());
            results.put("wouldTrigger", matchedAlerts);
            results.put("checks", matches);

        } catch (Exception e) {
            results.put("error", e.getMessage());
        }

        return results;
    }


    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email é obrigatório"));
            }

            log.info("🧪 Testando envio de email para: {}", email);

            notificationService.sendEmailAlert(
                    email,
                    "🧪 Teste do Crypto Monitor",
                    "Se você recebeu este email, o sistema de notificações está funcionando corretamente!\n\n" +
                            "Data/Hora: " + new java.util.Date() + "\n" +
                            "Sistema: Crypto Monitor"
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email de teste enviado para: " + email,
                    "note", "Verifique sua caixa de entrada (e spam)"
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao enviar email de teste: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "suggestion", "Verifique as configurações de email no application.yml"
                    ));
        }
    }


    @PostMapping("/clear-cooldown")
    public ResponseEntity<?> clearCooldown(@RequestBody(required = false) Map<String, String> request) {
        try {
            if (request != null && request.containsKey("coinSymbol") && request.containsKey("alertType")) {
                String coinSymbol = request.get("coinSymbol");
                String alertType = request.get("alertType");

                notificationService.clearCooldown(coinSymbol, alertType);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Cooldown removido para: " + coinSymbol + " (" + alertType + ")"
                ));
            } else {
                notificationService.clearAllCooldowns();

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Todos os cooldowns foram removidos"
                ));
            }

        } catch (Exception e) {
            log.error("❌ Erro ao limpar cooldown: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/force-alert")
    public ResponseEntity<?> forceAlert(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String coinSymbol = (String) request.get("coinSymbol");

            if (email == null || coinSymbol == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email e coinSymbol são obrigatórios"));
            }

            log.info("🔥 Forçando disparo de alerta: {} para {}", coinSymbol, email);

            // Buscar crypto
            Optional<CryptoCurrency> cryptoOpt = cryptoService.getCryptoByCoinId(coinSymbol.toLowerCase());

            if (cryptoOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Criptomoeda não encontrada: " + coinSymbol));
            }

            CryptoCurrency crypto = cryptoOpt.get();

            AlertRule tempAlert = new AlertRule();
            tempAlert.setCoinSymbol(coinSymbol.toUpperCase());
            tempAlert.setNotificationEmail(email);
            tempAlert.setAlertType(AlertRule.AlertType.PERCENT_CHANGE_24H);
            tempAlert.setThresholdValue(BigDecimal.valueOf(-999)); // Sempre vai disparar
            tempAlert.setActive(true);

            alertService.checkAlertsForCryptoAndUser(crypto, email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Alerta forçado para " + coinSymbol,
                    "crypto", Map.of(
                            "name", crypto.getName(),
                            "price", crypto.getCurrentPrice(),
                            "change24h", crypto.getPriceChange24h()
                    )
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao forçar alerta: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/alerts/{email}")
    public ResponseEntity<?> getAlertsByEmail(@PathVariable String email) {
        try {
            List<AlertRule> alerts = alertService.getActiveAlertRulesForUser(email);

            List<Map<String, Object>> alertInfo = new ArrayList<>();
            for (AlertRule alert : alerts) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", alert.getId());
                info.put("coinSymbol", alert.getCoinSymbol());
                info.put("type", alert.getAlertType());
                info.put("threshold", alert.getThresholdValue());
                info.put("active", alert.getActive());
                alertInfo.add(info);
            }

            return ResponseEntity.ok(Map.of(
                    "email", email,
                    "totalAlerts", alerts.size(),
                    "alerts", alertInfo
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao buscar alertas: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/reprocess-alerts")
    public ResponseEntity<?> reprocessAlerts(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email é obrigatório"));
            }

            log.info("🔄 Reprocessando alertas para: {}", email);

            List<CryptoCurrency> cryptos = cryptoService.getCurrentPrices();
            alertService.processAlertsForUser(cryptos, email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Alertas reprocessados para: " + email,
                    "cryptosChecked", cryptos.size()
            ));

        } catch (Exception e) {
            log.error("❌ Erro ao reprocessar alertas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}