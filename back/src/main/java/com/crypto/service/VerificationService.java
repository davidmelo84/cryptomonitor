package com.crypto.service;

import com.crypto.model.User;
import com.crypto.model.VerificationToken;
import com.crypto.repository.UserRepository;
import com.crypto.repository.VerificationTokenRepository;
import com.crypto.util.LogMasker;
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
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔐 CRIANDO TOKEN DE VERIFICAÇÃO");
        log.info("   👤 Usuário: {}", LogMasker.maskUsername(user.getUsername()));
        log.info("   📧 Email: {}", LogMasker.maskEmail(user.getEmail()));

        // Deletar tokens antigos
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
        // ✅ ENVIO DE EMAIL SÍNCRONO + RETRY
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

        int maxRetries = 3;
        boolean sent = false;
        Exception lastError = null;

        for (int i = 0; i < maxRetries && !sent; i++) {
            try {
                log.info("📧 Tentando enviar e-mail ({}/{}) para {}",
                        i + 1, maxRetries, LogMasker.maskEmail(user.getEmail()));

                emailService.sendEmail(user.getEmail(), subject, body);

                sent = true;
                log.info("   ✅ EMAIL ENVIADO COM SUCESSO!");
            } catch (Exception e) {
                lastError = e;
                log.warn("⚠️ Falha na tentativa {}/{}: {}", i + 1, maxRetries, e.getMessage());

                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(2000); // aguardar 2s antes do retry
                    } catch (InterruptedException ignored) {}
                }
            }
        }

        if (!sent) {
            log.error("❌ ERRO FATAL: Não foi possível enviar o e-mail após {} tentativas!", maxRetries);
            throw new RuntimeException("Falha ao enviar email: " + lastError.getMessage(), lastError);
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return code;
    }

    /**
     * Mantido para compatibilidade — agora apenas faz log e delega ao método síncrono.
     */
    public void sendVerificationEmail(User user, String code) {
        log.warn("⚠️ sendVerificationEmail() foi chamado, mas o envio síncrono já é feito em createVerificationToken.");
    }

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
