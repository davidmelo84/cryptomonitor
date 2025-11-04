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
     * ✅ NOVO: Validar configuração ao iniciar
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔧 VALIDANDO CONFIGURAÇÃO DO SENDGRID");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            log.error("❌ SENDGRID_API_KEY NÃO CONFIGURADA!");
            log.error("   Adicione no Render: Environment → Add Variable");
            log.error("   Nome: SENDGRID_API_KEY");
            log.error("   Valor: SG.xxxxxxxxxxxx");
        } else {
            String maskedKey = sendGridApiKey.length() > 10
                    ? sendGridApiKey.substring(0, 10) + "..."
                    : "***";
            log.info("✅ SENDGRID_API_KEY: {}", maskedKey);
        }

        if (fromEmail == null || fromEmail.isEmpty()) {
            log.error("❌ SENDGRID_FROM_EMAIL NÃO CONFIGURADO!");
        } else {
            log.info("✅ SENDGRID_FROM_EMAIL: {}", fromEmail);
        }

        log.info("✅ SENDGRID_FROM_NAME: {}", fromName);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Envia email via SendGrid (SÍNCRONO)
     */
    public void sendEmail(String to, String subject, String body) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📧 ENVIANDO EMAIL VIA SENDGRID");
        log.info("   De: {} <{}>", fromName, fromEmail);
        log.info("   Para: {}", to);
        log.info("   Assunto: {}", subject);

        // ✅ VALIDAÇÃO CRÍTICA
        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("❌ ERRO: SENDGRID_API_KEY não está configurada!");
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            throw new IllegalStateException(
                    "SendGrid API Key não configurada. " +
                            "Configure SENDGRID_API_KEY no Render."
            );
        }

        if (fromEmail == null || fromEmail.isEmpty()) {
            throw new IllegalStateException(
                    "SENDGRID_FROM_EMAIL não configurado. " +
                            "Configure no Render."
            );
        }

        try {
            // Criar objetos SendGrid
            Email from = new Email(fromEmail, fromName);
            Email toEmail = new Email(to);
            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, toEmail, content);

            // Enviar via API
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            log.info("📤 Enviando requisição para SendGrid API...");

            Response response = sg.api(request);
            int statusCode = response.getStatusCode();

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📬 RESPOSTA DO SENDGRID:");
            log.info("   Status Code: {}", statusCode);
            log.info("   Body: {}", response.getBody());

            if (statusCode >= 200 && statusCode < 300) {
                log.info("✅ EMAIL ENVIADO COM SUCESSO!");
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                log.error("❌ FALHA AO ENVIAR EMAIL!");
                log.error("   Status: {}", statusCode);
                log.error("   Body: {}", response.getBody());
                log.error("   Headers: {}", response.getHeaders());
                log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                throw new RuntimeException(
                        "SendGrid retornou status " + statusCode + ": " + response.getBody()
                );
            }

        } catch (IOException e) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("❌ ERRO DE I/O ao chamar SendGrid API");
            log.error("   Mensagem: {}", e.getMessage());
            log.error("   Classe: {}", e.getClass().getName());
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            throw new RuntimeException(
                    "Erro de comunicação com SendGrid: " + e.getMessage(), e
            );

        } catch (IllegalStateException e) {
            // Re-lançar erros de configuração
            throw e;

        } catch (Exception e) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("❌ ERRO INESPERADO ao enviar email");
            log.error("   Tipo: {}", e.getClass().getSimpleName());
            log.error("   Mensagem: {}", e.getMessage());
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", e);

            throw new RuntimeException(
                    "Erro ao enviar email via SendGrid: " + e.getMessage(), e
            );
        }
    }

    /**
     * Testa a configuração do SendGrid
     */
    public boolean testConnection() {
        try {
            log.info("🧪 Testando configuração do SendGrid...");

            sendEmail(
                    fromEmail,
                    "🧪 Teste - Crypto Monitor",
                    "Este é um email de teste.\n\nSe você recebeu, está funcionando! ✅"
            );

            log.info("✅ Teste de conexão OK!");
            return true;

        } catch (Exception e) {
            log.error("❌ Teste de conexão FALHOU: {}", e.getMessage());
            return false;
        }
    }
}