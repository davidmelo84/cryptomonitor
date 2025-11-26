package com.crypto.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class SendGridEmailService {

    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:}")
    private String fromEmail;

    @Value("${sendgrid.from.name:Crypto Monitor}")
    private String fromName;

    /**
     * 🔧 Validação PERMISSIVA — NÃO BLOQUEIA O STARTUP
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔧 VALIDANDO CONFIGURAÇÃO DO SENDGRID");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // ⛔ Não configurar API Key é permitido — apenas warn
        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            log.warn("⚠️ SENDGRID_API_KEY não configurada!");
            log.warn("   Emails NÃO serão enviados.");
            log.warn("   Configure no Render → Environment: SENDGRID_API_KEY");
            return; // 👉 Não bloqueia
        }

        // Apenas valida formato (não bloqueia)
        if (!sendGridApiKey.startsWith("SG.")) {
            log.warn("⚠️ SENDGRID_API_KEY com formato inválido (esperado: SG.xxxxx)");
        }

        // Tamanho esperado ~69 chars
        if (sendGridApiKey.length() < 50) {
            log.warn("⚠️ SENDGRID_API_KEY parece curta (esperado ~69 chars). Pode falhar.");
        }

        log.info("✅ SENDGRID_API_KEY: {}", maskApiKey(sendGridApiKey));

        // Validar email remetente — apenas warn
        if (fromEmail == null || fromEmail.isEmpty()) {
            log.warn("⚠️ SENDGRID_FROM_EMAIL não configurado.");
            log.warn("   Emails NÃO serão enviados.");
            return;
        }

        log.info("✅ SENDGRID_FROM_EMAIL: {}", fromEmail);
        log.info("✅ SENDGRID_FROM_NAME: {}", fromName);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 🔒 Mascara API Key antes de logar
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 15) return "***";
        return apiKey.substring(0, 10) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 📧 Envia email via SendGrid
     */
    public void sendEmail(String to, String subject, String body) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📧 ENVIANDO EMAIL VIA SENDGRID");
        log.info("   De: {} <{}>", fromName, fromEmail);
        log.info("   Para: {}", to);
        log.info("   Assunto: {}", subject);

        // ✔ Validar antes de enviar (aqui sim é crítico)
        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            throw new IllegalStateException("SENDGRID_API_KEY não configurada — configure no Render.");
        }

        if (fromEmail == null || fromEmail.isEmpty()) {
            throw new IllegalStateException("SENDGRID_FROM_EMAIL não configurado — configure no Render.");
        }

        try {
            Email from = new Email(fromEmail, fromName);
            Email toEmail = new Email(to);
            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            log.info("📤 Enviando requisição para SendGrid API...");

            Response response = sg.api(request);
            int statusCode = response.getStatusCode();

            log.info("📬 RESPOSTA DO SENDGRID:");
            log.info("   Status Code: {}", statusCode);
            log.info("   Body: {}", response.getBody());

            if (statusCode >= 200 && statusCode < 300) {
                log.info("✅ EMAIL ENVIADO COM SUCESSO!");
            } else {
                log.error("❌ FALHA AO ENVIAR EMAIL! Status: {}", statusCode);
                throw new RuntimeException("SendGrid retornou erro: " + response.getBody());
            }

        } catch (IOException e) {
            log.error("❌ ERRO DE I/O ao chamar SendGrid API: {}", e.getMessage());
            throw new RuntimeException("Erro ao comunicar com SendGrid", e);

        } catch (Exception e) {
            log.error("❌ ERRO INESPERADO ao enviar email: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar email via SendGrid", e);
        }
    }

    /**
     * 🧪 Testa a configuração enviando email para o próprio remetente
     */
    public boolean testConnection() {
        try {
            if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
                log.warn("⚠️ Teste ignorado — API Key não configurada.");
                return false;
            }

            log.info("🧪 Testando envio de email...");
            sendEmail(fromEmail, "🧪 Teste - Crypto Monitor", "Teste de conexão OK!");
            log.info("✅ Teste OK!");
            return true;

        } catch (Exception e) {
            log.error("❌ Teste de email falhou: {}", e.getMessage());
            return false;
        }
    }
}
