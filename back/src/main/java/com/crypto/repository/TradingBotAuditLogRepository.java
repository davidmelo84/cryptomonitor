package com.crypto.repository;

import com.crypto.model.TradingBotAuditLog;
import com.crypto.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradingBotAuditLogRepository extends JpaRepository<TradingBotAuditLog, Long> {

    Page<TradingBotAuditLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<TradingBotAuditLog> findByUserAndSeverityOrderByCreatedAtDesc(
            User user, TradingBotAuditLog.Severity severity);

    @Query("SELECT l FROM TradingBotAuditLog l WHERE l.createdAt >= :since " +
            "AND l.severity = :severity ORDER BY l.createdAt DESC")
    List<TradingBotAuditLog> findRecentBySeverity(
            LocalDateTime since, TradingBotAuditLog.Severity severity);

    long countByUserAndActionAndCreatedAtAfter(
            User user, TradingBotAuditLog.AuditAction action, LocalDateTime since);
}