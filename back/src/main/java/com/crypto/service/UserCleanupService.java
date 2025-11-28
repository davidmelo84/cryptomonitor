package com.crypto.service;

import com.crypto.model.User;
import com.crypto.model.VerificationToken;
import com.crypto.repository.UserRepository;
import com.crypto.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;


    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupUnverifiedAccounts() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧹 INICIANDO LIMPEZA DE CONTAS NÃO VERIFICADAS");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            int tokensRemoved = cleanupExpiredTokens();
            log.info("   🗑️ Tokens expirados removidos: {}", tokensRemoved);

            int accountsRemoved = cleanupOldUnverifiedUsers();
            log.info("   🗑️ Contas não verificadas removidas: {}", accountsRemoved);

            log.info("✅ LIMPEZA CONCLUÍDA!");
            log.info("📊 Total: {} tokens + {} contas", tokensRemoved, accountsRemoved);

        } catch (Exception e) {
            log.error("❌ ERRO na limpeza automática: {}", e.getMessage(), e);
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }


    @Scheduled(fixedDelay = 86400000, initialDelay = 3600000)
    public void logDailyStats() {
        try {
            Map<String, Object> stats = getUnverifiedStats();

            long total = (Long) stats.get("totalUsers");
            long verified = (Long) stats.get("verifiedUsers");
            long unverified = (Long) stats.get("unverifiedUsers");
            long oldUnverified = (Long) stats.get("oldUnverified");

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📊 ESTATÍSTICAS DIÁRIAS DE USUÁRIOS");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("   Total: {}", total);
            log.info("   Verificados: {} ({}%)", verified,
                    total > 0 ? (verified * 100 / total) : 0);
            log.info("   Não verificados: {}", unverified);
            log.info("   Não verificados antigos (>7d): {}", oldUnverified);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ Erro ao gerar estatísticas: {}", e.getMessage());
        }
    }

    @Transactional
    public int cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        List<VerificationToken> expiredTokens =
                tokenRepository.findAll().stream()
                        .filter(t -> t.getExpiryDate().isBefore(cutoff))
                        .toList();

        expiredTokens.forEach(tokenRepository::delete);

        return expiredTokens.size();
    }

    @Transactional
    public int cleanupOldUnverifiedUsers() {
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

        usersToDelete.forEach(user -> {
            tokenRepository.findByUser(user)
                    .ifPresent(tokenRepository::delete);

            userRepository.delete(user);
        });

        return usersToDelete.size();
    }

    @Transactional
    public Map<String, Object> performManualCleanup() {
        int tokens = cleanupExpiredTokens();
        int accounts = cleanupOldUnverifiedUsers();

        return Map.of(
                "success", true,
                "tokensRemoved", tokens,
                "accountsRemoved", accounts,
                "timestamp", LocalDateTime.now()
        );
    }


    public Map<String, Object> getUnverifiedStats() {
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
    }
}
