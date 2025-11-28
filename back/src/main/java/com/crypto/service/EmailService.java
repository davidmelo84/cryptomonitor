package com.crypto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SendGridEmailService sendGridEmailService;


    public void sendEmail(String to, String subject, String body) {
        log.info("📧 EmailService: Delegando para SendGridEmailService");

        sendGridEmailService.sendEmail(to, subject, body);
    }


    @Async
    public CompletableFuture<Void> sendEmailAsync(String to, String subject, String body) {
        try {
            sendGridEmailService.sendEmail(to, subject, body);
        } catch (Exception e) {
            log.error("❌ Erro no envio assíncrono: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
}