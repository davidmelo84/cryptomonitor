package com.crypto.service;

import com.crypto.model.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 🔥 Cache com cooldowns */
    private final Map<String, LocalDateTime> notificationCache = new ConcurrentHashMap<>();

    /**
     * 🧹 LIMPEZA AUTOMÁTICA DO CACHE A CADA 1 HORA
     * Remove notificações com mais de 2 horas
     */
    @Scheduled(fixedDelay = 3600000)
    public void cleanupNotificationCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);

        long before = notificationCache.size();

        notificationCache.entrySet().removeIf(
                entry -> entry.getValue().isBefore(cutoff)
        );

        long after = notificationCache.size();
        long removed = before - after;

        if (removed > 0) {
            log.debug("🗑️ Cleanup executado: {} entradas removidas do cache de notificações", removed);
        }
    }

    /**
     * Envia notificação
     */
    @Async
    public CompletableFuture<Void> sendNotification(NotificationMessage message) {
        try {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📬 ENVIANDO NOTIFICAÇÃO");
            log.info("   📧 Para: {}", message.getRecipient());
            log.info("   🪙 Crypto: {} ({})", message.getCoinName(), message.getCoinSymbol());
            log.info("   🔔 Tipo: {}", message.getAlertType());
            log.info("   💰 Preço: {}", message.getCurrentPrice());
            log.info("   📊 Variação: {}", message.getChangePercentage());

            if (isInCooldown(message)) {
                log.warn("⏰ Notificação em COOLDOWN para {} ({})",
                        message.getCoinSymbol(), message.getAlertType());
                log.warn("   ⏱️ Cooldown configurado: {} minutos", notificationCooldownMinutes);
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
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
                log.info("✅ NOTIFICAÇÃO ENVIADA COM SUCESSO!");
            } else {
                log.error("❌ FALHA AO ENVIAR NOTIFICAÇÃO!");
            }

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ ERRO CRÍTICO ao enviar notificação: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    private boolean sendEmailNotification(NotificationMessage message) {
        try {
            log.info("📧 Preparando email...");

            String subject = String.format("🚨 Alerta Crypto: %s (%s)",
                    message.getCoinName(), message.getCoinSymbol());

            String emailBody = buildEmailBody(message);

            log.info("📤 Enviando email para: {}", message.getRecipient());

            emailService.sendEmail(message.getRecipient(), subject, emailBody);

            log.info("✅ Email ENVIADO com sucesso para {}", message.getRecipient());
            return true;

        } catch (Exception e) {
            log.error("❌ ERRO ao enviar email:");
            log.error("   📧 Destinatário: {}", message.getRecipient());
            log.error("   🔗 Detalhes: {}", e.getMessage());
            log.error("   Stack trace:", e);
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
                Para mais informações, acesse o painel de controle.
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
        String cacheKey = message.getCoinSymbol().toUpperCase() + "_" + message.getAlertType();
        LocalDateTime lastNotification = notificationCache.get(cacheKey);

        if (lastNotification == null) {
            log.debug("✅ Nenhum cooldown ativo para {}", cacheKey);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownEnd = lastNotification.plusMinutes(notificationCooldownMinutes);
        boolean inCooldown = now.isBefore(cooldownEnd);

        if (inCooldown) {
            long minutesRemaining = java.time.Duration.between(now, cooldownEnd).toMinutes();
            log.warn("⏰ Cooldown ativo: {} (faltam {} minutos)", cacheKey, minutesRemaining);
        }

        return inCooldown;
    }

    /** ⛔ AGORA sem limpeza aqui — limpeza é feita pelo Scheduler */
    private void updateNotificationCache(NotificationMessage message) {
        String cacheKey = message.getCoinSymbol().toUpperCase() + "_" + message.getAlertType();
        notificationCache.put(cacheKey, LocalDateTime.now());

        log.debug("📝 Cooldown registrado: {} (próximo alerta em {} minutos)",
                cacheKey, notificationCooldownMinutes);
    }

    public void clearCooldown(String coinSymbol, String alertType) {
        String cacheKey = coinSymbol.toUpperCase() + "_" + alertType;
        notificationCache.remove(cacheKey);
        log.info("🗑️  Cooldown removido: {}", cacheKey);
    }

    public void clearAllCooldowns() {
        int size = notificationCache.size();
        notificationCache.clear();
        log.info("🗑️  Todos os cooldowns removidos ({} entradas)", size);
    }

    private void sendTelegramNotification(NotificationMessage message) {
        try {
            String telegramMessage = buildTelegramMessage(message);
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", telegramBotToken);

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
                            response -> log.info("✅ Mensagem Telegram enviada com sucesso"),
                            error -> log.error("❌ Erro ao enviar mensagem Telegram: {}", error.getMessage())
                    );

        } catch (Exception e) {
            log.error("❌ Erro ao enviar notificação Telegram: {}", e.getMessage());
        }
    }

    private String buildTelegramMessage(NotificationMessage message) {
        String emoji = getEmojiForAlertType(message.getAlertType());

        return String.format("""
                %s *%s*
                
                💰 *%s (%s)*
                💵 Preço: `%s`
                📈 Variação: `%s`
                🕐 %s
                """,
                emoji,
                getAlertTypeDescription(message.getAlertType()).toUpperCase(),
                message.getCoinName(),
                message.getCoinSymbol(),
                message.getCurrentPrice(),
                message.getChangePercentage(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
        );
    }

    private String getAlertTypeDescription(com.crypto.model.AlertRule.AlertType alertType) {
        switch (alertType) {
            case PRICE_INCREASE:
                return "Alta de Preço";
            case PRICE_DECREASE:
                return "Queda de Preço";
            case VOLUME_SPIKE:
                return "Aumento de Volume";
            case PERCENT_CHANGE_24H:
                return "Variação Percentual 24h";
            case MARKET_CAP:
                return "Market Cap";
            default:
                return "Alerta Geral";
        }
    }

    private String getEmojiForAlertType(com.crypto.model.AlertRule.AlertType alertType) {
        switch (alertType) {
            case PRICE_INCREASE:
                return "📈";
            case PRICE_DECREASE:
                return "📉";
            case VOLUME_SPIKE:
                return "🔊";
            case PERCENT_CHANGE_24H:
                return "⚡";
            case MARKET_CAP:
                return "🏦";
            default:
                return "🔔";
        }
    }

    public void sendTestNotification() {
        NotificationMessage testMessage = NotificationMessage.builder()
                .coinSymbol("BTC")
                .coinName("Bitcoin")
                .currentPrice("$45,000.00")
                .changePercentage("5.25%")
                .alertType(com.crypto.model.AlertRule.AlertType.PRICE_INCREASE)
                .message("🧪 Esta é uma notificação de teste do sistema de monitoramento de criptomoedas!")
                .recipient("seu-email@gmail.com")
                .build();

        log.info("🧪 Enviando notificação de TESTE...");
        sendNotification(testMessage);
    }

    public void sendEmailAlert(String to, String subject, String message) {
        try {
            log.info("📧 Enviando alerta genérico para: {}", to);
            emailService.sendEmail(to, subject, message);
            log.info("✅ Email de alerta enviado com sucesso para: {}", to);

        } catch (Exception e) {
            log.error("❌ Erro ao enviar email de alerta: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no envio de email", e);
        }
    }
}
