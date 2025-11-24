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
     * 🔧 Validação completa da configuração
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔧 VALIDANDO CONFIGURAÇÃO DO SENDGRID");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 🔥 NOVO: Validar chave obrigatória
        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            throw new IllegalStateException(
                    "❌ SENDGRID_API_KEY NÃO CONFIGURADA!\n" +
                            "Configure no Render:\n" +
                            "1. Dashboard → Environment\n" +
                            "2. Nome: SENDGRID_API_KEY\n" +
                            "3. Valor: SG.xxxxxxxxxx\n" +
                            "4. Restart Service"
            );
        }

        // 🔥 NOVO: Validar formato correto
        if (!sendGridApiKey.startsWith("SG.")) {
            throw new IllegalStateException(
                    "❌ SENDGRID_API_KEY com formato inválido!\n" +
                            "Chaves SendGrid devem começar com 'SG.'\n" +
                            "Crie uma nova em: https://app.sendgrid.com/settings/api_keys"
            );
        }

        // 🔥 NOVO: Validar tamanho
        if (sendGridApiKey.length() < 50) {
            throw new IllegalStateException(
                    "❌ SENDGRID_API_KEY muito curta!\n" +
                            "Chaves válidas geralmente têm 69 caracteres.\n" +
                            "Verifique se copiou a chave inteira."
            );
        }

        // 🔒 LOG SEGURO (mascarado)
        log.info("✅ SENDGRID_API_KEY: {}", maskApiKey(sendGridApiKey));

        // Validar email remetente
        if (fromEmail == null || fromEmail.isEmpty()) {
            throw new IllegalStateException(
                    "❌ SENDGRID_FROM_EMAIL NÃO CONFIGURADO!"
            );
        }

        log.info("✅ SENDGRID_FROM_EMAIL: {}", fromEmail);
        log.info("✅ SENDGRID_FROM_NAME: {}", fromName);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 🔒 Mascara API Key antes de logar
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 15) {
            return "***";
        }

        return apiKey.substring(0, 10) + "..." +
                apiKey.substring(apiKey.length() - 4);
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

        // ✔ Validar antes de enviar
        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            throw new IllegalStateException(
                    "SENDGRID_API_KEY não configurada. Configure no Render."
            );
        }

        if (fromEmail == null || fromEmail.isEmpty()) {
            throw new IllegalStateException(
                    "SENDGRID_FROM_EMAIL não configurado. Configure no Render."
            );
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

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📬 RESPOSTA DO SENDGRID:");
            log.info("   Status Code: {}", statusCode);
            log.info("   Body: {}", response.getBody());

            if (statusCode >= 200 && statusCode < 300) {
                log.info("✅ EMAIL ENVIADO COM SUCESSO!");
            } else {
                log.error("❌ FALHA AO ENVIAR EMAIL!");
                log.error("   Status: {}", statusCode);
                log.error("   Body: {}", response.getBody());
                log.error("   Headers: {}", response.getHeaders());

                throw new RuntimeException(
                        "SendGrid retornou status " + statusCode + ": " + response.getBody()
                );
            }

        } catch (IOException e) {
            log.error("❌ ERRO DE I/O ao chamar SendGrid API");
            log.error("   Mensagem: {}", e.getMessage());
            log.error("   Classe: {}", e.getClass().getName());

            throw new RuntimeException(
                    "Erro de comunicação com SendGrid: " + e.getMessage(), e
            );

        } catch (Exception e) {
            log.error("❌ ERRO INESPERADO ao enviar email");
            log.error("   Tipo: {}", e.getClass().getSimpleName());
            log.error("   Mensagem: {}", e.getMessage(), e);

            throw new RuntimeException(
                    "Erro ao enviar email via SendGrid: " + e.getMessage(), e
            );
        }
    }

    /**
     * 🧪 Testa a configuração enviando email para o próprio remetente
     */
    public boolean testConnection() {
        try {
            log.info("🧪 Testando envio de email (self-test)...");

            sendEmail(
                    fromEmail,
                    "🧪 Teste - Crypto Monitor",
                    "Este é um email de teste.\n\nSe você recebeu, está funcionando! ✅"
            );

            log.info("✅ Teste de conexão OK!");
            return true;

        } catch (Exception e) {
            log.error("❌ Teste de conexão falhou: {}", e.getMessage());
            return false;
        }
    }
}
