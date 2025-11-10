// back/src/main/java/com/crypto/service/AlertService.java
package com.crypto.service;

import com.crypto.model.AlertRule;
import com.crypto.model.AlertRule.AlertType; // ✅ Import do enum interno
import com.crypto.dto.CryptoCurrency;
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

    // =========================================
    // GERENCIAMENTO DE ALERTAS
    // =========================================

    /**
     * ✅ Cria uma nova regra de alerta genérica
     */
    @Transactional
    public AlertRule createAlertRule(AlertRule alertRule) {
        try {
            log.info("📝 Criando nova regra de alerta");
            log.debug("   - Símbolo: {}", alertRule.getCoinSymbol());
            log.debug("   - Email: {}", alertRule.getNotificationEmail());
            log.debug("   - Tipo: {}", alertRule.getAlertType());
            log.debug("   - Threshold: {}", alertRule.getThresholdValue());
            log.debug("   - Target Price: {}", alertRule.getTargetPrice());

            // Tentar vincular ao usuário autenticado (se disponível)
            try {
                Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                        ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                        : null;

                if (principal instanceof org.springframework.security.core.userdetails.User) {
                    String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                    Optional<User> userOpt = userRepository.findByUsername(username);

                    if (userOpt.isPresent()) {
                        alertRule.setUser(userOpt.get());
                        log.debug("   ✅ Regra vinculada ao usuário: {}", username);
                    }
                }
            } catch (Exception e) {
                log.debug("   ⚠️ Não foi possível vincular usuário: {}", e.getMessage());
            }

            // Garantir que está ativa
            if (alertRule.getActive() == null) {
                alertRule.setActive(true);
            }

            // Normalizar símbolo para UPPERCASE
            if (alertRule.getCoinSymbol() != null) {
                alertRule.setCoinSymbol(alertRule.getCoinSymbol().toUpperCase());
            }

            // ✅ VALIDAÇÃO: Garantir consistência entre tipo de alerta e campos
            validateAlertRule(alertRule);

            // Salvar no banco
            AlertRule savedRule = alertRuleRepository.save(alertRule);

            log.info("✅ Regra de alerta criada com sucesso: ID {}", savedRule.getId());

            return savedRule;

        } catch (Exception e) {
            log.error("❌ Erro ao criar regra de alerta: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao criar regra de alerta: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ VALIDAÇÃO: Garante consistência entre tipo de alerta e campos
     */
    private void validateAlertRule(AlertRule rule) {
        AlertType type = rule.getAlertType();

        if (type == AlertType.PERCENT_CHANGE_24H) {
            if (rule.getThresholdValue() == null) {
                throw new IllegalArgumentException("PERCENT_CHANGE_24H requer thresholdValue");
            }
            // ✅ targetPrice DEVE ser null para este tipo
            rule.setTargetPrice(null);
        }
        else if (type == AlertType.PRICE_INCREASE || type == AlertType.PRICE_DECREASE) {
            // ✅ Para alertas de preço, usar thresholdValue como preço alvo
            if (rule.getThresholdValue() == null) {
                throw new IllegalArgumentException(type + " requer thresholdValue como preço alvo");
            }
            if (rule.getThresholdValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Preço alvo deve ser maior que zero");
            }
            // ✅ targetPrice pode ser null ou igual a thresholdValue
            rule.setTargetPrice(null);
        }
        else if (type == AlertType.VOLUME_SPIKE || type == AlertType.MARKET_CAP) {
            if (rule.getThresholdValue() == null) {
                throw new IllegalArgumentException(type + " requer thresholdValue");
            }
            rule.setTargetPrice(null);
        }
    }

    /**
     * ✅ Desativa TODOS os alertas de um usuário por email
     */
    @Transactional
    public int deactivateAllAlertsForUser(String email) {
        try {
            log.info("🗑️  Desativando todos os alertas para: {}", email);

            List<AlertRule> userAlerts = alertRuleRepository
                    .findByNotificationEmailAndActiveTrue(email);

            if (userAlerts.isEmpty()) {
                log.info("   ℹ️  Nenhum alerta ativo encontrado para: {}", email);
                return 0;
            }

            int deactivatedCount = 0;

            for (AlertRule rule : userAlerts) {
                rule.setActive(false);
                alertRuleRepository.save(rule);
                deactivatedCount++;

                log.debug("   ✅ Alerta desativado: {} - {} (Threshold: {})",
                        rule.getCoinSymbol(), rule.getAlertType(), rule.getThresholdValue());
            }

            log.info("✅ Total de {} alertas desativados para {}", deactivatedCount, email);

            return deactivatedCount;

        } catch (Exception e) {
            log.error("❌ Erro ao desativar alertas para {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Falha ao desativar alertas: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ Desativa um alerta específico por ID
     */
    @Transactional
    public void deactivateAlertRule(Long ruleId) {
        try {
            log.info("🗑️  Desativando alerta ID: {}", ruleId);

            Optional<AlertRule> ruleOpt = alertRuleRepository.findById(ruleId);

            if (ruleOpt.isEmpty()) {
                log.warn("⚠️  Alerta não encontrado: ID {}", ruleId);
                throw new RuntimeException("Alerta não encontrado: " + ruleId);
            }

            AlertRule rule = ruleOpt.get();
            rule.setActive(false);
            alertRuleRepository.save(rule);

            log.info("✅ Alerta desativado: {} - {} (ID: {})",
                    rule.getCoinSymbol(), rule.getAlertType(), ruleId);

        } catch (Exception e) {
            log.error("❌ Erro ao desativar alerta {}: {}", ruleId, e.getMessage(), e);
            throw new RuntimeException("Falha ao desativar alerta: " + e.getMessage(), e);
        }
    }

    // =========================================
    // CONSULTAS DE ALERTAS
    // =========================================

    public List<AlertRule> getActiveAlertRules() {
        try {
            log.debug("📋 Buscando todos os alertas ativos");
            List<AlertRule> rules = alertRuleRepository.findByActiveTrue();
            log.debug("   ✅ Encontrados {} alertas ativos", rules.size());
            return rules;
        } catch (Exception e) {
            log.error("❌ Erro ao buscar alertas ativos: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao buscar alertas: " + e.getMessage(), e);
        }
    }

    public List<AlertRule> getActiveAlertRulesForUser(String userEmail) {
        try {
            log.debug("📋 Buscando alertas ativos para: {}", userEmail);
            List<AlertRule> rules = alertRuleRepository
                    .findByNotificationEmailAndActiveTrue(userEmail);
            log.debug("   ✅ Encontrados {} alertas para {}", rules.size(), userEmail);
            return rules;
        } catch (Exception e) {
            log.error("❌ Erro ao buscar alertas para {}: {}", userEmail, e.getMessage(), e);
            throw new RuntimeException("Falha ao buscar alertas: " + e.getMessage(), e);
        }
    }

    // =========================================
    // PROCESSAMENTO DE ALERTAS
    // =========================================

    @Transactional
    public void processAlertsForUser(List<CryptoCurrency> cryptos, String userEmail) {
        log.info("🔍 Processando alertas para email: {}", userEmail);

        List<AlertRule> allUserRules = alertRuleRepository
                .findByNotificationEmailAndActiveTrue(userEmail);

        if (allUserRules.isEmpty()) {
            log.debug("Nenhum alerta ativo para: {}", userEmail);
            return;
        }

        Map<String, List<AlertRule>> rulesBySymbol = allUserRules.stream()
                .collect(Collectors.groupingBy(rule -> rule.getCoinSymbol().toUpperCase()));

        int alertsTriggered = 0;

        for (CryptoCurrency crypto : cryptos) {
            String normalizedSymbol = crypto.getSymbol().toUpperCase();
            List<AlertRule> rules = rulesBySymbol.get(normalizedSymbol);

            if (rules == null || rules.isEmpty()) continue;

            for (AlertRule rule : rules) {
                try {
                    if (shouldTriggerAlert(crypto, rule)) {
                        triggerAlert(crypto, rule);
                        alertsTriggered++;
                    }
                } catch (Exception e) {
                    log.error("Erro ao verificar regra {}: {}", rule.getId(), e.getMessage());
                }
            }
        }

        log.info("✅ {} alertas disparados para {}", alertsTriggered, userEmail);
    }

    /**
     * ✅ Processar alertas APENAS para usuários ATIVOS
     *
     * ⚠️ IMPORTANTE: Não processa alertas "órfãos" (sem monitoramento ativo)
     */
    @Transactional
    public void processAlerts(List<CryptoCurrency> cryptos) {
        try {
            log.info("🔍 Processando alertas para todos os usuários");

            List<AlertRule> allActiveRules = alertRuleRepository.findByActiveTrue();

            if (allActiveRules.isEmpty()) {
                log.debug("   ℹ️  Nenhum alerta ativo no sistema");
                return;
            }

            // ✅ NOVO: Agrupar por email
            Map<String, List<AlertRule>> rulesByEmail = allActiveRules.stream()
                    .collect(Collectors.groupingBy(AlertRule::getNotificationEmail));

            // ✅ MUDANÇA CRÍTICA: Apenas logar, NÃO disparar
            log.info("📊 Total de {} usuários com alertas ativos", rulesByEmail.size());

            for (String email : rulesByEmail.keySet()) {
                List<AlertRule> userRules = rulesByEmail.get(email);
                log.debug("   👤 {}: {} alertas", email, userRules.size());
            }

            log.info("ℹ️  Alertas NÃO serão disparados automaticamente");
            log.info("   Use /api/monitoring/start para ativar monitoramento");

        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas: {}", e.getMessage(), e);
        }
    }

    public void checkAlertsForCryptoAndUser(CryptoCurrency crypto, String userEmail) {
        try {
            String normalizedSymbol = crypto.getSymbol().toUpperCase();
            log.debug("🔍 Verificando alertas de {} para {}", normalizedSymbol, userEmail);

            List<AlertRule> rules = alertRuleRepository
                    .findByCoinSymbolAndNotificationEmailAndActiveTrue(normalizedSymbol, userEmail);

            if (rules.isEmpty()) {
                log.debug("   ℹ️  Nenhum alerta para {} ({})", normalizedSymbol, userEmail);
                return;
            }

            int triggered = 0;

            for (AlertRule rule : rules) {
                try {
                    if (shouldTriggerAlert(crypto, rule)) {
                        triggerAlert(crypto, rule);
                        triggered++;
                    }
                } catch (Exception e) {
                    log.error("Erro ao verificar regra {}: {}", rule.getId(), e.getMessage());
                }
            }

            log.debug("   ✅ {} alertas disparados", triggered);

        } catch (Exception e) {
            log.error("❌ Erro ao verificar alertas: {}", e.getMessage(), e);
        }
    }

    // =========================================
    // LÓGICA DE VERIFICAÇÃO
    // =========================================

    private boolean shouldTriggerAlert(CryptoCurrency crypto, AlertRule rule) {
        BigDecimal threshold = rule.getThresholdValue();

        switch (rule.getAlertType()) {
            case PRICE_INCREASE:
                return crypto.getCurrentPrice() != null &&
                        threshold != null &&
                        crypto.getCurrentPrice().compareTo(threshold) >= 0;

            case PRICE_DECREASE:
                return crypto.getCurrentPrice() != null &&
                        threshold != null &&
                        crypto.getCurrentPrice().compareTo(threshold) <= 0;

            case VOLUME_SPIKE:
                return crypto.getTotalVolume() != null &&
                        threshold != null &&
                        crypto.getTotalVolume().compareTo(threshold) >= 0;

            case PERCENT_CHANGE_24H:
                if (crypto.getPriceChange24h() == null || threshold == null) return false;

                double priceChange = crypto.getPriceChange24h();
                double thresholdValue = threshold.doubleValue();

                // Threshold negativo = queda, positivo = alta
                if (thresholdValue < 0) {
                    return priceChange <= thresholdValue;
                } else {
                    return priceChange >= thresholdValue;
                }

            case MARKET_CAP:
                return crypto.getMarketCap() != null &&
                        threshold != null &&
                        crypto.getMarketCap().compareTo(threshold) >= 0;

            default:
                return false;
        }
    }

    // =========================================
    // DISPARO DE ALERTAS
    // =========================================

    private void triggerAlert(CryptoCurrency crypto, AlertRule rule) {
        String message = buildAlertMessage(crypto, rule);

        NotificationMessage notification = NotificationMessage.builder()
                .coinSymbol(crypto.getSymbol().toUpperCase())
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

    private String buildAlertMessage(CryptoCurrency crypto, AlertRule rule) {
        switch (rule.getAlertType()) {
            case PRICE_INCREASE:
                return String.format(
                        "🚀 %s (%s) atingiu $%s (limite $%s). Variação 24h: %.2f%%",
                        crypto.getName(), crypto.getSymbol().toUpperCase(),
                        df.format(crypto.getCurrentPrice()),
                        rule.getThresholdValue() != null ? df.format(rule.getThresholdValue()) : "N/A",
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case PRICE_DECREASE:
                return String.format(
                        "📉 %s (%s) caiu para $%s (limite $%s). Variação 24h: %.2f%%",
                        crypto.getName(), crypto.getSymbol().toUpperCase(),
                        df.format(crypto.getCurrentPrice()),
                        rule.getThresholdValue() != null ? df.format(rule.getThresholdValue()) : "N/A",
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case VOLUME_SPIKE:
                return String.format(
                        "📊 %s (%s) com volume acima de %s (atual %s)",
                        crypto.getName(), crypto.getSymbol().toUpperCase(),
                        rule.getThresholdValue() != null ? df.format(rule.getThresholdValue()) : "N/A",
                        crypto.getTotalVolume() != null ? df.format(crypto.getTotalVolume()) : "N/A"
                );

            case PERCENT_CHANGE_24H:
                return String.format(
                        "⚡ %s (%s) variou %.2f%% nas últimas 24h (limite: %s%%)",
                        crypto.getName(), crypto.getSymbol().toUpperCase(),
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0,
                        rule.getThresholdValue() != null ? df.format(rule.getThresholdValue()) : "N/A"
                );

            case MARKET_CAP:
                return String.format(
                        "🏦 %s (%s) com market cap acima de %s (atual %s)",
                        crypto.getName(), crypto.getSymbol().toUpperCase(),
                        rule.getThresholdValue() != null ? df.format(rule.getThresholdValue()) : "N/A",
                        crypto.getMarketCap() != null ? df.format(crypto.getMarketCap()) : "N/A"
                );

            default:
                return String.format("%s (%s) - alerta disparado",
                        crypto.getName(), crypto.getSymbol().toUpperCase());
        }
    }

    // =========================================
    // UTILITÁRIOS
    // =========================================

    public long countActiveAlerts() {
        try {
            return alertRuleRepository.findByActiveTrue().size();
        } catch (Exception e) {
            log.error("❌ Erro ao contar alertas: {}", e.getMessage());
            return 0;
        }
    }

    public long countActiveAlertsForUser(String userEmail) {
        try {
            return alertRuleRepository
                    .findByNotificationEmailAndActiveTrue(userEmail)
                    .size();
        } catch (Exception e) {
            log.error("❌ Erro ao contar alertas de {}: {}", userEmail, e.getMessage());
            return 0;
        }
    }
}