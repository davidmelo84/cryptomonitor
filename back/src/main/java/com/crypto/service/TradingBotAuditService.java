package com.crypto.service;

import com.crypto.model.*;
import com.crypto.repository.TradingBotAuditLogRepository;
import com.crypto.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ Serviço de Auditoria para Trading Bots
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingBotAuditService {

    private final TradingBotAuditLogRepository auditRepository;
    private final UserRepository userRepository;

    @Async
    public void logAction(
            String username,
            TradingBot bot,
            TradingBotAuditLog.AuditAction action,
            TradingBotAuditLog.Severity severity,
            String description) {

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            HttpServletRequest request = getCurrentHttpRequest();

            TradingBotAuditLog auditLog = TradingBotAuditLog.builder()
                    .user(user)
                    .bot(bot)
                    .action(action)
                    .severity(severity)
                    .description(description)
                    .wasSimulation(bot != null ? bot.getIsSimulation() : null)
                    .ipAddress(request != null ? getClientIp(request) : null)
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .build();

            auditRepository.save(auditLog);

            if (severity == TradingBotAuditLog.Severity.CRITICAL) {
                log.warn("🚨 AUDITORIA CRÍTICA - Usuário: {}, Ação: {}, Descrição: {}",
                        username, action, description);
            } else {
                log.info("📋 Auditoria registrada - Usuário: {}, Ação: {}",
                        username, action);
            }

        } catch (Exception e) {
            log.error("Erro ao registrar auditoria: {}", e.getMessage(), e);
        }
    }

    public Page<TradingBotAuditLog> getUserAuditLogs(
            String username, int page, int size) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return auditRepository.findByUserOrderByCreatedAtDesc(
                user, PageRequest.of(page, size));
    }

    public List<TradingBotAuditLog> getCriticalLogs(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return auditRepository.findByUserAndSeverityOrderByCreatedAtDesc(
                user, TradingBotAuditLog.Severity.CRITICAL);
    }

    public List<TradingBotAuditLog> getRecentCriticalSystemLogs(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return auditRepository.findRecentBySeverity(
                since, TradingBotAuditLog.Severity.CRITICAL);
    }

    public long countRecentAttempts(
            String username,
            TradingBotAuditLog.AuditAction action,
            int minutes) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);

        return auditRepository.countByUserAndActionAndCreatedAtAfter(
                user, action, since);
    }

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
