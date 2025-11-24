package com.crypto.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ VALIDADOR DE VARIÁVEIS DE AMBIENTE
 *
 * Garante que todas variáveis críticas estão configuradas
 * ANTES da aplicação subir completamente.
 */
@Slf4j
@Configuration
public class EnvironmentValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:}")
    private String fromEmail;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @PostConstruct
    public void validateEnvironment() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 VALIDANDO VARIÁVEIS DE AMBIENTE");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // ✅ Validar JWT_SECRET
        if (jwtSecret == null || jwtSecret.isEmpty() || "default_secret".equals(jwtSecret)) {
            errors.add("JWT_SECRET não configurado ou usando valor padrão inseguro");
        } else if (jwtSecret.length() < 32) {
            warnings.add("JWT_SECRET tem menos de 32 caracteres (recomendado: 64)");
        } else {
            log.info("✅ JWT_SECRET: Configurado ({} chars)", jwtSecret.length());
        }

        // ✅ Validar SendGrid (apenas em prod)
        if ("prod".equals(activeProfile)) {
            if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
                errors.add("SENDGRID_API_KEY não configurado (necessário em produção)");
            } else if (!sendGridApiKey.startsWith("SG.")) {
                errors.add("SENDGRID_API_KEY com formato inválido (deve começar com SG.)");
            } else {
                log.info("✅ SENDGRID_API_KEY: Configurado");
            }

            if (fromEmail == null || fromEmail.isEmpty()) {
                errors.add("SENDGRID_FROM_EMAIL não configurado");
            } else {
                log.info("✅ SENDGRID_FROM_EMAIL: {}", fromEmail);
            }
        }

        // ✅ Validar Database
        if (datasourceUrl.contains("h2:mem") && "prod".equals(activeProfile)) {
            errors.add("Usando H2 em memória em PRODUÇÃO! Configure PostgreSQL.");
        } else if (datasourceUrl.contains("postgresql")) {
            log.info("✅ DATABASE: PostgreSQL configurado");
        }

        // ✅ Profile ativo
        log.info("✅ PROFILE: {}", activeProfile);

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // RESULTADO
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        if (!warnings.isEmpty()) {
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("⚠️ AVISOS:");
            warnings.forEach(w -> log.warn("   - {}", w));
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }

        if (!errors.isEmpty()) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("❌ ERROS CRÍTICOS:");
            errors.forEach(e -> log.error("   - {}", e));
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("");
            log.error("🔧 COMO CORRIGIR NO RENDER:");
            log.error("1. Acesse: Dashboard → seu-service → Environment");
            log.error("2. Adicione as variáveis faltantes");
            log.error("3. Clique em 'Save Changes'");
            log.error("4. Render fará restart automático");
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            throw new IllegalStateException(
                    "Configuração inválida! Verifique as variáveis de ambiente."
            );
        }

        log.info("✅ TODAS AS VARIÁVEIS VALIDADAS COM SUCESSO!");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}