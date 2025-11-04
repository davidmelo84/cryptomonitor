package com.crypto.service;

import com.crypto.model.User;
import com.crypto.model.VerificationToken;
import com.crypto.repository.UserRepository;
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
        log.info("   👤 Usuário: {}", user.getUsername());
        log.info("   📧 Email: {}", user.getEmail());

        // Deletar tokens antigos
        tokenRepository.findByUser(user).ifPresent(token -> {
            log.info("   🗑️ Deletando token antigo");
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
        log.info("   ✅ Token salvo no banco");
        log.info("   🔢 Código: {}", code);

        // Enviar email
        try {
            log.info("   📧 Enviando email de verificação...");
            sendVerificationEmail(user, code);
            log.info("   ✅ EMAIL ENVIADO COM SUCESSO!");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("❌ ERRO CRÍTICO ao enviar email!");
            log.error("   Usuário: {}", user.getUsername());
            log.error("   Email: {}", user.getEmail());
            log.error("   Erro: {}", e.getMessage());
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", e);

            throw new RuntimeException("Falha ao enviar email: " + e.getMessage(), e);
        }

        return code;
    }

    public void sendVerificationEmail(User user, String code) {
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

        emailService.sendEmail(user.getEmail(), subject, body);
    }

    @Transactional
    public boolean verifyCode(String code) {
        log.info("🔍 Verificando código: {}", code);

        return tokenRepository.findByCode(code)
                .map(token -> {
                    if (token.isExpired()) {
                        log.warn("⏰ Código expirado");
                        return false;
                    }

                    if (token.getVerified()) {
                        log.warn("⚠️ Código já usado");
                        return false;
                    }

                    token.setVerified(true);
                    tokenRepository.save(token);

                    User user = token.getUser();
                    user.setEnabled(true);
                    userRepository.save(user);

                    log.info("✅ Código verificado! Usuário {} ativado", user.getUsername());
                    return true;
                }).orElse(false);
    }

    @Transactional
    public boolean resendCode(String email) {
        log.info("🔄 Reenviando código para: {}", email);

        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.getEnabled()) {
                        log.warn("⚠️ Usuário já está ativado");
                        return false;
                    }
                    createVerificationToken(user);
                    return true;
                }).orElse(false);
    }

    /**
     * ✅ NOVO: Busca usuário pelo código de verificação
     * Usado no AuthController após verificação bem-sucedida
     */
    public User getUserByCode(String code) {
        log.debug("🔍 Buscando usuário pelo código: {}", code);

        return tokenRepository.findByCode(code)
                .map(VerificationToken::getUser)
                .orElseThrow(() -> new RuntimeException("Código não encontrado: " + code));
    }

    /**
     * ✅ OPCIONAL: Validar se código existe antes de usar
     */
    public boolean isCodeValid(String code) {
        return tokenRepository.findByCode(code)
                .map(token -> !token.isExpired() && !token.getVerified())
                .orElse(false);
    }

    /**
     * ✅ OPCIONAL: Limpar tokens expirados (tarefa agendada)
     */
    @Transactional
    public int cleanExpiredTokens() {
        // Implementar limpeza de tokens expirados
        // Pode ser chamado por um @Scheduled
        return 0;
    }
}