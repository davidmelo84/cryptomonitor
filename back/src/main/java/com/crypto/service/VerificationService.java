// back/src/main/java/com/crypto/service/VerificationService.java
package com.crypto.service;

import com.crypto.model.User;
import com.crypto.model.VerificationToken;
import com.crypto.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Gera código de 6 dígitos
     */
    private String generateCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Cria token de verificação e envia email
     */
    @Transactional
    public String createVerificationToken(User user) {
        // Deletar tokens antigos do usuário
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        // Gerar novo token e código
        String token = UUID.randomUUID().toString();
        String code = generateCode();

        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .code(code)
                .user(user)
                .verified(false)
                .build();

        tokenRepository.save(verificationToken);

        // Enviar email com o código
        sendVerificationEmail(user, code);

        log.info("📧 Token de verificação criado para: {}", user.getEmail());
        return token;
    }

    /**
     * Envia email com código de verificação
     */
    private void sendVerificationEmail(User user, String code) {
        String subject = "🔐 Código de Verificação - Crypto Monitor";

        String body = String.format("""
            Olá %s!
            
            Bem-vindo ao Crypto Monitor! 🚀
            
            Para ativar sua conta, use o código abaixo:
            
            ╔══════════════════╗
            ║   CÓDIGO: %s   ║
            ╚══════════════════╝
            
            Este código é válido por 24 horas.
            
            Se você não criou esta conta, ignore este email.
            
            Atenciosamente,
            Equipe Crypto Monitor
            """,
                user.getUsername(),
                code
        );

        emailService.sendEmail(user.getEmail(), subject, body);
    }

    /**
     * Verifica o código informado
     */
    @Transactional
    public boolean verifyCode(String code) {
        return tokenRepository.findByCode(code)
                .map(token -> {
                    if (token.isExpired()) {
                        log.warn("⚠️ Código expirado: {}", code);
                        return false;
                    }

                    if (token.getVerified()) {
                        log.warn("⚠️ Código já utilizado: {}", code);
                        return false;
                    }

                    // Marcar como verificado
                    token.setVerified(true);
                    tokenRepository.save(token);

                    // Ativar usuário
                    User user = token.getUser();
                    user.setEnabled(true);

                    log.info("✅ Email verificado com sucesso: {}", user.getEmail());
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("❌ Código inválido: {}", code);
                    return false;
                });
    }

    /**
     * Reenvia código de verificação
     */
    @Transactional
    public boolean resendCode(String email) {
        // Buscar usuário não verificado
        // Implementar lógica no UserRepository
        return false; // Implementar
    }
}