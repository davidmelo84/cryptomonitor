package com.crypto.service;

import com.crypto.model.AlertRule;
import com.crypto.dto.CryptoCurrency;
import com.crypto.model.User;
import com.crypto.model.dto.NotificationMessage;
import com.crypto.repository.AlertRuleRepository;
import com.crypto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    /**
     * ✅ CORREÇÃO: Processa alertas APENAS para um usuário específico
     */
    public void processAlertsForUser(List<CryptoCurrency> cryptos, String userEmail) {
        log.info("🔍 Processando alertas para email: {}", userEmail);

        int alertsChecked = 0;
        int alertsTriggered = 0;

        for (CryptoCurrency crypto : cryptos) {
            try {
                // ✅ CORREÇÃO: Normalizar símbolo para UPPERCASE
                String normalizedSymbol = crypto.getSymbol().toUpperCase();

                log.debug("📊 Verificando {} | Symbol API: {} | Normalizado: {}",
                        crypto.getName(), crypto.getSymbol(), normalizedSymbol);

                // Busca apenas alertas ATIVOS deste email para esta crypto
                List<AlertRule> rules = alertRuleRepository
                        .findByCoinSymbolAndNotificationEmailAndActiveTrue(
                                normalizedSymbol,  // ✅ USA UPPERCASE
                                userEmail
                        );

                alertsChecked += rules.size();

                log.debug("🔔 Encontrados {} alertas para {} (email: {})",
                        rules.size(), normalizedSymbol, userEmail);

                for (AlertRule rule : rules) {
                    try {
                        if (shouldTriggerAlert(crypto, rule)) {
                            triggerAlert(crypto, rule);
                            alertsTriggered++;
                        }
                    } catch (Exception e) {
                        log.error("Erro ao verificar regra {} para {} (usuário: {}): {}",
                                rule.getId(), normalizedSymbol, userEmail, e.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("Erro ao processar {} para {}: {}",
                        crypto.getSymbol(), userEmail, e.getMessage());
            }
        }

        log.info("✅ Processamento concluído para {}: {} alertas verificados, {} disparados",
                userEmail, alertsChecked, alertsTriggered);
    }

    /**
     * ✅ CORREÇÃO: Lógica de verificação CORRIGIDA
     */
    private boolean shouldTriggerAlert(CryptoCurrency crypto, AlertRule rule) {
        BigDecimal threshold = rule.getThresholdValue();

        switch (rule.getAlertType()) {
            case PRICE_INCREASE:
                return crypto.getCurrentPrice() != null &&
                        crypto.getCurrentPrice().compareTo(threshold) >= 0;

            case PRICE_DECREASE:
                return crypto.getCurrentPrice() != null &&
                        crypto.getCurrentPrice().compareTo(threshold) <= 0;

            case VOLUME_SPIKE:
                return crypto.getTotalVolume() != null &&
                        crypto.getTotalVolume().compareTo(threshold) >= 0;

            case PERCENT_CHANGE_24H:
                if (crypto.getPriceChange24h() == null) {
                    return false;
                }

                double priceChange = crypto.getPriceChange24h();
                double thresholdValue = threshold.doubleValue();

                // ✅ CORREÇÃO: Log detalhado para DEBUG
                log.debug("🔍 {} | Variação: {}% | Threshold: {}% | Tipo: {}",
                        crypto.getSymbol(),
                        String.format("%.2f", priceChange),
                        String.format("%.2f", thresholdValue),
                        thresholdValue < 0 ? "QUEDA" : "ALTA");

                // ✅ CORREÇÃO: Threshold NEGATIVO → Alerta de QUEDA
                if (thresholdValue < 0) {
                    // Exemplo: threshold = -0.5
                    // Se priceChange = -0.6 → -0.6 <= -0.5 → TRUE ✅
                    // Se priceChange = -0.4 → -0.4 <= -0.5 → FALSE ✅
                    boolean triggered = priceChange <= thresholdValue;

                    if (triggered) {
                        log.info("📉 ALERTA DE QUEDA DISPARADO: {} caiu {}% (threshold: {}%)",
                                crypto.getSymbol(),
                                String.format("%.2f", priceChange),
                                String.format("%.2f", thresholdValue));
                    } else {
                        log.debug("⚪ {} não atingiu threshold de queda: {}% > {}%",
                                crypto.getSymbol(),
                                String.format("%.2f", priceChange),
                                String.format("%.2f", thresholdValue));
                    }

                    return triggered;
                }

                // ✅ CORREÇÃO: Threshold POSITIVO → Alerta de ALTA
                else if (thresholdValue > 0) {
                    // Exemplo: threshold = 0.5
                    // Se priceChange = 0.6 → 0.6 >= 0.5 → TRUE ✅
                    // Se priceChange = 0.4 → 0.4 >= 0.5 → FALSE ✅
                    boolean triggered = priceChange >= thresholdValue;

                    if (triggered) {
                        log.info("📈 ALERTA DE ALTA DISPARADO: {} subiu {}% (threshold: {}%)",
                                crypto.getSymbol(),
                                String.format("%.2f", priceChange),
                                String.format("%.2f", thresholdValue));
                    } else {
                        log.debug("⚪ {} não atingiu threshold de alta: {}% < {}%",
                                crypto.getSymbol(),
                                String.format("%.2f", priceChange),
                                String.format("%.2f", thresholdValue));
                    }

                    return triggered;
                }

                return false;

            case MARKET_CAP:
                return crypto.getMarketCap() != null &&
                        crypto.getMarketCap().compareTo(threshold) >= 0;

            default:
                return false;
        }
    }

    /**
     * Dispara o alerta enviando notificação
     */
    private void triggerAlert(CryptoCurrency crypto, AlertRule rule) {
        String message = buildAlertMessage(crypto, rule);

        NotificationMessage notification = NotificationMessage.builder()
                .coinSymbol(crypto.getSymbol())
                .coinName(crypto.getName())
                .currentPrice("$" + df.format(crypto.getCurrentPrice()))
                .changePercentage(crypto.getPriceChange24h() != null
                        ? String.format("%.2f%%", crypto.getPriceChange24h())
                        : "N/A")
                .alertType(rule.getAlertType())
                .message(message)
                .recipient(rule.getNotificationEmail())
                .build();

        notificationService.sendNotification(notification);

        log.info("🔔 Alerta disparado: {} - {} -> {} (Email: {})",
                crypto.getSymbol(), rule.getAlertType(), message, rule.getNotificationEmail());
    }

    /**
     * Constrói a mensagem do alerta
     */
    private String buildAlertMessage(CryptoCurrency crypto, AlertRule rule) {
        switch (rule.getAlertType()) {
            case PRICE_INCREASE:
                return String.format(
                        "🚀 %s (%s) atingiu $%s (limite $%s). Variação 24h: %.2f%%",
                        crypto.getName(),
                        crypto.getSymbol().toUpperCase(),
                        df.format(crypto.getCurrentPrice()),
                        df.format(rule.getThresholdValue()),
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case PRICE_DECREASE:
                return String.format(
                        "📉 %s (%s) caiu para $%s (limite $%s). Variação 24h: %.2f%%",
                        crypto.getName(),
                        crypto.getSymbol().toUpperCase(),
                        df.format(crypto.getCurrentPrice()),
                        df.format(rule.getThresholdValue()),
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case VOLUME_SPIKE:
                return String.format(
                        "📊 %s (%s) com volume acima de %s (atual %s)",
                        crypto.getName(),
                        crypto.getSymbol().toUpperCase(),
                        df.format(rule.getThresholdValue()),
                        df.format(crypto.getTotalVolume())
                );

            case PERCENT_CHANGE_24H:
                return String.format(
                        "⚡ %s (%s) variou %.2f%% nas últimas 24h (limite: %s%%)",
                        crypto.getName(),
                        crypto.getSymbol().toUpperCase(),
                        crypto.getPriceChange24h(),
                        df.format(rule.getThresholdValue())
                );

            case MARKET_CAP:
                return String.format(
                        "🏦 %s (%s) com market cap acima de %s (atual %s)",
                        crypto.getName(),
                        crypto.getSymbol().toUpperCase(),
                        df.format(rule.getThresholdValue()),
                        df.format(crypto.getMarketCap())
                );

            default:
                return String.format("%s (%s) - alerta disparado",
                        crypto.getName(), crypto.getSymbol().toUpperCase());
        }
    }

    // ============================================
    // MÉTODOS DE GERENCIAMENTO (Mantidos)
    // ============================================

    public AlertRule createAlertRule(AlertRule alertRule) {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                    : null;

            if (principal instanceof org.springframework.security.core.userdetails.User) {
                String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                Optional<User> u = userRepository.findByUsername(username);
                u.ifPresent(alertRule::setUser);
            }
        } catch (Exception e) {
            log.debug("Não foi possível vincular usuário à regra: {}", e.getMessage());
        }

        alertRule.setActive(true);
        AlertRule saved = alertRuleRepository.save(alertRule);
        log.info("✅ Nova regra de alerta criada: {}", saved);
        return saved;
    }

    public List<AlertRule> getActiveAlertRules() {
        return alertRuleRepository.findByActiveTrue();
    }

    public void deactivateAlertRule(Long ruleId) {
        alertRuleRepository.findById(ruleId).ifPresent(rule -> {
            rule.setActive(false);
            alertRuleRepository.save(rule);
            log.info("🛑 Regra de alerta {} desativada", ruleId);
        });
    }

    public int deactivateAllAlertsForUser(String userEmail) {
        log.info("🛑 Desativando todos os alertas para: {}", userEmail);

        List<AlertRule> userAlerts = alertRuleRepository
                .findByNotificationEmailAndActiveTrue(userEmail);

        int deactivatedCount = 0;
        for (AlertRule rule : userAlerts) {
            rule.setActive(false);
            alertRuleRepository.save(rule);
            deactivatedCount++;
        }

        log.info("✅ {} alertas desativados para {}", deactivatedCount, userEmail);
        return deactivatedCount;
    }

    public void checkAlertsForCrypto(CryptoCurrency crypto) {
        String normalizedSymbol = crypto.getSymbol().toUpperCase();
        List<AlertRule> rules = alertRuleRepository.findByCoinSymbolAndActiveTrue(normalizedSymbol);

        for (AlertRule rule : rules) {
            try {
                if (shouldTriggerAlert(crypto, rule)) {
                    triggerAlert(crypto, rule);
                }
            } catch (Exception e) {
                log.error("Erro ao verificar regra {} para {}: {}",
                        rule.getId(), normalizedSymbol, e.getMessage());
            }
        }
    }

    public void checkAlertsForCryptoAndUser(CryptoCurrency crypto, String userEmail) {
        log.debug("🔍 Verificando alertas de {} para {}", crypto.getSymbol(), userEmail);

        String normalizedSymbol = crypto.getSymbol().toUpperCase();

        List<AlertRule> rules = alertRuleRepository
                .findByCoinSymbolAndNotificationEmailAndActiveTrue(
                        normalizedSymbol,
                        userEmail
                );

        if (rules.isEmpty()) {
            log.debug("Nenhum alerta ativo para {} (usuário: {})", normalizedSymbol, userEmail);
            return;
        }

        for (AlertRule rule : rules) {
            try {
                if (shouldTriggerAlert(crypto, rule)) {
                    triggerAlert(crypto, rule);
                    log.info("🔔 Alerta disparado: {} para {} (regra: {})",
                            normalizedSymbol, userEmail, rule.getId());
                }
            } catch (Exception e) {
                log.error("Erro ao verificar regra {} para {} (usuário: {}): {}",
                        rule.getId(), normalizedSymbol, userEmail, e.getMessage());
            }
        }
    }

    public List<AlertRule> getActiveAlertRulesForUser(String userEmail) {
        log.debug("📋 Buscando alertas ativos para: {}", userEmail);
        List<AlertRule> rules = alertRuleRepository
                .findByNotificationEmailAndActiveTrue(userEmail);
        log.debug("Encontrados {} alertas ativos para {}", rules.size(), userEmail);
        return rules;
    }

    public List<AlertRule> getAlertRulesForUser(String username) {
        return alertRuleRepository.findAll()
                .stream()
                .filter(r -> r.getUser() != null && username.equals(r.getUser().getUsername()))
                .toList();
    }

    public void processAlerts(List<CryptoCurrency> cryptos) {
        for (CryptoCurrency crypto : cryptos) {
            checkAlertsForCrypto(crypto);
        }
    }
}