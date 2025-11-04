// back/src/main/java/com/crypto/service/AlertService.java

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
     * Cria uma nova regra de alerta
     */
    @Transactional
    public AlertRule createAlertRule(AlertRule alertRule) {
        try {
            log.info("📝 Criando nova regra de alerta");
            log.debug("   - Símbolo: {}", alertRule.getCoinSymbol());
            log.debug("   - Email: {}", alertRule.getNotificationEmail());
            log.debug("   - Tipo: {}", alertRule.getAlertType());
            log.debug("   - Threshold: {}", alertRule.getThresholdValue());

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
     * Desativa TODOS os alertas de um usuário por email
     */
    @Transactional
    public int deactivateAllAlertsForUser(String email) {
        try {
            log.info("🗑️  Desativando todos os alertas para: {}", email);

            // Buscar todos os alertas ativos do usuário
            List<AlertRule> userAlerts = alertRuleRepository
                    .findByNotificationEmailAndActiveTrue(email);

            if (userAlerts.isEmpty()) {
                log.info("   ℹ️  Nenhum alerta ativo encontrado para: {}", email);
                return 0;
            }

            int deactivatedCount = 0;

            // Desativar cada alerta
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
     * Desativa um alerta específico por ID
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

    /**
     * Deleta fisicamente um alerta (alternativa à desativação)
     */
    @Transactional
    public void deleteAlertRule(Long ruleId) {
        try {
            log.info("🗑️  Deletando alerta ID: {}", ruleId);

            if (!alertRuleRepository.existsById(ruleId)) {
                log.warn("⚠️  Alerta não encontrado: ID {}", ruleId);
                throw new RuntimeException("Alerta não encontrado: " + ruleId);
            }

            alertRuleRepository.deleteById(ruleId);
            log.info("✅ Alerta deletado: ID {}", ruleId);

        } catch (Exception e) {
            log.error("❌ Erro ao deletar alerta {}: {}", ruleId, e.getMessage(), e);
            throw new RuntimeException("Falha ao deletar alerta: " + e.getMessage(), e);
        }
    }

    // =========================================
    // CONSULTAS DE ALERTAS
    // =========================================

    /**
     * Retorna todos os alertas ativos do sistema
     */
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

    /**
     * Retorna alertas ativos de um email específico
     */
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

    /**
     * Retorna alertas de um usuário por username (requer User vinculado)
     */
    public List<AlertRule> getAlertRulesForUser(String username) {
        try {
            log.debug("📋 Buscando alertas para username: {}", username);

            return alertRuleRepository.findAll()
                    .stream()
                    .filter(rule -> rule.getUser() != null &&
                            username.equals(rule.getUser().getUsername()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Erro ao buscar alertas para username {}: {}", username, e.getMessage(), e);
            throw new RuntimeException("Falha ao buscar alertas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um alerta específico por ID
     */
    public Optional<AlertRule> getAlertRuleById(Long ruleId) {
        try {
            log.debug("📋 Buscando alerta ID: {}", ruleId);
            return alertRuleRepository.findById(ruleId);
        } catch (Exception e) {
            log.error("❌ Erro ao buscar alerta {}: {}", ruleId, e.getMessage(), e);
            throw new RuntimeException("Falha ao buscar alerta: " + e.getMessage(), e);
        }
    }

    /**
     * Retorna alertas por símbolo e email
     */
    public List<AlertRule> getAlertsBySymbolAndEmail(String coinSymbol, String email) {
        try {
            String normalizedSymbol = coinSymbol.toUpperCase();
            log.debug("📋 Buscando alertas: {} para {}", normalizedSymbol, email);

            return alertRuleRepository
                    .findByCoinSymbolAndNotificationEmailAndActiveTrue(normalizedSymbol, email);
        } catch (Exception e) {
            log.error("❌ Erro ao buscar alertas: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao buscar alertas: " + e.getMessage(), e);
        }
    }

    // =========================================
    // PROCESSAMENTO DE ALERTAS
    // =========================================

    /**
     * Processa alertas para um usuário específico
     */
    @Transactional
    public void processAlertsForUser(List<CryptoCurrency> cryptos, String userEmail) {
        log.info("🔍 Processando alertas para email: {}", userEmail);

        // Buscar todos os alertas ativos do usuário
        List<AlertRule> allUserRules = alertRuleRepository
                .findByNotificationEmailAndActiveTrue(userEmail);

        if (allUserRules.isEmpty()) {
            log.debug("Nenhum alerta ativo para: {}", userEmail);
            return;
        }

        // Agrupar alertas por símbolo (UPPERCASE)
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
     * Processa alertas para TODOS os usuários (usado pelo sistema)
     */
    public void processAlerts(List<CryptoCurrency> cryptos) {
        try {
            log.info("🔍 Processando alertas para todos os usuários");

            List<AlertRule> allActiveRules = alertRuleRepository.findByActiveTrue();

            if (allActiveRules.isEmpty()) {
                log.debug("   ℹ️  Nenhum alerta ativo no sistema");
                return;
            }

            // Agrupar por email
            Map<String, List<AlertRule>> rulesByEmail = allActiveRules.stream()
                    .collect(Collectors.groupingBy(AlertRule::getNotificationEmail));

            int totalTriggered = 0;

            for (Map.Entry<String, List<AlertRule>> entry : rulesByEmail.entrySet()) {
                String email = entry.getKey();
                List<AlertRule> userRules = entry.getValue();

                // Agrupar por símbolo
                Map<String, List<AlertRule>> rulesBySymbol = userRules.stream()
                        .collect(Collectors.groupingBy(rule -> rule.getCoinSymbol().toUpperCase()));

                for (CryptoCurrency crypto : cryptos) {
                    String normalizedSymbol = crypto.getSymbol().toUpperCase();
                    List<AlertRule> rules = rulesBySymbol.get(normalizedSymbol);

                    if (rules == null || rules.isEmpty()) continue;

                    for (AlertRule rule : rules) {
                        try {
                            if (shouldTriggerAlert(crypto, rule)) {
                                triggerAlert(crypto, rule);
                                totalTriggered++;
                            }
                        } catch (Exception e) {
                            log.error("Erro ao processar regra {}: {}", rule.getId(), e.getMessage());
                        }
                    }
                }
            }

            log.info("✅ Total de {} alertas disparados", totalTriggered);

        } catch (Exception e) {
            log.error("❌ Erro ao processar alertas: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica alertas para uma crypto específica
     */
    public void checkAlertsForCrypto(CryptoCurrency crypto) {
        try {
            String normalizedSymbol = crypto.getSymbol().toUpperCase();
            log.debug("🔍 Verificando alertas para: {}", normalizedSymbol);

            List<AlertRule> rules = alertRuleRepository
                    .findByCoinSymbolAndActiveTrue(normalizedSymbol);

            if (rules.isEmpty()) {
                log.debug("   ℹ️  Nenhum alerta para {}", normalizedSymbol);
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

            log.debug("   ✅ {} alertas disparados para {}", triggered, normalizedSymbol);

        } catch (Exception e) {
            log.error("❌ Erro ao verificar alertas: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica alertas para uma crypto e usuário específicos
     */
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
                        crypto.getCurrentPrice().compareTo(threshold) >= 0;

            case PRICE_DECREASE:
                return crypto.getCurrentPrice() != null &&
                        crypto.getCurrentPrice().compareTo(threshold) <= 0;

            case VOLUME_SPIKE:
                return crypto.getTotalVolume() != null &&
                        crypto.getTotalVolume().compareTo(threshold) >= 0;

            case PERCENT_CHANGE_24H:
                if (crypto.getPriceChange24h() == null) return false;

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
                        df.format(crypto.getCurrentPrice()), df.format(rule.getThresholdValue()),
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case PRICE_DECREASE:
                return String.format(
                        "📉 %s (%s) caiu para $%s (limite $%s). Variação 24h: %.2f%%",
                        crypto.getName(), crypto.getSymbol().toUpperCase(),
                        df.format(crypto.getCurrentPrice()), df.format(rule.getThresholdValue()),
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case VOLUME_SPIKE:
                return String.format(
                        "📊 %s (%s) com volume acima de %s (atual %s)",
                        crypto.getName(), crypto.getSymbol().toUpperCase(),
                        df.format(rule.getThresholdValue()), df.format(crypto.getTotalVolume())
                );

            case PERCENT_CHANGE_24H:
                return String.format(
                        "⚡ %s (%s) variou %.2f%% nas últimas 24h (limite: %s%%)",
                        crypto.getName(), crypto.getSymbol().toUpperCase(),
                        crypto.getPriceChange24h(), df.format(rule.getThresholdValue())
                );

            case MARKET_CAP:
                return String.format(
                        "🏦 %s (%s) com market cap acima de %s (atual %s)",
                        crypto.getName(), crypto.getSymbol().toUpperCase(),
                        df.format(rule.getThresholdValue()), df.format(crypto.getMarketCap())
                );

            default:
                return String.format("%s (%s) - alerta disparado",
                        crypto.getName(), crypto.getSymbol().toUpperCase());
        }
    }

    // =========================================
    // UTILITÁRIOS
    // =========================================

    /**
     * Conta quantos alertas ativos existem no sistema
     */
    public long countActiveAlerts() {
        try {
            return alertRuleRepository.findByActiveTrue().size();
        } catch (Exception e) {
            log.error("❌ Erro ao contar alertas: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Conta quantos alertas ativos um usuário tem
     */
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

    /**
     * Reativa um alerta desativado
     */
    @Transactional
    public void reactivateAlertRule(Long ruleId) {
        try {
            log.info("🔄 Reativando alerta ID: {}", ruleId);

            Optional<AlertRule> ruleOpt = alertRuleRepository.findById(ruleId);

            if (ruleOpt.isEmpty()) {
                throw new RuntimeException("Alerta não encontrado: " + ruleId);
            }

            AlertRule rule = ruleOpt.get();
            rule.setActive(true);
            alertRuleRepository.save(rule);

            log.info("✅ Alerta reativado: ID {}", ruleId);

        } catch (Exception e) {
            log.error("❌ Erro ao reativar alerta {}: {}", ruleId, e.getMessage(), e);
            throw new RuntimeException("Falha ao reativar alerta: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza um alerta existente
     */
    @Transactional
    public AlertRule updateAlertRule(Long ruleId, AlertRule updatedRule) {
        try {
            log.info("🔄 Atualizando alerta ID: {}", ruleId);

            Optional<AlertRule> existingRuleOpt = alertRuleRepository.findById(ruleId);

            if (existingRuleOpt.isEmpty()) {
                throw new RuntimeException("Alerta não encontrado: " + ruleId);
            }

            AlertRule existingRule = existingRuleOpt.get();

            // Atualizar campos
            if (updatedRule.getCoinSymbol() != null) {
                existingRule.setCoinSymbol(updatedRule.getCoinSymbol().toUpperCase());
            }
            if (updatedRule.getAlertType() != null) {
                existingRule.setAlertType(updatedRule.getAlertType());
            }
            if (updatedRule.getThresholdValue() != null) {
                existingRule.setThresholdValue(updatedRule.getThresholdValue());
            }
            if (updatedRule.getNotificationEmail() != null) {
                existingRule.setNotificationEmail(updatedRule.getNotificationEmail());
            }
            if (updatedRule.getActive() != null) {
                existingRule.setActive(updatedRule.getActive());
            }

            AlertRule saved = alertRuleRepository.save(existingRule);
            log.info("✅ Alerta atualizado: ID {}", ruleId);

            return saved;

        } catch (Exception e) {
            log.error("❌ Erro ao atualizar alerta {}: {}", ruleId, e.getMessage(), e);
            throw new RuntimeException("Falha ao atualizar alerta: " + e.getMessage(), e);
        }
    }
}