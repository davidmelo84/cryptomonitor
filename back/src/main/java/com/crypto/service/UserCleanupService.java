// back/src/main/java/com/crypto/service/UserCleanupService.java
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

/**
 * ✅ SERVIÇO DE LIMPEZA DE CONTAS NÃO VERIFICADAS
 *
 * Remove automaticamente:
 * - Contas criadas há mais de 7 dias e não verificadas
 * - Tokens de verificação expirados (> 24h)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;

    /**
     * ✅ LIMPEZA AUTOMÁTICA - Executa diariamente às 3h da manhã
     */
    @Scheduled(cron = "0 0 3 * * *") // 3h AM todos os dias
    @Transactional
    public void cleanupUnverifiedAccounts() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧹 INICIANDO LIMPEZA DE CONTAS NÃO VERIFICADAS");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            // ✅ 1. Limpar tokens expirados (> 24h)
            int tokensRemoved = cleanupExpiredTokens();
            log.info("   🗑️  Tokens expirados removidos: {}", tokensRemoved);

            // ✅ 2. Limpar contas não verificadas antigas (> 7 dias)
            int accountsRemoved = cleanupOldUnverifiedUsers();
            log.info("   🗑️  Contas não verificadas removidas: {}", accountsRemoved);

            log.info("✅ LIMPEZA CONCLUÍDA!");
            log.info("   📊 Total: {} tokens + {} contas", tokensRemoved, accountsRemoved);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ ERRO na limpeza automática: {}", e.getMessage(), e);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    /**
     * ✅ Remover tokens expirados (> 24h)
     */
    @Transactional
    public int cleanupExpiredTokens() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

            List<VerificationToken> expiredTokens = tokenRepository.findAll().stream()
                    .filter(token -> token.getExpiryDate().isBefore(cutoffTime))
                    .toList();

            if (expiredTokens.isEmpty()) {
                log.debug("   ℹ️  Nenhum token expirado encontrado");
                return 0;
            }

            for (VerificationToken token : expiredTokens) {
                log.debug("   🗑️  Removendo token: {} (expirado em: {})",
                        token.getCode(), token.getExpiryDate());
                tokenRepository.delete(token);
            }

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
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);

            // Buscar usuários não verificados antigos
            List<User> oldUnverifiedUsers = userRepository.findAll().stream()
                    .filter(user -> !user.getEnabled()) // Não verificado
                    .filter(user -> {
                        // ✅ USAR createdAt diretamente
                        if (user.getCreatedAt() != null) {
                            return user.getCreatedAt().isBefore(cutoffTime);
                        }
                        // Fallback: usar token de verificação
                        return tokenRepository.findByUser(user)
                                .map(token -> token.getExpiryDate().isBefore(cutoffTime))
                                .orElse(true); // Se não tem token, considerar antigo
                    })
                    .toList();

            if (oldUnverifiedUsers.isEmpty()) {
                log.debug("   ℹ️  Nenhuma conta antiga não verificada encontrada");
                return 0;
            }

            for (User user : oldUnverifiedUsers) {
                log.info("   🗑️  Removendo conta não verificada: {} ({})",
                        user.getUsername(), user.getEmail());

                // Remover token associado primeiro
                tokenRepository.findByUser(user)
                        .ifPresent(tokenRepository::delete);

                // Remover usuário
                userRepository.delete(user);
            }

            return oldUnverifiedUsers.size();

        } catch (Exception e) {
            log.error("❌ Erro ao limpar contas: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * ✅ LIMPEZA MANUAL - Endpoint para admin
     */
    @Transactional
    public Map<String, Object> performManualCleanup() {
        log.info("🧹 LIMPEZA MANUAL INICIADA");

        int tokensRemoved = cleanupExpiredTokens();
        int accountsRemoved = cleanupOldUnverifiedUsers();

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("tokensRemoved", tokensRemoved);
        result.put("accountsRemoved", accountsRemoved);
        result.put("timestamp", LocalDateTime.now());

        log.info("✅ Limpeza manual concluída: {} tokens, {} contas",
                tokensRemoved, accountsRemoved);

        return result;
    }

    /**
     * ✅ Estatísticas de contas não verificadas
     */
    public Map<String, Object> getUnverifiedStats() {
        try {
            List<User> allUsers = userRepository.findAll();

            long totalUsers = allUsers.size();
            long verifiedUsers = allUsers.stream()
                    .filter(User::getEnabled)
                    .count();
            long unverifiedUsers = totalUsers - verifiedUsers;

            // Contar por idade
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            long recentUnverified = allUsers.stream()
                    .filter(user -> !user.getEnabled())
                    .filter(user -> tokenRepository.findByUser(user)
                            .map(token -> token.getExpiryDate().isAfter(oneDayAgo))
                            .orElse(false))
                    .count();

            long oldUnverified = allUsers.stream()
                    .filter(user -> !user.getEnabled())
                    .filter(user -> tokenRepository.findByUser(user)
                            .map(token -> token.getExpiryDate().isBefore(sevenDaysAgo))
                            .orElse(true))
                    .count();

            return Map.of(
                    "totalUsers", totalUsers,
                    "verifiedUsers", verifiedUsers,
                    "unverifiedUsers", unverifiedUsers,
                    "recentUnverified", recentUnverified,
                    "oldUnverified", oldUnverified,
                    "timestamp", LocalDateTime.now()
            );

        } catch (Exception e) {
            log.error("❌ Erro ao obter estatísticas: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}