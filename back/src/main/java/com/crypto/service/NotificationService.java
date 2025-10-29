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
    
        @Value("${notification.telegram.enabled:false}")
        private Boolean telegramNotificationEnabled;
    
        @Value("${notification.telegram.bot-token:}")
        private String telegramBotToken;
    
        @Value("${notification.telegram.chat-id:}")
        private String telegramChatId;
    
        // ✅ CORREÇÃO: Cooldown REDUZIDO para testes (5 minutos)
        // Em produção, voltar para 30 minutos
        @Value("${notification.email.cooldown-minutes:5}")
        private int notificationCooldownMinutes;
    
        // Cache para evitar spam de notificações
        private final Map<String, LocalDateTime> notificationCache = new ConcurrentHashMap<>();
    
        /**
         * ✅ CORREÇÃO: Método principal com LOGS detalhados
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
    
                // ✅ CORREÇÃO: Verificar cooldown COM LOGS
                if (isInCooldown(message)) {
                    log.warn("⏰ Notificação em COOLDOWN para {} ({})",
                            message.getCoinSymbol(), message.getAlertType());
                    log.warn("   ⏱️  Cooldown configurado: {} minutos", notificationCooldownMinutes);
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    return CompletableFuture.completedFuture(null);
                }
    
                // ✅ CORREÇÃO: Registrar no cache ANTES de enviar
                updateNotificationCache(message);
    
                // Enviar por diferentes canais
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
    
        /**
         * ✅ CORREÇÃO: Envio de email com TRATAMENTO DE ERROS detalhado
         */
        private boolean sendEmailNotification(NotificationMessage message) {
            try {
                log.info("📧 Preparando email...");
    
                SimpleMailMessage email = new SimpleMailMessage();
                email.setFrom(emailFromName + " <" + emailFrom + ">");
                email.setTo(message.getRecipient());
                email.setSubject(String.format("🚨 Alerta Crypto: %s (%s)",
                        message.getCoinName(), message.getCoinSymbol()));
    
                String emailBody = buildEmailBody(message);
                email.setText(emailBody);
    
                log.info("📤 Enviando email para: {}", message.getRecipient());
                log.debug("📝 Assunto: {}", email.getSubject());
                log.debug("📄 Corpo (preview): {}", emailBody.substring(0, Math.min(100, emailBody.length())));
    
                mailSender.send(email);
    
                log.info("✅ Email ENVIADO com sucesso para {}", email.getTo()[0]);
                return true;
    
            } catch (org.springframework.mail.MailAuthenticationException e) {
                log.error("❌ ERRO DE AUTENTICAÇÃO no email:");
                log.error("   📧 Email configurado: {}", emailFrom);
                log.error("   ⚠️  Verifique:");
                log.error("      1. Senha de app do Gmail está correta?");
                log.error("      2. Verificação em duas etapas está ativa?");
                log.error("      3. Variáveis de ambiente estão carregadas?");
                log.error("   🔗 Detalhes: {}", e.getMessage());
                return false;
    
            } catch (org.springframework.mail.MailSendException e) {
                log.error("❌ ERRO ao ENVIAR email:");
                log.error("   📧 Destinatário: {}", message.getRecipient());
                log.error("   ⚠️  Verifique:");
                log.error("      1. Email do destinatário está correto?");
                log.error("      2. Servidor SMTP está acessível?");
                log.error("   🔗 Detalhes: {}", e.getMessage());
                return false;
    
            } catch (Exception e) {
                log.error("❌ ERRO INESPERADO ao enviar email:");
                log.error("   🔗 Tipo: {}", e.getClass().getSimpleName());
                log.error("   🔗 Mensagem: {}", e.getMessage());
                log.error("   🔗 Stack trace:", e);
                return false;
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
    
        /**
         * ✅ CORREÇÃO: Verificação de cooldown COM LOGS
         */
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
    
        /**
         * Atualiza o cache de notificações
         */
        private void updateNotificationCache(NotificationMessage message) {
            String cacheKey = message.getCoinSymbol().toUpperCase() + "_" + message.getAlertType();
            notificationCache.put(cacheKey, LocalDateTime.now());
    
            log.debug("📝 Cooldown registrado: {} (próximo alerta em {} minutos)",
                    cacheKey, notificationCooldownMinutes);
    
            // Limpar entradas antigas do cache (mais de 2 horas)
            notificationCache.entrySet().removeIf(entry ->
                    LocalDateTime.now().isAfter(entry.getValue().plusHours(2))
            );
        }
    
        /**
         * ✅ NOVO: Método para LIMPAR cooldown (útil para testes)
         */
        public void clearCooldown(String coinSymbol, String alertType) {
            String cacheKey = coinSymbol.toUpperCase() + "_" + alertType;
            notificationCache.remove(cacheKey);
            log.info("🗑️  Cooldown removido: {}", cacheKey);
        }
    
        /**
         * ✅ NOVO: Método para LIMPAR TODOS os cooldowns
         */
        public void clearAllCooldowns() {
            int size = notificationCache.size();
            notificationCache.clear();
            log.info("🗑️  Todos os cooldowns removidos ({} entradas)", size);
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
                                response -> log.info("✅ Mensagem Telegram enviada com sucesso"),
                                error -> log.error("❌ Erro ao enviar mensagem Telegram: {}", error.getMessage())
                        );
    
            } catch (Exception e) {
                log.error("❌ Erro ao enviar notificação Telegram: {}", e.getMessage());
            }
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
                case PERCENT_CHANGE_24H:
                    return "Variação Percentual 24h";
                case MARKET_CAP:
                    return "Market Cap";
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
                case PERCENT_CHANGE_24H:
                    return "⚡";
                case MARKET_CAP:
                    return "🏦";
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
                    .recipient("seu-email@gmail.com") // ✅ IMPORTANTE: Configurar email de teste
                    .build();
    
            log.info("🧪 Enviando notificação de TESTE...");
            sendNotification(testMessage);
        }
    
        /**
         * Envia alerta genérico (usado por TradingBots)
         */
        public void sendEmailAlert(String to, String subject, String message) {
            try {
                log.info("📧 Enviando alerta genérico para: {}", to);
    
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom(emailFromName + " <" + emailFrom + ">");
                mailMessage.setTo(to);
                mailMessage.setSubject(subject);
                mailMessage.setText(message);
    
                mailSender.send(mailMessage);
                log.info("✅ Email de alerta enviado com sucesso para: {}", to);
    
            } catch (Exception e) {
                log.error("❌ Erro ao enviar email de alerta: {}", e.getMessage(), e);
                throw new RuntimeException("Falha no envio de email", e);
            }
        }
    }