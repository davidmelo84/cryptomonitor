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
 * Serviço de envio de notificações assíncronas.
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


    // ================================================
    // 🔥 CLEANUP DO CACHE (executa a cada 1h)
    // ================================================
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


    // ================================================
    // 🔥 MÉTODO ASSÍNCRONO PRINCIPAL
    // ================================================
    @Async("taskExecutor")
    public CompletableFuture<Void> sendNotification(NotificationMessage message) {

        final String masked = LogMasker.maskEmail(message.getRecipient());

        try {
            log.info("Enviando notificação para {} - Crypto: {} ({})",
                    masked,
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
                log.warn("Notificação enviada parcialmente (Telegram OK?)");
            }

        } catch (Exception e) {
            log.error("Erro ao enviar notificação: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }


    // ================================================
    // 🔥 ENVIO DE EMAIL
    // ================================================
    private boolean sendEmailNotification(NotificationMessage message) {
        try {
            log.debug("Preparando email");

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


    // ================================================
    // 🔥 TELEGRAM
    // ================================================
    private void sendTelegramNotification(NotificationMessage message) {
        try {
            String telegramMessage = buildTelegramMessage(message);

            String url = "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage";

            Map<String, Object> requestBody = Map.of(
                    "chat_id", telegramChatId,
                    "text", telegramMessage,
                    "parse_mode", "Markdown"
            );

            webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            r -> log.debug("Telegram enviado"),
                            e -> log.error("Erro ao enviar Telegram: {}", e.getMessage())
                    );

        } catch (Exception e) {
            log.error("Falha geral no Telegram: {}", e.getMessage());
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


    // ================================================
    // 🔥 COOLDOWN
    // ================================================
    private boolean isInCooldown(NotificationMessage message) {
        String key = message.getCoinSymbol().toUpperCase() + "_" + message.getAlertType();
        LocalDateTime last = notificationCache.get(key);

        if (last == null) return false;

        LocalDateTime cooldownEnd = last.plusMinutes(notificationCooldownMinutes);
        boolean inCooldown = LocalDateTime.now().isBefore(cooldownEnd);

        if (inCooldown) {
            long minutesLeft = java.time.Duration
                    .between(LocalDateTime.now(), cooldownEnd)
                    .toMinutes();

            log.debug("Cooldown ativo: {} (faltam {} minutos)", key, minutesLeft);
        }

        return inCooldown;
    }


    private void updateNotificationCache(NotificationMessage message) {
        String key = message.getCoinSymbol().toUpperCase() + "_" + message.getAlertType();
        notificationCache.put(key, LocalDateTime.now());
        log.debug("Cooldown registrado: {}", key);
    }


    // ================================================
    // 🔥 UTILIDADES
    // ================================================
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

    // ================================================
// 🔥 MÉTODO PÚBLICO COMPATÍVEL COM O DebugController
// ================================================
    public void sendEmailAlert(String recipient, String subject, String body) {
        try {
            if (!emailNotificationEnabled) {
                log.warn("Envio de email desabilitado. Ignorando sendEmailAlert().");
                return;
            }

            log.info("Enviando email manual (DebugController) para {}", LogMasker.maskEmail(recipient));

            emailService.sendEmail(recipient, subject, body);

            log.info("Email manual enviado com sucesso para {}", LogMasker.maskEmail(recipient));

        } catch (Exception e) {
            log.error("Erro ao enviar email manual: {}", e.getMessage(), e);
        }
    }
    // ================================================
    // 🔥 ADMIN
    // ================================================
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
