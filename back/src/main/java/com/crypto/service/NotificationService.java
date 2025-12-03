package com.crypto.service;

import com.crypto.model.dto.NotificationMessage;
import com.crypto.util.LogMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ REFATORADO - Logs otimizados por ambiente
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final WebClient webClient;

    @Value("${notification.email.enabled:true}")
    private Boolean emailNotificationEnabled;

    @Value("${notification.email.from:crypto-monitor@exemplo.com}")
    private String emailFrom;

    @Value("${notification.email.from-name:Crypto Monitoring System}")
    private String emailFromName;

    @Value("${notification.telegram.enabled:false}")
    private Boolean telegramNotificationEnabled;

    @Value("${notification.telegram.bot-token:}")
    private String telegramBotToken;

    @Value("${notification.telegram.chat-id:}")
    private String telegramChatId;

    @Value("${notification.email.cooldown-minutes:5}")
    private int notificationCooldownMinutes;

    private final Map<String, LocalDateTime> notificationCache = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 3600000)
    public void cleanupNotificationCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        long before = notificationCache.size();

        notificationCache.entrySet().removeIf(
                entry -> entry.getValue().isBefore(cutoff)
        );

        long removed = before - notificationCache.size();
        if (removed > 0) {
            log.debug("Cleanup executado: {} entradas removidas do cache", removed);
        }
    }

    @Async
    public CompletableFuture<Void> sendNotification(NotificationMessage message) {
        try {
            log.info("Enviando notificação para {} - Crypto: {} ({})",
                    LogMasker.maskEmail(message.getRecipient()),
                    message.getCoinSymbol(),
                    message.getAlertType());

            if (isInCooldown(message)) {
                log.debug("Notificação em cooldown: {} - {}",
                        message.getCoinSymbol(), message.getAlertType());
                return CompletableFuture.completedFuture(null);
            }

            updateNotificationCache(message);

            boolean emailSent = false;
            if (emailNotificationEnabled) {
                emailSent = sendEmailNotification(message);
            }

            if (telegramNotificationEnabled && !telegramBotToken.isEmpty()) {
                sendTelegramNotification(message);
            }

            if (emailSent) {
                log.info("Notificação enviada com sucesso");
            } else {
                log.error("Falha ao enviar notificação");
            }

        } catch (Exception e) {
            log.error("Erro ao enviar notificação: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    private boolean sendEmailNotification(NotificationMessage message) {
        try {
            log.debug("Preparando email de notificação");

            String subject = String.format("🚨 Alerta Crypto: %s (%s)",
                    message.getCoinName(), message.getCoinSymbol());

            String body = buildEmailBody(message);

            emailService.sendEmail(message.getRecipient(), subject, body);

            log.debug("Email enviado");
            return true;

        } catch (Exception e) {
            log.error("Erro ao enviar email para {}: {}",
                    LogMasker.maskEmail(message.getRecipient()), e.getMessage());
            return false;
        }
    }

    private String buildEmailBody(NotificationMessage message) {
        return String.format("""
                %s

                📊 Detalhes:
                • Moeda: %s (%s)
                • Preço Atual: %s
                • Variação 24h: %s
                • Tipo de Alerta: %s
                • Data/Hora: %s

                ---
                Este é um alerta automático do sistema de monitoramento de criptomoedas.
                """,
                message.getMessage(),
                message.getCoinName(),
                message.getCoinSymbol(),
                message.getCurrentPrice(),
                message.getChangePercentage(),
                getAlertTypeDescription(message.getAlertType()),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );
    }

    private boolean isInCooldown(NotificationMessage message) {
        String key = message.getCoinSymbol().toUpperCase() + "_" + message.getAlertType();
        LocalDateTime last = notificationCache.get(key);

        if (last == null) return false;

        LocalDateTime cooldownEnd = last.plusMinutes(notificationCooldownMinutes);
        boolean inCooldown = LocalDateTime.now().isBefore(cooldownEnd);

        if (inCooldown) {
            long minutesLeft = java.time.Duration.between(
                    LocalDateTime.now(), cooldownEnd).toMinutes();
            log.debug("Cooldown ativo: {} (faltam {} minutos)", key, minutesLeft);
        }

        return inCooldown;
    }

    private void updateNotificationCache(NotificationMessage message) {
        String key = message.getCoinSymbol().toUpperCase() + "_" + message.getAlertType();
        notificationCache.put(key, LocalDateTime.now());
        log.debug("Cooldown registrado: {}", key);
    }

    private void sendTelegramNotification(NotificationMessage message) {
        try {
            String telegramMessage = buildTelegramMessage(message);

            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage",
                    telegramBotToken
            );

            Map<String, Object> requestBody = Map.of(
                    "chat_id", telegramChatId,
                    "text", telegramMessage,
                    "parse_mode", "Markdown"
            );

            log.debug("Enviando notificação Telegram");

            webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            r -> log.debug("Telegram enviado com sucesso"),
                            e -> log.error("Erro ao enviar Telegram: {}", e.getMessage())
                    );

        } catch (Exception e) {
            log.error("Falha no Telegram: {}", e.getMessage());
        }
    }

    private String buildTelegramMessage(NotificationMessage message) {
        return String.format("""
                %s *%s*

                💰 *%s (%s)*
                💵 Preço: `%s`
                📈 Variação: `%s`
                🕐 %s
                """,
                getEmojiForAlertType(message.getAlertType()),
                getAlertTypeDescription(message.getAlertType()).toUpperCase(),
                message.getCoinName(),
                message.getCoinSymbol(),
                message.getCurrentPrice(),
                message.getChangePercentage(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
        );
    }

    private String getAlertTypeDescription(com.crypto.model.AlertRule.AlertType type) {
        return switch (type) {
            case PRICE_INCREASE -> "Alta de Preço";
            case PRICE_DECREASE -> "Queda de Preço";
            case VOLUME_SPIKE -> "Aumento de Volume";
            case PERCENT_CHANGE_24H -> "Variação 24h";
            case MARKET_CAP -> "Market Cap";
            default -> "Alerta Geral";
        };
    }

    private String getEmojiForAlertType(com.crypto.model.AlertRule.AlertType type) {
        return switch (type) {
            case PRICE_INCREASE -> "📈";
            case PRICE_DECREASE -> "📉";
            case VOLUME_SPIKE -> "🔊";
            case PERCENT_CHANGE_24H -> "⚡";
            case MARKET_CAP -> "🏦";
            default -> "🔔";
        };
    }

    public void sendEmailAlert(String to, String subject, String message) {
        try {
            log.info("Enviando alerta para: {}", LogMasker.maskEmail(to));
            emailService.sendEmail(to, subject, message);
        } catch (Exception e) {
            log.error("Erro ao enviar email: {}", e.getMessage());
        }
    }

    public void clearCooldown(String coinSymbol, String alertType) {
        String key = coinSymbol.toUpperCase() + "_" + alertType;
        LocalDateTime removed = notificationCache.remove(key);

        if (removed != null) {
            log.info("Cooldown removido: {}", key);
        } else {
            log.debug("Cooldown não existia: {}", key);
        }
    }

    public void clearAllCooldowns() {
        int sizeBefore = notificationCache.size();
        notificationCache.clear();
        log.info("Todos os cooldowns removidos (total: {})", sizeBefore);
    }

    public Map<String, Object> getCooldownStats() {
        return Map.of(
                "totalCooldowns", notificationCache.size(),
                "cooldownMinutes", notificationCooldownMinutes,
                "activeCooldowns", new ArrayList<>(notificationCache.keySet())
        );
    }
}