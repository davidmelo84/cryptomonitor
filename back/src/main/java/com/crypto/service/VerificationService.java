// back/src/main/java/com/crypto/service/VerificationService.java
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

    /**
     * Gera código de 6 dígitos
     */
    private String generateCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * ✅ CORRIGIDO: Cria token de verificação e envia email
     */
    @Transactional
    public String createVerificationToken(User user) {
        try {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📝 CRIANDO TOKEN DE VERIFICAÇÃO");
            log.info("   👤 Usuário: {}", user.getUsername());
            log.info("   📧 Email: {}", user.getEmail());

            // ✅ Deletar tokens antigos do usuário (se existirem)
            tokenRepository.findByUser(user).ifPresent(oldToken -> {
                log.info("🗑️  Deletando token antigo: {}", oldToken.getCode());
                tokenRepository.delete(oldToken);
            });

            // ✅ Gerar novo token e código
            String token = UUID.randomUUID().toString();
            String code = generateCode();

            log.info("🔑 Token gerado: {}", token);
            log.info("🔢 Código gerado: {}", code);

            // ✅ Criar entidade
            VerificationToken verificationToken = VerificationToken.builder()
                    .token(token)
                    .code(code)
                    .user(user)
                    .verified(false)
                    .build();

            // ✅ Salvar no banco
            VerificationToken saved = tokenRepository.save(verificationToken);
            log.info("✅ Token salvo no banco - ID: {}", saved.getId());

            // ✅ Enviar email COM TRY-CATCH
            try {
                sendVerificationEmail(user, code);
                log.info("📧 Email de verificação enviado com sucesso!");
            } catch (Exception emailError) {
                log.error("❌ ERRO AO ENVIAR EMAIL:");
                log.error("   Mensagem: {}", emailError.getMessage());
                log.error("   Tipo: {}", emailError.getClass().getSimpleName());
                emailError.printStackTrace();

                // ✅ NÃO FALHAR - Token foi criado, email pode ser reenviado depois
                log.warn("⚠️  Token criado, mas email falhou. Usuário pode solicitar reenvio.");
            }

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return token;

        } catch (Exception e) {
            log.error("❌ ERRO CRÍTICO ao criar token de verificação:", e);
            throw new RuntimeException("Falha ao criar token de verificação", e);
        }
    }

    /**
     * ✅ CORRIGIDO: Envia email com código de verificação
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

        log.info("📤 Enviando email de verificação...");
        log.info("   Para: {}", user.getEmail());
        log.info("   Código: {}", code);

        // ✅ CRÍTICO: EmailService deve propagar exceções para serem tratadas
        emailService.sendEmail(user.getEmail(), subject, body);
    }

    /**
     * ✅ CORRIGIDO: Verifica o código informado
     */
    @Transactional
    public boolean verifyCode(String code) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 VERIFICANDO CÓDIGO: {}", code);

        return tokenRepository.findByCode(code)
                .map(token -> {
                    log.info("✅ Token encontrado no banco");
                    log.info("   ID: {}", token.getId());
                    log.info("   Usuário: {}", token.getUser().getUsername());
                    log.info("   Expirado: {}", token.isExpired());
                    log.info("   Já verificado: {}", token.getVerified());

                    if (token.isExpired()) {
                        log.warn("⚠️ Código EXPIRADO");
                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        return false;
                    }

                    if (token.getVerified()) {
                        log.warn("⚠️ Código JÁ UTILIZADO");
                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        return false;
                    }

                    // ✅ Marcar como verificado
                    token.setVerified(true);
                    tokenRepository.save(token);
                    log.info("✅ Token marcado como verificado");

                    // ✅ Ativar usuário
                    User user = token.getUser();
                    user.setEnabled(true);
                    userRepository.save(user);
                    log.info("✅ Usuário ativado: {}", user.getEmail());

                    log.info("🎉 VERIFICAÇÃO CONCLUÍDA COM SUCESSO!");
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("❌ CÓDIGO NÃO ENCONTRADO NO BANCO: {}", code);
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    return false;
                });
    }

    /**
     * ✅ CORRIGIDO: Reenvia código de verificação
     */
    @Transactional
    public boolean resendCode(String email) {
        try {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("🔄 REENVIANDO CÓDIGO");
            log.info("   📧 Email: {}", email);

            // ✅ Buscar usuário por email
            return userRepository.findByEmail(email)
                    .map(user -> {
                        log.info("✅ Usuário encontrado: {}", user.getUsername());
                        log.info("   Habilitado: {}", user.getEnabled());

                        // ✅ Se já está habilitado, não precisa reenviar
                        if (user.getEnabled()) {
                            log.warn("⚠️ Usuário já está verificado!");
                            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                            return false;
                        }

                        // ✅ Criar novo token (que automaticamente deleta o antigo)
                        String token = createVerificationToken(user);
                        log.info("✅ Novo código enviado com sucesso!");
                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        return true;
                    })
                    .orElseGet(() -> {
                        log.warn("❌ Usuário não encontrado: {}", email);
                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        return false;
                    });

        } catch (Exception e) {
            log.error("❌ ERRO ao reenviar código: {}", e.getMessage(), e);
            return false;
        }
    }
}