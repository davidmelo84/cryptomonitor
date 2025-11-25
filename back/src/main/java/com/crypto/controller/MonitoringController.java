// back/src/main/java/com/crypto/controller/MonitoringController.java

package com.crypto.controller;

import com.crypto.model.AlertRule;
import com.crypto.util.InputSanitizer;
import com.crypto.service.AlertService;
import com.crypto.service.MonitoringControlService;

import com.crypto.util.LogMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringControlService monitoringControlService;
    private final AlertService alertService;
    private final InputSanitizer sanitizer;

    /**
     * Inicia o monitoramento para o usuário autenticado
     */
    @PostMapping("/start")
    public ResponseEntity<?> startMonitoring(
            @RequestBody Map<String, Object> request,
            Authentication authentication
    ) {
        try {
            // ------------------------------
            // ✅ EMAIL
            // ------------------------------
            String emailRaw = (String) request.get("email");
            if (emailRaw == null || emailRaw.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email é obrigatório"));
            }
            String email = sanitizer.sanitizeEmail(emailRaw);

            // ------------------------------
            // ✅ CRYPTOS
            // ------------------------------
            @SuppressWarnings("unchecked")
            List<String> cryptocurrenciesRaw = (List<String>) request.get("cryptocurrencies");

            if (cryptocurrenciesRaw == null || cryptocurrenciesRaw.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Selecione pelo menos uma criptomoeda"));
            }

            List<String> cryptocurrencies = cryptocurrenciesRaw.stream()
                    .map(crypto -> {
                        try {
                            return sanitizer.sanitizeCoinId(crypto);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Criptomoeda inválida: " + crypto);
                        }
                    })
                    .toList();

            Integer checkIntervalMinutes = (Integer) request.get("checkIntervalMinutes");

            // ------------------------------
            // ✅ VALIDAÇÃO DOS THRESHOLDS (CORRIGIDA)
            // ------------------------------

            // 🔹 valor informado pelo usuário (positivo)
            Double buyThreshold = request.get("buyThreshold") != null
                    ? ((Number) request.get("buyThreshold")).doubleValue()
                    : 5.0;

            if (buyThreshold < 0.1 || buyThreshold > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "buyThreshold deve estar entre 0.1% e 100%"));
            }

            // 💡 Será transformado em negativo na criação das regras

            Double sellThreshold = request.get("sellThreshold") != null
                    ? ((Number) request.get("sellThreshold")).doubleValue()
                    : 10.0;

            if (sellThreshold < 0.1 || sellThreshold > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "sellThreshold deve estar entre 0.1% e 100%"));
            }

            String username = authentication != null
                    ? authentication.getName()
                    : "guest";

            // ------------------------------
            // LOG
            // ------------------------------
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📥 REQUISIÇÃO PARA INICIAR MONITORAMENTO");
            log.info("   👤 Usuário: {}", LogMasker.maskUsername(username));
            log.info("   📧 Email: {}", LogMasker.maskEmail(email));
            log.info("   📊 Cryptos (sanitizadas): {}", cryptocurrencies);
            log.info("   ⏱️  Intervalo: {} minutos", checkIntervalMinutes);
            log.info("   📉 Threshold compra (positivo): {}%", buyThreshold);
            log.info("   📈 Threshold venda: {}%", sellThreshold);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // ------------------------------
            // 🔄 Limpar alertas antigos
            // ------------------------------
            try {
                log.info("🗑️  Apagando alertas antigos de {}", email);
                alertService.deactivateAllAlertsForUser(email);
            } catch (Exception e) {
                log.warn("⚠️  Erro ao deletar alertas antigos: {}", e.getMessage());
            }

            // ------------------------------
            // Criação das regras
            // ------------------------------
            int rulesCreated = createAlertRulesForUser(
                    email,
                    cryptocurrencies,
                    buyThreshold,   // positivo aqui
                    sellThreshold
            );

            // ------------------------------
            // Inicia monitoramento
            // ------------------------------
            boolean started = monitoringControlService.startMonitoring(username, email);

            if (started) {
                return ResponseEntity.ok(Map.of(
                        "message", "Monitoramento iniciado com sucesso",
                        "username", username,
                        "email", email,
                        "cryptocurrencies", cryptocurrencies,
                        "alertRulesCreated", rulesCreated,
                        "interval", checkIntervalMinutes != null ? checkIntervalMinutes : 5,
                        "active", true
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Monitoramento já está ativo",
                                "message", "Pare o atual antes de iniciar outro."
                        ));
            }

        } catch (IllegalArgumentException e) {
            log.error("⚠️  Entrada inválida: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("❌ ERRO AO INICIAR MONITORAMENTO: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao iniciar monitoramento",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Cria alertas apenas para as criptomoedas selecionadas
     */
    private int createAlertRulesForUser(
            String email,
            List<String> cryptos,
            Double buyThreshold,      // positivo
            Double sellThreshold
    ) {
        int count = 0;

        log.info("🔧 Criando alertas para {} cryptos", cryptos.size());

        for (String cryptoId : cryptos) {
            try {
                String symbol = mapCoinIdToSymbol(cryptoId);

                log.info("   🔹 Criando alertas para: {} ({})", symbol, cryptoId);

                // ------------------------------
                // 📉 ALERTA DE COMPRA (queda)
                // buyThreshold -> negativo aqui
                // ------------------------------
                AlertRule buyRule = new AlertRule();
                buyRule.setCoinSymbol(symbol);
                buyRule.setNotificationEmail(email);
                buyRule.setAlertType(AlertRule.AlertType.PERCENT_CHANGE_24H);
                buyRule.setThresholdValue(BigDecimal.valueOf(-buyThreshold)); // NEGATIVO CORRETAMENTE
                buyRule.setActive(true);

                alertService.createAlertRule(buyRule);
                count++;

                // ------------------------------
                // 📈 ALERTA DE VENDA (alta)
                // ------------------------------
                AlertRule sellRule = new AlertRule();
                sellRule.setCoinSymbol(symbol);
                sellRule.setNotificationEmail(email);
                sellRule.setAlertType(AlertRule.AlertType.PERCENT_CHANGE_24H);
                sellRule.setThresholdValue(BigDecimal.valueOf(sellThreshold)); // POSITIVO
                sellRule.setActive(true);

                alertService.createAlertRule(sellRule);
                count++;

            } catch (Exception e) {
                log.error("   ❌ Erro ao criar regras para {}: {}", cryptoId, e.getMessage());
            }
        }

        log.info("🎯 Total de alertas criados: {}", count);
        return count;
    }

    /**
     * Mapeia coinId -> símbolo
     */
    private String mapCoinIdToSymbol(String coinId) {
        return switch (coinId.toLowerCase()) {
            case "bitcoin" -> "BTC";
            case "ethereum" -> "ETH";
            case "cardano" -> "ADA";
            case "polkadot" -> "DOT";
            case "chainlink" -> "LINK";
            case "solana" -> "SOL";
            case "avalanhe-2" -> "AVAX";
            case "polygon", "matic-network" -> "MATIC";
            case "litecoin" -> "LTC";
            case "bitcoin-cash" -> "BCH";
            case "ripple" -> "XRP";
            case "dogecoin" -> "DOGE";
            case "binancecoin" -> "BNB";
            default -> coinId.toUpperCase();
        };
    }

    /**
     * Para o monitoramento
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopMonitoring(Authentication authentication) {
        try {
            String username = authentication != null
                    ? authentication.getName()
                    : "guest";

            log.info("🛑 REQUISIÇÃO PARA PARAR MONITORAMENTO — Usuário: {}", username);

            boolean stopped = monitoringControlService.stopMonitoring(username);

            if (stopped) {
                return ResponseEntity.ok(Map.of(
                        "message", "Monitoramento parado com sucesso",
                        "username", username,
                        "active", false
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Nenhum monitoramento ativo",
                                "message", "Não há monitoramento ativo para parar."
                        ));
            }

        } catch (Exception e) {
            log.error("❌ ERRO AO PARAR MONITORAMENTO: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao parar monitoramento",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Status do monitoramento
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication authentication) {
        try {
            String username = authentication != null
                    ? authentication.getName()
                    : "guest";

            Map<String, Object> status = monitoringControlService.getMonitoringStatus(username);

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("❌ Erro ao obter status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao obter status",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Lista monitoramentos ativos
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveMonitorings() {
        try {
            return ResponseEntity.ok(Map.of(
                    "message", "Endpoint em desenvolvimento",
                    "totalActive", 0
            ));
        } catch (Exception e) {
            log.error("❌ Erro ao listar monitoramentos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao listar monitoramentos"));
        }
    }
}
