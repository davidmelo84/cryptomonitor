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
     * ============================================
     * MÉTODOS ORIGINAIS (Mantidos para compatibilidade)
     * ============================================
     */

    /**
     * Processa alertas para todas as criptomoedas (todos os usuários)
     */
    public void processAlerts(List<CryptoCurrency> cryptos) {
        for (CryptoCurrency crypto : cryptos) {
            checkAlertsForCrypto(crypto);
        }
    }

    /**
     * Verifica alertas para uma criptomoeda específica (todos os usuários)
     */
    public void checkAlertsForCrypto(CryptoCurrency crypto) {
        List<AlertRule> rules = alertRuleRepository.findByCoinSymbolAndActiveTrue(crypto.getSymbol());

        for (AlertRule rule : rules) {
            try {
                if (shouldTriggerAlert(crypto, rule)) {
                    triggerAlert(crypto, rule);
                }
            } catch (Exception e) {
                log.error("Erro ao verificar regra {} para {}: {}", rule.getId(), crypto.getSymbol(), e.getMessage());
            }
        }
    }

    /**
     * ============================================
     * NOVOS MÉTODOS - FILTRADOS POR USUÁRIO
     * ============================================
     */

    /**
     * NOVO: Processa alertas APENAS para um usuário específico
     *
     * Este método é chamado pelo MonitoringControlService quando o usuário
     * tem monitoramento ativo.
     *
     * @param cryptos Lista de criptomoedas atualizadas
     * @param userEmail Email do usuário que receberá os alertas
     */
    public void processAlertsForUser(List<CryptoCurrency> cryptos, String userEmail) {
        log.info("🔍 Processando alertas para email: {}", userEmail);

        int alertsChecked = 0;
        int alertsTriggered = 0;

        for (CryptoCurrency crypto : cryptos) {
            try {
                // Busca apenas alertas ATIVOS deste email para esta crypto
                List<AlertRule> rules = alertRuleRepository
                        .findByCoinSymbolAndNotificationEmailAndActiveTrue(
                                crypto.getSymbol(),
                                userEmail
                        );

                alertsChecked += rules.size();

                for (AlertRule rule : rules) {
                    try {
                        if (shouldTriggerAlert(crypto, rule)) {
                            triggerAlert(crypto, rule);
                            alertsTriggered++;
                        }
                    } catch (Exception e) {
                        log.error("Erro ao verificar regra {} para {} (usuário: {}): {}",
                                rule.getId(), crypto.getSymbol(), userEmail, e.getMessage());
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
     * NOVO: Verifica alertas de uma crypto específica APENAS para um usuário
     *
     * Útil para processamento sob demanda de uma criptomoeda específica
     *
     * @param crypto Criptomoeda a verificar
     * @param userEmail Email do usuário
     */
    public void checkAlertsForCryptoAndUser(CryptoCurrency crypto, String userEmail) {
        log.debug("🔍 Verificando alertas de {} para {}", crypto.getSymbol(), userEmail);

        // Busca alertas ativos deste email para esta crypto
        List<AlertRule> rules = alertRuleRepository
                .findByCoinSymbolAndNotificationEmailAndActiveTrue(
                        crypto.getSymbol(),
                        userEmail
                );

        if (rules.isEmpty()) {
            log.debug("Nenhum alerta ativo para {} (usuário: {})", crypto.getSymbol(), userEmail);
            return;
        }

        for (AlertRule rule : rules) {
            try {
                if (shouldTriggerAlert(crypto, rule)) {
                    triggerAlert(crypto, rule);
                    log.info("🔔 Alerta disparado: {} para {} (regra: {})",
                            crypto.getSymbol(), userEmail, rule.getId());
                }
            } catch (Exception e) {
                log.error("Erro ao verificar regra {} para {} (usuário: {}): {}",
                        rule.getId(), crypto.getSymbol(), userEmail, e.getMessage());
            }
        }
    }

    /**
     * NOVO: Busca todos os alertas ativos de um usuário específico
     *
     * Útil para exibir no dashboard do usuário
     *
     * @param userEmail Email do usuário
     * @return Lista de alertas ativos deste usuário
     */
    public List<AlertRule>  getActiveAlertRulesForUser(String userEmail) {
        log.debug("📋 Buscando alertas ativos para: {}", userEmail);

        List<AlertRule> rules = alertRuleRepository
                .findByNotificationEmailAndActiveTrue(userEmail);

        log.debug("Encontrados {} alertas ativos para {}", rules.size(), userEmail);
        return rules;
    }

    /**
     * ============================================
     * MÉTODOS DE VERIFICAÇÃO E TRIGGER
     * (Não alterados - usados pelos métodos acima)
     * ============================================
     */

    /**
     * Verifica se o alerta deve ser disparado
     */
    /**
     * Verifica se o alerta deve ser disparado
     */
    /**
     * Verifica se o alerta deve ser disparado
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
                // ⬇️ CORRIGIDO: Verifica direção da variação
                if (crypto.getPriceChange24h() == null) {
                    return false;
                }

                double priceChange = crypto.getPriceChange24h();
                double thresholdValue = threshold.doubleValue();

                // Log para debug
                log.debug("🔍 {} | Variação: {}% | Threshold: {}%",
                        crypto.getSymbol(), priceChange, thresholdValue);

                // Se threshold é NEGATIVO → alerta de QUEDA
                if (thresholdValue < 0) {
                    boolean triggered = priceChange <= thresholdValue;
                    if (triggered) {
                        log.info("📉 ALERTA DE QUEDA: {} caiu {}% (threshold: {}%)",
                                crypto.getSymbol(), priceChange, thresholdValue);
                    }
                    return triggered;
                }
                // Se threshold é POSITIVO → alerta de ALTA
                else if (thresholdValue > 0) {
                    boolean triggered = priceChange >= thresholdValue;
                    if (triggered) {
                        log.info("📈 ALERTA DE ALTA: {} subiu {}% (threshold: {}%)",
                                crypto.getSymbol(), priceChange, thresholdValue);
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
    }    /**
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

        log.info("🔔 Alerta disparado: {} - {} -> {}",
                crypto.getSymbol(), rule.getAlertType(), message);
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
                        crypto.getSymbol(),
                        df.format(crypto.getCurrentPrice()),
                        df.format(rule.getThresholdValue()),
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case PRICE_DECREASE:
                return String.format(
                        "📉 %s (%s) caiu para $%s (limite $%s). Variação 24h: %.2f%%",
                        crypto.getName(),
                        crypto.getSymbol(),
                        df.format(crypto.getCurrentPrice()),
                        df.format(rule.getThresholdValue()),
                        crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : 0
                );

            case VOLUME_SPIKE:
                return String.format(
                        "📊 %s (%s) com volume acima de %s (atual %s)",
                        crypto.getName(),
                        crypto.getSymbol(),
                        df.format(rule.getThresholdValue()),
                        df.format(crypto.getTotalVolume())
                );

            case PERCENT_CHANGE_24H:
                return String.format(
                        "⚡ %s (%s) variou %.2f%% nas últimas 24h (limite: %s%%)",
                        crypto.getName(),
                        crypto.getSymbol(),
                        crypto.getPriceChange24h(),
                        df.format(rule.getThresholdValue())
                );

            case MARKET_CAP:
                return String.format(
                        "🏦 %s (%s) com market cap acima de %s (atual %s)",
                        crypto.getName(),
                        crypto.getSymbol(),
                        df.format(rule.getThresholdValue()),
                        df.format(crypto.getMarketCap())
                );

            default:
                return String.format("%s (%s) - alerta disparado",
                        crypto.getName(), crypto.getSymbol());
        }
    }

    /**
     * ============================================
     * MÉTODOS DE GERENCIAMENTO DE ALERTAS
     * (Não alterados)
     * ============================================
     */

    /**
     * Cria uma nova regra de alerta
     */
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

    /**
     * Retorna todas as regras de alerta ativas
     */
    public List<AlertRule> getActiveAlertRules() {
        return alertRuleRepository.findByActiveTrue();
    }

    /**
     * Desativa uma regra de alerta
     */
    public void deactivateAlertRule(Long ruleId) {
        alertRuleRepository.findById(ruleId).ifPresent(rule -> {
            rule.setActive(false);
            alertRuleRepository.save(rule);
            log.info("🛑 Regra de alerta {} desativada", ruleId);
        });
    }

    /**
     * Retorna alertas de um usuário específico (por username)
     */
    public List<AlertRule> getAlertRulesForUser(String username) {
        return alertRuleRepository.findAll()
                .stream()
                .filter(r -> r.getUser() != null && username.equals(r.getUser().getUsername()))
                .toList();
    }

    /**
     * NOVO: Desativa todos os alertas de um usuário específico (por email)
     *
     * Útil quando o usuário para o monitoramento
     *
     * @param userEmail Email do usuário
     * @return Número de alertas desativados
     */
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

    /**
     * NOVO: Reativa todos os alertas de um usuário específico (por email)
     *
     * Útil quando o usuário reinicia o monitoramento
     *
     * @param userEmail Email do usuário
     * @return Número de alertas reativados
     */
    public int reactivateAllAlertsForUser(String userEmail) {
        log.info("✅ Reativando alertas para: {}", userEmail);

        List<AlertRule> userAlerts = alertRuleRepository
                .findAll()
                .stream()
                .filter(r -> userEmail.equals(r.getNotificationEmail()) && !r.getActive())
                .toList();

        int reactivatedCount = 0;
        for (AlertRule rule : userAlerts) {
            rule.setActive(true);
            alertRuleRepository.save(rule);
            reactivatedCount++;
        }

        log.info("✅ {} alertas reativados para {}", reactivatedCount, userEmail);
        return reactivatedCount;
    }
}

/*
 * ============================================
 * RESUMO DAS ALTERAÇÕES
 * ============================================
 *
 * NOVOS MÉTODOS ADICIONADOS:
 *
 * 1. processAlertsForUser(List<CryptoCurrency> cryptos, String userEmail)
 *    - Processa alertas APENAS para um usuário específico
 *    - Chamado pelo MonitoringControlService
 *
 * 2. checkAlertsForCryptoAndUser(CryptoCurrency crypto, String userEmail)
 *    - Verifica alertas de uma crypto para um usuário
 *    - Útil para verificação sob demanda
 *
 * 3. getActiveAlertRulesForUser(String userEmail)
 *    - Retorna alertas ativos de um usuário
 *    - Útil para dashboard
 *
 * 4. deactivateAllAlertsForUser(String userEmail)
 *    - Desativa todos os alertas de um usuário
 *    - Chamado quando para o monitoramento
 *
 * 5. reactivateAllAlertsForUser(String userEmail)
 *    - Reativa alertas de um usuário
 *    - Chamado quando reinicia o monitoramento
 *
 * MÉTODOS MANTIDOS (Compatibilidade):
 * - processAlerts(List<CryptoCurrency> cryptos)
 * - checkAlertsForCrypto(CryptoCurrency crypto)
 * - createAlertRule(AlertRule alertRule)
 * - getActiveAlertRules()
 * - deactivateAlertRule(Long ruleId)
 * - getAlertRulesForUser(String username)
 *
 * MÉTODOS AUXILIARES (Inalterados):
 * - shouldTriggerAlert(CryptoCurrency crypto, AlertRule rule)
 * - triggerAlert(CryptoCurrency crypto, AlertRule rule)
 * - buildAlertMessage(CryptoCurrency crypto, AlertRule rule)
 *
 * ============================================
 * FLUXO DE USO:
 * ============================================
 *
 * 1. Usuário inicia monitoramento via frontend
 * 2. MonitoringControlService cria scheduler
 * 3. Scheduler chama: CryptoMonitoringService.updateAndProcessAlertsForUser(email)
 * 4. Este chama: AlertService.processAlertsForUser(cryptos, email)
 * 5. AlertService busca apenas alertas deste email
 * 6. Verifica condições e dispara notificações
 * 7. NotificationService envia email apenas para este usuário
 */