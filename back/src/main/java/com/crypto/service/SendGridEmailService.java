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

/**
 * ✅ REFATORADO - Logs otimizados por ambiente
 * - DEBUG: Detalhes técnicos
 * - INFO: Eventos de negócio relevantes
 * - WARN/ERROR: Problemas que requerem atenção
 */
@Slf4j
@Service
public class SendGridEmailService {

    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:}")
    private String fromEmail;

    @Value("${sendgrid.from.name:Crypto Monitor}")
    private String fromName;

    @PostConstruct
    public void validateConfiguration() {
        log.info("Validando configuração do SendGrid");

        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            log.warn("SENDGRID_API_KEY não configurada - emails não serão enviados");
            return;
        }

        if (!sendGridApiKey.startsWith("SG.")) {
            log.warn("SENDGRID_API_KEY com formato inválido (esperado: SG.xxxxx)");
        }

        if (sendGridApiKey.length() < 50) {
            log.warn("SENDGRID_API_KEY parece curta - pode falhar");
        }

        log.debug("SENDGRID_API_KEY: {}", maskApiKey(sendGridApiKey));

        if (fromEmail == null || fromEmail.isEmpty()) {
            log.warn("SENDGRID_FROM_EMAIL não configurado");
            return;
        }

        log.info("SendGrid configurado - From: {} <{}>", fromName, fromEmail);
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 15) return "***";
        return apiKey.substring(0, 10) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Envia email via SendGrid
     *
     * @throws IllegalStateException se configuração estiver inválida
     * @throws RuntimeException se falha ao enviar
     */
    public void sendEmail(String to, String subject, String body) {
        log.debug("Enviando email via SendGrid para: {}", to);

        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            throw new IllegalStateException("SENDGRID_API_KEY não configurada");
        }

        if (fromEmail == null || fromEmail.isEmpty()) {
            throw new IllegalStateException("SENDGRID_FROM_EMAIL não configurado");
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

            log.debug("Enviando requisição para SendGrid API");

            Response response = sg.api(request);
            int statusCode = response.getStatusCode();

            log.debug("Resposta SendGrid - Status: {}", statusCode);

            if (statusCode >= 200 && statusCode < 300) {
                log.info("Email enviado com sucesso para: {}", to);
            } else {
                log.error("Falha ao enviar email - Status: {}, Body: {}",
                        statusCode, response.getBody());
                throw new RuntimeException("SendGrid retornou erro: " + statusCode);
            }

        } catch (IOException e) {
            log.error("Erro de I/O ao comunicar com SendGrid: {}", e.getMessage());
            throw new RuntimeException("Erro ao comunicar com SendGrid", e);

        } catch (Exception e) {
            log.error("Erro inesperado ao enviar email: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar email via SendGrid", e);
        }
    }

    public boolean testConnection() {
        try {
            if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
                log.warn("Teste ignorado - API Key não configurada");
                return false;
            }

            log.info("Testando conexão SendGrid");
            sendEmail(fromEmail, "🧪 Teste - Crypto Monitor",
                    "Teste de conexão OK!\nTimestamp: " + java.time.LocalDateTime.now());
            log.info("Teste de conexão SendGrid OK");
            return true;

        } catch (Exception e) {
            log.error("Teste de email falhou: {}", e.getMessage());
            return false;
        }
    }
}