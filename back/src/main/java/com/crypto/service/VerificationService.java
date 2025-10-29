package com.crypto.service;

import com.crypto.model.User;
import com.crypto.model.VerificationToken;
import com.crypto.repository.UserRepository;
import com.crypto.repository.VerificationTokenRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    @Transactional
    public String createVerificationToken(User user) {
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        String token = UUID.randomUUID().toString();
        String code = generateCode();

        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .code(code)
                .user(user)
                .verified(false)
                .build();

        tokenRepository.save(verificationToken);

        try {
            sendVerificationEmail(user, code);
        } catch (Exception e) {
            log.error("Erro ao enviar email de verificação: {}", e.getMessage(), e);
        }

        return code;
    }

    public void sendVerificationEmail(User user, String code) {
        String subject = "🔐 Código de Verificação - Crypto Monitor";
        String body = String.format("""
                Olá %s!
                
                Para ativar sua conta, use o código abaixo:
                
                ╔══════════════════╗
                ║   CÓDIGO: %s   ║
                ╚══════════════════╝
                
                Este código é válido por 24 horas.
                
                Se você não criou esta conta, ignore este email.
                """, user.getUsername(), code);

        emailService.sendEmail(user.getEmail(), subject, body);
    }

    @Transactional
    public boolean verifyCode(String code) {
        return tokenRepository.findByCode(code)
                .map(token -> {
                    if (token.isExpired() || token.getVerified()) return false;

                    token.setVerified(true);
                    tokenRepository.save(token);

                    User user = token.getUser();
                    user.setEnabled(true);
                    userRepository.save(user);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public boolean resendCode(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.getEnabled()) return false;
                    createVerificationToken(user);
                    return true;
                }).orElse(false);
    }

    public User getUserByCode(@NotBlank(message = "Código é obrigatório") @Size(min = 6, max = 6, message = "Código deve ter 6 dígitos") String code) {
        return null;
    }
}
