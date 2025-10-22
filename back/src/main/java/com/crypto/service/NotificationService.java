package com.crypto.service;

import com.crypto.model.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
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

    private final JavaMailSender mailSender;
    private final WebClient webClient;

    @Value("${notification.email.enabled:true}")
    private Boolean emailNotificationEnabled;

    @Value("${notification.email.from:crypto-monitor@exemplo.com}")
    private String emailFrom;

    @Value("${notification.email.from-name:Crypto Monitoring System}")
    private String emailFromName;

    @Value("${notification.email.to:seu-email@gmail.com}")
    private String defaultEmailTo;

    @Value("${notification.telegram.enabled:false}")
    private Boolean telegramNotificationEnabled;

    @Value("${notification.telegram.bot-token:}")
    private String telegramBotToken;

    @Value("${notification.telegram.chat-id:}")
    private String telegramChatId;

    // Cache para evitar spam de notificações (mesma moeda, mesmo tipo de alerta)
    private final Map<String, LocalDateTime> notificationCache = new ConcurrentHashMap<>();
    private static final int NOTIFICATION_COOLDOWN_MINUTES = 30;

    /**
     * Método principal para envio de notificações
     */
    @Async
    public CompletableFuture<Void> sendNotification(NotificationMessage message) {
        try {
            // Verificar se não está em cooldown
            if (isInCooldown(message)) {
                log.debug("Notificação em cooldown para {}: {}", message.getCoinSymbol(), message.getAlertType());
                return CompletableFuture.completedFuture(null);
            }

            // Registrar no cache
            updateNotificationCache(message);

            // Enviar por diferentes canais
            if (emailNotificationEnabled) {
                sendEmailNotification(message);
            }

            if (telegramNotificationEnabled && !telegramBotToken.isEmpty()) {
                sendTelegramNotification(message);
            }

            // Log da notificação enviada
            log.info("Notificação enviada para {}: {} - {}",
                    message.getCoinSymbol(), message.getAlertType(), message.getChangePercentage());

        } catch (Exception e) {
            log.error("Erro ao enviar notificação: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Envio de notificação por email
     */

    private void sendEmailNotification(NotificationMessage message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(emailFromName + " <" + emailFrom + ">");
            email.setTo(message.getRecipient() != null ? message.getRecipient() : defaultEmailTo);
            email.setSubject(String.format("🚨 Alerta Crypto: %s (%s)",
                    message.getCoinName(), message.getCoinSymbol()));

            String emailBody = buildEmailBody(message);
            email.setText(emailBody);

            mailSender.send(email);
            log.info("Email enviado com sucesso para {}", email.getTo()[0]);

        } catch (Exception e) {
            log.error("Erro ao enviar email: {}", e.getMessage());
        }
    }

    /**
     * Envio de notificação via Telegram
     */
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
                            response -> log.info("Mensagem Telegram enviada com sucesso"),
                            error -> log.error("Erro ao enviar mensagem Telegram: {}", error.getMessage())
                    );

        } catch (Exception e) {
            log.error("Erro ao enviar notificação Telegram: {}", e.getMessage());
        }
    }

    /**
     * Constrói o corpo do email
     */
    private String buildEmailBody(NotificationMessage message) {
        return String.format("""
                %s
                
                📊 Detalhes:
                • Moeda: %s (%s)
                • Preço Atual: %s
                • Variação: %s
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

    /**
     * Constrói mensagem para Telegram
     */
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

    /**
     * Verifica se a notificação está em cooldown
     */
    private boolean isInCooldown(NotificationMessage message) {
        String cacheKey = message.getCoinSymbol() + "_" + message.getAlertType();
        LocalDateTime lastNotification = notificationCache.get(cacheKey);

        if (lastNotification == null) {
            return false;
        }

        return LocalDateTime.now().isBefore(lastNotification.plusMinutes(NOTIFICATION_COOLDOWN_MINUTES));
    }

    /**
     * Atualiza o cache de notificações
     */
    private void updateNotificationCache(NotificationMessage message) {
        String cacheKey = message.getCoinSymbol() + "_" + message.getAlertType();
        notificationCache.put(cacheKey, LocalDateTime.now());

        // Limpar entradas antigas do cache (mais de 2 horas)
        notificationCache.entrySet().removeIf(entry ->
                LocalDateTime.now().isAfter(entry.getValue().plusHours(2))
        );
    }

    /**
     * Retorna descrição do tipo de alerta
     */
    private String getAlertTypeDescription(com.crypto.model.AlertRule.AlertType alertType) {
        switch (alertType) {
            case PRICE_INCREASE:
                return "Alta de Preço";
            case PRICE_DECREASE:
                return "Queda de Preço";
            case VOLUME_SPIKE:
                return "Aumento de Volume";
            default:
                return "Alerta Geral";
        }
    }

    /**
     * Retorna emoji apropriado para o tipo de alerta
     */
    private String getEmojiForAlertType(com.crypto.model.AlertRule.AlertType alertType) {
        switch (alertType) {
            case PRICE_INCREASE:
                return "📈";
            case PRICE_DECREASE:
                return "📉";
            case VOLUME_SPIKE:
                return "🔊";
            default:
                return "🔔";
        }
    }

    /**
     * Envia notificação de teste
     */
    public void sendTestNotification() {
        NotificationMessage testMessage = NotificationMessage.builder()
                .coinSymbol("BTC")
                .coinName("Bitcoin")
                .currentPrice("$45,000.00")
                .changePercentage("5.25%")
                .alertType(com.crypto.model.AlertRule.AlertType.PRICE_INCREASE)
                .message("🧪 Esta é uma notificação de teste do sistema de monitoramento de criptomoedas!")
                .build();

        sendNotification(testMessage);
    }

    // Adicione este método no NotificationService
    public void sendEmailAlert(String to, String subject, String message) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom("testeprojeto0001@gmail.com"); // ou "testeprojeto0001@gmail.com"
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);
            log.info("Email de alerta enviado com sucesso para: {}", to);

        } catch (Exception e) {
            log.error("Erro ao enviar email de alerta: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no envio de email", e);
        }
    }
}