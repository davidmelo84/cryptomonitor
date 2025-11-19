// back/src/main/java/com/crypto/service/UserCleanupService.java
package com.crypto.service;

import com.crypto.model.User;
import com.crypto.model.VerificationToken;
import com.crypto.repository.UserRepository;
import com.crypto.repository.VerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ✅ SERVIÇO DE LIMPEZA DE CONTAS NÃO VERIFICADAS
 *
 * Remove automaticamente:
 * - Contas criadas há mais de 7 dias e não verificadas
 * - Tokens de verificação expirados (> 24h)
 */
@Slf4j
@Service
public class UserCleanupService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;

    /**
     * ✅ CORREÇÃO: Removido @RequiredArgsConstructor (causava ciclo)
     * Criado construtor manual seguro.
     */
    public UserCleanupService(
            VerificationTokenRepository tokenRepository,
            UserRepository userRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * ✅ LIMPEZA AUTOMÁTICA - Executa diariamente às 3h da manhã
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupUnverifiedAccounts() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧹 INICIANDO LIMPEZA DE CONTAS NÃO VERIFICADAS");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            int tokensRemoved = cleanupExpiredTokens();
            log.info("   🗑️  Tokens expirados removidos: {}", tokensRemoved);

            int accountsRemoved = cleanupOldUnverifiedUsers();
            log.info("   🗑️  Contas não verificadas removidas: {}", accountsRemoved);

            log.info("✅ LIMPEZA CONCLUÍDA!");
            log.info("📊 Total: {} tokens + {} contas", tokensRemoved, accountsRemoved);
        } catch (Exception e) {
            log.error("❌ ERRO na limpeza automática: {}", e.getMessage(), e);
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * ✅ Remover tokens expirados (> 24h)
     */
    @Transactional
    public int cleanupExpiredTokens() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

            List<VerificationToken> expiredTokens = tokenRepository.findAll().stream()
                    .filter(t -> t.getExpiryDate().isBefore(cutoff))
                    .toList();

            if (expiredTokens.isEmpty()) {
                log.debug("ℹ️ Nenhum token expirado encontrado");
                return 0;
            }

            expiredTokens.forEach(token -> {
                log.debug("🗑️ Removendo token: {} (expirado em: {})",
                        token.getCode(), token.getExpiryDate());
                tokenRepository.delete(token);
            });

            return expiredTokens.size();

        } catch (Exception e) {
            log.error("❌ Erro ao limpar tokens: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * ✅ Remover contas não verificadas antigas (> 7 dias)
     */
    @Transactional
    public int cleanupOldUnverifiedUsers() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

            List<User> usersToDelete = userRepository.findAll().stream()
                    .filter(u -> !u.getEnabled())
                    .filter(u -> {
                        if (u.getCreatedAt() != null)
                            return u.getCreatedAt().isBefore(cutoff);

                        return tokenRepository.findByUser(u)
                                .map(t -> t.getExpiryDate().isBefore(cutoff))
                                .orElse(true);
                    })
                    .toList();

            if (usersToDelete.isEmpty()) {
                log.debug("ℹ️ Nenhuma conta não verificada antiga encontrada");
                return 0;
            }

            for (User user : usersToDelete) {
                log.info("🗑️ Removendo conta não verificada: {} ({})",
                        user.getUsername(), user.getEmail());

                tokenRepository.findByUser(user)
                        .ifPresent(tokenRepository::delete);

                userRepository.delete(user);
            }

            return usersToDelete.size();

        } catch (Exception e) {
            log.error("❌ Erro ao limpar contas: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * ✅ LIMPEZA MANUAL - endpoint admin
     */
    @Transactional
    public Map<String, Object> performManualCleanup() {
        log.info("🧹 LIMPEZA MANUAL INICIADA");

        int tokens = cleanupExpiredTokens();
        int accounts = cleanupOldUnverifiedUsers();

        log.info("✅ Limpeza manual concluída: {} tokens, {} contas", tokens, accounts);

        return Map.of(
                "success", true,
                "tokensRemoved", tokens,
                "accountsRemoved", accounts,
                "timestamp", LocalDateTime.now()
        );
    }

    /**
     * 📊 Estatísticas
     */
    public Map<String, Object> getUnverifiedStats() {
        try {
            List<User> all = userRepository.findAll();

            long total = all.size();
            long verified = all.stream().filter(User::getEnabled).count();
            long unverified = total - verified;

            LocalDateTime d1 = LocalDateTime.now().minusDays(1);
            LocalDateTime d7 = LocalDateTime.now().minusDays(7);

            long recent = all.stream()
                    .filter(u -> !u.getEnabled())
                    .filter(u -> tokenRepository.findByUser(u)
                            .map(t -> t.getExpiryDate().isAfter(d1))
                            .orElse(false))
                    .count();

            long old = all.stream()
                    .filter(u -> !u.getEnabled())
                    .filter(u -> tokenRepository.findByUser(u)
                            .map(t -> t.getExpiryDate().isBefore(d7))
                            .orElse(true))
                    .count();

            return Map.of(
                    "totalUsers", total,
                    "verifiedUsers", verified,
                    "unverifiedUsers", unverified,
                    "recentUnverified", recent,
                    "oldUnverified", old,
                    "timestamp", LocalDateTime.now()
            );

        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}
