package com.crypto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // Injeta valores do application.yml
    @Value("${notification.email.from-name:Crypto Monitoring System}")
    private String fromName;

    @Value("${notification.email.from:andremendoncaolv@gmail.com}")
    private String fromEmail;

    /**
     * Envia email de forma síncrona (legacy)
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();

            // Define nome + email do remetente
            mail.setFrom(fromName + " <" + fromEmail + ">");
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);

            mailSender.send(mail);
            log.info("📧 Email enviado com sucesso para: {}", to);

        } catch (Exception e) {
            log.error("❌ Erro ao enviar email para {}: {}", to, e.getMessage());
            throw new RuntimeException("Falha ao enviar email", e);
        }
    }

    /**
     * Envia email de forma assíncrona
     */
    @Async
    public CompletableFuture<Void> sendEmailAsync(String to, String subject, String body) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();

            // Define nome + email do remetente
            mail.setFrom(fromName + " <" + fromEmail + ">");
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);

            mailSender.send(mail);
            log.info("📧 Email assíncrono enviado com sucesso para: {}", to);

        } catch (Exception e) {
            log.error("❌ Erro ao enviar email assíncrono para {}: {}", to, e.getMessage());
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }
}