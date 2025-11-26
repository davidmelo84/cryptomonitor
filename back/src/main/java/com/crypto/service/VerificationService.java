package com.crypto.service;

import com.crypto.model.User;
import com.crypto.model.VerificationToken;
import com.crypto.repository.UserRepository;
import com.crypto.repository.VerificationTokenRepository;
import com.crypto.util.LogMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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

    // ============================================================
    //  MÉTODO COM RETRY AUTOMÁTICO + EXPONENTIAL BACKOFF
    // ============================================================
    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2) // 2s → 4s → 8s
    )
    public void sendEmailWithRetry(String to, String subject, String body) {
        log.info("📧 Tentando enviar e-mail para {} ...",
                LogMasker.maskEmail(to));

        emailService.sendEmail(to, subject, body);

        log.info("   ✅ EMAIL ENVIADO COM SUCESSO!");
    }

    // ============================================================
    //  RECUPERAÇÃO QUANDO TODAS AS TENTATIVAS FALHAM
    // ============================================================
    @Recover
    public void recoverEmailSend(Exception e, String to, String subject, String body) {
        log.error("❌ ERRO FATAL: Não foi possível enviar o e-mail para {} mesmo após múltiplas tentativas!",
                LogMasker.maskEmail(to));
        throw new RuntimeException("Falha ao enviar email: " + e.getMessage(), e);
    }

    // ============================================================
    //  CRIAÇÃO DO TOKEN + ENVIO DO EMAIL
    // ============================================================
    @Transactional
    public String createVerificationToken(User user) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔐 CRIANDO TOKEN DE VERIFICAÇÃO");
        log.info("   👤 Usuário: {}", LogMasker.maskUsername(user.getUsername()));
        log.info("   📧 Email: {}", LogMasker.maskEmail(user.getEmail()));

        // Remover tokens antigos
        tokenRepository.findByUser(user).ifPresent(token -> {
            log.info("   🗑️ Deletando token antigo para usuário {}",
                    LogMasker.maskUsername(user.getUsername()));
            tokenRepository.delete(token);
        });

        String token = UUID.randomUUID().toString();
        String code = generateCode();

        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .code(code)
                .user(user)
                .verified(false)
                .build();

        tokenRepository.save(verificationToken);

        log.info("   🔑 Token salvo no banco: {}", LogMasker.maskToken(token));
        log.info("   🔢 Código gerado: ****** (oculto por segurança)");

        // --------------------------------------
        // ENVIO DE EMAIL COM SPRING RETRY
        // --------------------------------------
        String subject = "🔐 Código de Verificação - Crypto Monitor";

        String body = String.format("""
                Olá %s!

                Para ativar sua conta no Crypto Monitor, use o código abaixo:

                ╔══════════════════╗
                ║   CÓDIGO: %s   ║
                ╚══════════════════╝

                ⏰ Este código é válido por 24 horas.

                Se você não criou esta conta, ignore este email.

                ---
                Crypto Monitor - Sistema de Monitoramento de Criptomoedas
                https://cryptomonitor-theta.vercel.app
                """, user.getUsername(), code);

        // Agora o retry é automático
        sendEmailWithRetry(user.getEmail(), subject, body);

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return code;
    }

    /**
     * Mantido apenas para compatibilidade; não envia mais email diretamente.
     */
    public void sendVerificationEmail(User user, String code) {
        log.warn("⚠️ sendVerificationEmail() foi chamado, mas o envio síncrono já ocorre em createVerificationToken.");
    }

    // ============================================================
    //  VERIFICAR CÓDIGO
    // ============================================================
    @Transactional
    public boolean verifyCode(String code) {
        log.info("🔍 Verificando código recebido: ******");

        return tokenRepository.findByCode(code)
                .map(token -> {
                    if (token.isExpired()) {
                        log.warn("⏰ Código expirado");
                        return false;
                    }

                    if (token.getVerified()) {
                        log.warn("⚠️ Código já foi utilizado");
                        return false;
                    }

                    token.setVerified(true);
                    tokenRepository.save(token);

                    User user = token.getUser();
                    user.setEnabled(true);
                    userRepository.save(user);

                    log.info("✅ Código verificado! Usuário {} ativado",
                            LogMasker.maskUsername(user.getUsername()));

                    return true;
                }).orElse(false);
    }

    // ============================================================
    //  REENVIAR CÓDIGO
    // ============================================================
    @Transactional
    public boolean resendCode(String email) {
        log.info("🔄 Reenviando código para: {}", LogMasker.maskEmail(email));

        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.getEnabled()) {
                        log.warn("⚠️ Usuário {} já está ativado",
                                LogMasker.maskUsername(user.getUsername()));
                        return false;
                    }
                    createVerificationToken(user);
                    return true;
                }).orElse(false);
    }

    public User getUserByCode(String code) {
        log.debug("🔍 Buscando usuário pelo código: ******");

        return tokenRepository.findByCode(code)
                .map(VerificationToken::getUser)
                .orElseThrow(() -> new RuntimeException("Código não encontrado"));
    }

    public boolean isCodeValid(String code) {
        return tokenRepository.findByCode(code)
                .map(token -> !token.isExpired() && !token.getVerified())
                .orElse(false);
    }

    @Transactional
    public int cleanExpiredTokens() {
        return 0;
    }
}
