package com.crypto.service;

import com.crypto.model.AlertRule;
import com.crypto.model.AlertRule.AlertType;
import com.crypto.model.CryptoCurrency;
import com.crypto.model.User;
import com.crypto.model.dto.NotificationMessage;
import com.crypto.repository.AlertRuleRepository;
import com.crypto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private final DecimalFormat df = new DecimalFormat("#,##0.00");



    @Transactional
    public AlertRule createAlertRule(AlertRule alertRule) {
        try {
            log.info("📝 Criando nova regra de alerta");

            if (alertRule.getCoinSymbol() != null) {
                alertRule.setCoinSymbol(alertRule.getCoinSymbol().toUpperCase());
            }

            try {
                Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                        ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                        : null;

                if (principal instanceof org.springframework.security.core.userdetails.User) {
                    String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                    userRepository.findByUsername(username)
                            .ifPresent(alertRule::setUser);
                }
            } catch (Exception ignored) {}

            if (alertRule.getActive() == null) alertRule.setActive(true);

            validateAlertRule(alertRule);

            return alertRuleRepository.save(alertRule);

        } catch (Exception e) {
            log.error("❌ Erro ao criar alerta: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar alerta: " + e.getMessage(), e);
        }
    }

    private void validateAlertRule(AlertRule rule) {
        AlertType type = rule.getAlertType();

        if (rule.getCoinSymbol() == null) {
            throw new IllegalArgumentException("coinSymbol é obrigatório");
        }

        if (type == AlertType.PERCENT_CHANGE_24H) {
            if (rule.getThresholdValue() == null)
                throw new IllegalArgumentException("PERCENT_CHANGE_24H requer thresholdValue");

            rule.setTargetPrice(null);
        }

        if (type == AlertType.PRICE_INCREASE || type == AlertType.PRICE_DECREASE) {
            if (rule.getThresholdValue() == null)
                throw new IllegalArgumentException(type + " requer thresholdValue");

            if (rule.getThresholdValue().compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Preço alvo deve ser > 0");

            rule.setTargetPrice(null);
        }

        if (type == AlertType.VOLUME_SPIKE || type == AlertType.MARKET_CAP) {
            if (rule.getThresholdValue() == null)
                throw new IllegalArgumentException(type + " requer thresholdValue");
            rule.setTargetPrice(null);
        }
    }

    @Transactional
    public int deactivateAllAlertsForUser(String email) {
        List<AlertRule> rules = alertRuleRepository.findByNotificationEmailAndActiveTrue(email);

        int count = 0;
        for (AlertRule rule : rules) {
            rule.setActive(false);
            alertRuleRepository.save(rule);
            count++;
        }

        return count;
    }

    @Transactional
    public void deactivateAlertRule(Long ruleId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Regra não encontrada"));

        rule.setActive(false);
        alertRuleRepository.save(rule);
    }



    public List<AlertRule> getActiveAlertRules() {
        return alertRuleRepository.findByActiveTrue();
    }

    public List<AlertRule> getActiveAlertRulesForUser(String email) {
        return alertRuleRepository.findByNotificationEmailAndActiveTrue(email);
    }



    @Transactional
    public void processAlertsForUser(List<CryptoCurrency> cryptos, String userEmail) {
        List<AlertRule> rules = alertRuleRepository
                .findByNotificationEmailAndActiveTrue(userEmail);

        if (rules.isEmpty()) return;

        Map<String, List<AlertRule>> rulesBySymbol = rules.stream()
                .collect(Collectors.groupingBy(r -> r.getCoinSymbol().toUpperCase()));

        for (CryptoCurrency crypto : cryptos) {
            List<AlertRule> cryptoRules = rulesBySymbol.get(
                    crypto.getSymbol().toUpperCase()
            );

            if (cryptoRules == null) continue;

            for (AlertRule rule : cryptoRules) {
                try {
                    if (shouldTriggerAlert(crypto, rule)) {
                        triggerAlert(crypto, rule);
                    }
                } catch (Exception e) {
                    log.error("Erro processando regra {}: {}", rule.getId(), e.getMessage());
                }
            }
        }
    }

    @Transactional
    public void processAlerts(List<CryptoCurrency> cryptos) {
        List<AlertRule> all = alertRuleRepository.findByActiveTrue();
        if (all.isEmpty()) return;

        Map<String, List<AlertRule>> grouped = all.stream()
                .collect(Collectors.groupingBy(AlertRule::getNotificationEmail));

        log.info("📊 {} usuários possuem alertas ativos", grouped.size());
        log.info("⚠️ Monitoramento automático não dispara alertas — apenas via /monitoring/start");
    }

    public void checkAlertsForCryptoAndUser(CryptoCurrency crypto, String email) {
        List<AlertRule> rules = alertRuleRepository
                .findByCoinSymbolAndNotificationEmailAndActiveTrue(
                        crypto.getSymbol().toUpperCase(), email
                );

        for (AlertRule rule : rules) {
            try {
                if (shouldTriggerAlert(crypto, rule)) {
                    triggerAlert(crypto, rule);
                }
            } catch (Exception ignored) {}
        }
    }



    private boolean shouldTriggerAlert(CryptoCurrency crypto, AlertRule rule) {
        if (crypto == null || rule == null || rule.getThresholdValue() == null) {
            return false;
        }

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

            case MARKET_CAP:
                return crypto.getMarketCap() != null &&
                        crypto.getMarketCap().compareTo(threshold) >= 0;

            case PERCENT_CHANGE_24H:
                if (crypto.getPriceChange24h() == null) return false;
                double change = crypto.getPriceChange24h();
                double trg = threshold.doubleValue();

                return trg < 0 ? change <= trg : change >= trg;

            default:
                return false;
        }
    }


    private void triggerAlert(CryptoCurrency crypto, AlertRule rule) {
        String msg = buildAlertMessage(crypto, rule);

        NotificationMessage notification = NotificationMessage.builder()
                .coinSymbol(crypto.getSymbol().toUpperCase())
                .coinName(crypto.getName())
                .currentPrice("$" + df.format(crypto.getCurrentPrice()))
                .changePercentage(
                        crypto.getPriceChange24h() != null ?
                                String.format("%.2f%%", crypto.getPriceChange24h()) : "N/A"
                )
                .alertType(rule.getAlertType())
                .recipient(rule.getNotificationEmail())
                .message(msg)
                .build();

        notificationService.sendNotification(notification);
    }

    private String buildAlertMessage(CryptoCurrency crypto, AlertRule rule) {
        switch (rule.getAlertType()) {
            case PRICE_INCREASE:
                return "🚀 " + crypto.getName() + " atingiu $" + df.format(crypto.getCurrentPrice());

            case PRICE_DECREASE:
                return "📉 " + crypto.getName() + " caiu para $" + df.format(crypto.getCurrentPrice());

            case VOLUME_SPIKE:
                return "📊 Volume de " + crypto.getName() + " ultrapassou " + df.format(rule.getThresholdValue());

            case MARKET_CAP:
                return "🏦 Market Cap de " + crypto.getName() + " ultrapassou " + df.format(rule.getThresholdValue());

            case PERCENT_CHANGE_24H:
                return "⚡ " + crypto.getName() + " variou " + crypto.getPriceChange24h() + "% em 24h";

            default:
                return "🔔 Alerta ativado para " + crypto.getName();
        }
    }



    public long countActiveAlerts() {
        return alertRuleRepository.findByActiveTrue().size();
    }

    public long countActiveAlertsForUser(String email) {
        return alertRuleRepository
                .findByNotificationEmailAndActiveTrue(email)
                .size();
    }
}
