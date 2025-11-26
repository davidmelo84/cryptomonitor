package com.crypto.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ VALIDADOR DE VARIÁVEIS DE AMBIENTE OTIMIZADO
 *
 * - ERROS críticos → interrompem o startup imediatamente
 * - AVISOS → validados de forma assíncrona para não atrasar o startup
 */
@Slf4j
@Configuration
public class EnvironmentValidator {

    // ===============================================================
    // 🔐 Variáveis Críticas
    // ===============================================================
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // ===============================================================
    // 📨 SendGrid
    // ===============================================================
    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:}")
    private String fromEmail;

    // ===============================================================
    // 🤖 Telegram
    // ===============================================================
    @Value("${telegram.enabled:false}")
    private boolean telegramEnabled;

    @Value("${telegram.bot.token:}")
    private String telegramBotToken;

    @Value("${telegram.chat.id:}")
    private String telegramChatId;

    // ===============================================================
    // 🚀 Validação principal (crítica) — não pode atrasar
    // ===============================================================
    @PostConstruct
    public void validateEnvironment() {

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 VALIDANDO VARIÁVEIS CRÍTICAS DE AMBIENTE...");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // ============================================================
        // ❌ ERRO CRÍTICO #1 – JWT
        // ============================================================
        if (jwtSecret == null || jwtSecret.isEmpty() || "default_secret".equals(jwtSecret)) {
            throw new IllegalStateException("""
                    ❌ JWT_SECRET não configurado ou usando valor inseguro!
                    Configure no Render:
                       Dashboard → Environment → JWT_SECRET
                    """);
        }

        log.info("✅ JWT_SECRET configurado ({} chars)", jwtSecret.length());

        // ============================================================
        // ❌ ERRO CRÍTICO #2 – Banco de dados em produção
        // ============================================================
        if ("prod".equals(activeProfile) && datasourceUrl.contains("h2:mem")) {
            throw new IllegalStateException("""
                    ❌ H2 em memória detectado em PRODUÇÃO!
                    Configure PostgreSQL imediatamente.
                    """);
        }

        log.info("✅ DATABASE OK: {}", datasourceUrl);

        log.info("✅ Variáveis críticas validadas");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Agora roda validações secundárias sem bloquear
        validateSecondaryConfigsAsync();
    }

    // ===============================================================
    // 🧵 Validações secundárias — não bloqueiam o startup
    // ===============================================================
    @Async
    protected void validateSecondaryConfigsAsync() {
        try {
            Thread.sleep(1500); // dá uma folga pós-inicialização

            log.info("🔍 Validando variáveis secundárias...");

            List<String> warnings = new ArrayList<>();

            // ============================================================
            // 📨 SENDGRID
            // ============================================================
            if ("prod".equals(activeProfile)) {

                if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
                    warnings.add("SENDGRID_API_KEY não configurado (recomendado em produção)");
                } else if (!sendGridApiKey.startsWith("SG.")) {
                    warnings.add("SENDGRID_API_KEY com formato inválido (deve começar com SG.)");
                }

                if (fromEmail == null || fromEmail.isEmpty()) {
                    warnings.add("SENDGRID_FROM_EMAIL não configurado");
                }
            }

            // ============================================================
            // 🔐 JWT tamanho
            // ============================================================
            if (jwtSecret.length() < 32) {
                warnings.add("JWT_SECRET tem menos de 32 caracteres (recomendado: 64)");
            }

            // ============================================================
            // 🤖 TELEGRAM
            // ============================================================
            if ("prod".equals(activeProfile) && telegramEnabled) {

                if (telegramBotToken == null ||
                        !telegramBotToken.matches("^\\d+:[A-Za-z0-9_-]{35}$")) {
                    warnings.add("""
                            TELEGRAM_BOT_TOKEN inválido (esperado: número:token_35_chars)
                            Exemplo: 1234567890:abcdefghijklmnopqrstuvwxyzABCDE_
                            """);
                }

                if (telegramChatId == null || !telegramChatId.matches("^-?\\d+$")) {
                    warnings.add("TELEGRAM_CHAT_ID inválido (deve ser um número)");
                }
            }

            // ============================================================
            // ⚠️ LOG FINAL DE AVISOS (não interrompe)
            // ============================================================
            if (!warnings.isEmpty()) {
                log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.warn("⚠️ AVISOS DE CONFIGURAÇÃO:");
                warnings.forEach(w -> log.warn("   - {}", w));
                log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                log.info("✅ Sem avisos. Ambiente configurado corretamente.");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
