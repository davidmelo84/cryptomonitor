package com.crypto.controller;

import com.crypto.model.AlertRule;
import com.crypto.service.AlertService;
import com.crypto.service.MonitoringControlService;
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
@CrossOrigin(origins = "*")
public class MonitoringController {

    private final MonitoringControlService monitoringControlService;
    private final AlertService alertService; // ⬅️ ADICIONADO

    /**
     * Inicia o monitoramento para o usuário autenticado
     */
    @PostMapping("/start")
    public ResponseEntity<?> startMonitoring(
            @RequestBody Map<String, Object> request,
            Authentication authentication
    ) {
        try {
            String email = (String) request.get("email");
            List<String> cryptocurrencies = (List<String>) request.get("cryptocurrencies");
            Integer checkIntervalMinutes = (Integer) request.get("checkIntervalMinutes");
            Double buyThreshold = request.get("buyThreshold") != null
                    ? ((Number) request.get("buyThreshold")).doubleValue()
                    : 5.0;
            Double sellThreshold = request.get("sellThreshold") != null
                    ? ((Number) request.get("sellThreshold")).doubleValue()
                    : 10.0;

            String username = authentication != null
                    ? authentication.getName()
                    : "guest";

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email é obrigatório"));
            }

            if (cryptocurrencies == null || cryptocurrencies.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Selecione pelo menos uma criptomoeda"));
            }

            log.info("📥 Requisição para iniciar monitoramento:");
            log.info("   - Usuário: {}", username);
            log.info("   - Email: {}", email);
            log.info("   - Criptomoedas: {}", cryptocurrencies);
            log.info("   - Intervalo: {} minutos", checkIntervalMinutes);
            log.info("   - Threshold compra: -{}%", buyThreshold);
            log.info("   - Threshold venda: +{}%", sellThreshold);

            // ⬇️ NOVO: Criar regras de alerta automaticamente
            int rulesCreated = createAlertRulesForUser(email, cryptocurrencies, buyThreshold, sellThreshold);
            log.info("📋 Criadas {} regras de alerta", rulesCreated);

            boolean started = monitoringControlService.startMonitoring(username, email);

            if (started) {
                return ResponseEntity.ok(Map.of(
                        "message", "Monitoramento iniciado com sucesso",
                        "username", username,
                        "email", email,
                        "cryptocurrencies", cryptocurrencies,
                        "alertRulesCreated", rulesCreated, // ⬅️ ADICIONADO
                        "interval", checkIntervalMinutes,
                        "active", true
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Monitoramento já está ativo",
                                "message", "Você já tem um monitoramento ativo. Pare o atual antes de iniciar um novo."
                        ));
            }

        } catch (Exception e) {
            log.error("❌ Erro ao iniciar monitoramento: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao iniciar monitoramento",
                            "message", e.getMessage()
                    ));
        }
    }

    // ⬇️ NOVO MÉTODO AUXILIAR
    private int createAlertRulesForUser(String email, List<String> cryptos, Double buyThreshold, Double sellThreshold) {
        int count = 0;

        for (String cryptoId : cryptos) {
            try {
                // Mapear coinId para símbolo
                String symbol = mapCoinIdToSymbol(cryptoId);

                // Criar regra de QUEDA (oportunidade de compra)
                AlertRule buyRule = new AlertRule();
                buyRule.setCoinSymbol(symbol);
                buyRule.setNotificationEmail(email);
                buyRule.setAlertType(AlertRule.AlertType.PERCENT_CHANGE_24H);
                buyRule.setThresholdValue(BigDecimal.valueOf(-buyThreshold)); // Negativo = queda
                buyRule.setActive(true);
                alertService.createAlertRule(buyRule);
                count++;

                log.info("   ✅ Criada regra de COMPRA: {} com threshold -{}%", symbol, buyThreshold);

                // Criar regra de ALTA (oportunidade de venda)
                AlertRule sellRule = new AlertRule();
                sellRule.setCoinSymbol(symbol);
                sellRule.setNotificationEmail(email);
                sellRule.setAlertType(AlertRule.AlertType.PERCENT_CHANGE_24H);
                sellRule.setThresholdValue(BigDecimal.valueOf(sellThreshold)); // Positivo = alta
                sellRule.setActive(true);
                alertService.createAlertRule(sellRule);
                count++;

                log.info("   ✅ Criada regra de VENDA: {} com threshold +{}%", symbol, sellThreshold);

            } catch (Exception e) {
                log.error("   ❌ Erro ao criar regras para {}: {}", cryptoId, e.getMessage());
            }
        }

        return count;
    }

    // ⬇️ MÉTODO PARA MAPEAR COIN ID PARA SÍMBOLO
    private String mapCoinIdToSymbol(String coinId) {
        return switch (coinId.toLowerCase()) {
            case "bitcoin" -> "BTC";
            case "ethereum" -> "ETH";
            case "cardano" -> "ADA";
            case "polkadot" -> "DOT";
            case "chainlink" -> "LINK";
            case "solana" -> "SOL";
            case "avalanche-2" -> "AVAX";
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
     * Para o monitoramento do usuário autenticado
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopMonitoring(Authentication authentication) {
        try {
            String username = authentication != null
                    ? authentication.getName()
                    : "guest";

            log.info("📥 Requisição para parar monitoramento: user={}", username);

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
            log.error("❌ Erro ao parar monitoramento: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao parar monitoramento",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Verifica o status do monitoramento
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
     * Lista todos os monitoramentos ativos (útil para admin/debug)
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